package nl.jumbo.assignement.jumbolocator.repository

import nl.jumbo.assignement.jumbolocator.TestcontainersConfiguration
import nl.jumbo.assignement.jumbolocator.domain.model.Address
import nl.jumbo.assignement.jumbolocator.domain.model.GeoCoordinates
import nl.jumbo.assignement.jumbolocator.domain.model.OpeningHours
import nl.jumbo.assignement.jumbolocator.domain.model.Store
import nl.jumbo.assignement.jumbolocator.repository.entity.StoreEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.*

@DataJpaTest
@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ComponentScan(basePackages = ["nl.jumbo.assignement.jumbolocator.repository"])
class StoreRepositoryPostgisIntegrationTest {

    @Autowired
    private lateinit var postgisDataStoreRepository: PostgisDataStoreRepository

    @Autowired
    private lateinit var storeRepository: StoreRepositoryPostgis

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    private val bredaStores = listOf(
        StoreTestData(
            id = null,
            name = "Jumbo Breda Baliendijk",
            latitude = 51.598767,
            longitude = 4.793045,
            city = "Breda",
            postalCode = "4816 GD",
            street = "Baliendijk",
            street2 = "20",
            sapStoreId = "4838"
        ),
        StoreTestData(
            id = null,
            name = "Jumbo Breda Belcrumweg",
            latitude = 51.596607,
            longitude = 4.770219,
            city = "Breda",
            postalCode = "4815 HA",
            street = "Belcrumweg",
            street2 = "5-7",
            sapStoreId = "4620"
        ),
        StoreTestData(
            id = null,
            name = "Jumbo Breda Cypresstraat",
            latitude = 51.587510,
            longitude = 4.753484,
            city = "Breda",
            postalCode = "4814 PN",
            street = "Cypresstraat",
            street2 = "21",
            sapStoreId = "4648"
        ),
        StoreTestData(
            id = null,
            name = "Jumbo Breda De Burcht",
            latitude = 51.572716,
            longitude = 4.806992,
            city = "Breda",
            postalCode = "4834 HE",
            street = "De Burcht",
            street2 = "20",
            sapStoreId = "3695",
            isCollectionPoint = true
        )
    )

    private fun StoreTestData.toStoreEntity(): StoreEntity {
        val point = geometryFactory.createPoint(Coordinate(longitude, latitude))
        return StoreEntity(
            storeId = UUID.randomUUID().mostSignificantBits,
            addressName = name,
            street = street,
            street2 = street2,
            street3 = null,
            city = city,
            postalCode = postalCode,
            latitude = latitude,
            longitude = longitude,
            location = point,
            todayOpen = "08:00",
            todayClose = "22:00",
            locationType = "Supermarkt",
            collectionPoint = isCollectionPoint,
            complexNumber = UUID.randomUUID().toString(),
            showWarningMessage = false,
            sapStoreID = sapStoreId,
            uuid = UUID.randomUUID().toString()
        )
    }

    private fun StoreTestData.toStore(): Store {
        return Store(
            id = UUID.randomUUID().mostSignificantBits,
            name = name,
            address = Address(
                street = street,
                street2 = street2,
                street3 = null,
                city = city,
                postalCode = postalCode
            ),
            coordinates = GeoCoordinates(
                latitude = latitude,
                longitude = longitude
            ),
            openingHours = OpeningHours(
                open = "08:00",
                close = "22:00"
            ),
            locationType = "Supermarkt",
            isCollectionPoint = isCollectionPoint
        )
    }

    private data class StoreTestData(
        val id: Long? = null,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val city: String,
        val postalCode: String,
        val street: String,
        val street2: String,
        val sapStoreId: String,
        val isCollectionPoint: Boolean = false
    )

    @BeforeEach
    fun setup() {
        postgisDataStoreRepository.deleteAll()
    }

    @Test
    fun `findNearestStores should return stores ordered by distance from given coordinates in Breda`() {
        val testStoreEntities = bredaStores.map { it.toStoreEntity() }
        postgisDataStoreRepository.saveAll(testStoreEntities)

        val testCoordinates = GeoCoordinates(51.5765997828723, 4.798600263963545)
        val limit = 3

        val result = storeRepository.findNearestStores(testCoordinates, limit)

        assertThat(result.isRight()).isTrue()
        val storesWithDistance = result.getOrNull()
        assertThat(storesWithDistance).isNotNull
        assertThat(storesWithDistance).hasSize(3)

        assertThat(storesWithDistance!![0].store.name).isEqualTo("Jumbo Breda De Burcht")

        assertThat(storesWithDistance[0].distanceInMeters)
            .isLessThan(storesWithDistance[1].distanceInMeters)
        assertThat(storesWithDistance[1].distanceInMeters)
            .isLessThan(storesWithDistance[2].distanceInMeters)
    }

    @Test
    fun `saveAll should persist stores to database`() {
        postgisDataStoreRepository.deleteAll()

        val testStores = bredaStores.take(2).map { it.toStore() }

        val result = storeRepository.saveAll(testStores)

        assertThat(result.isRight()).isTrue()
        val savedStores = result.getOrNull()
        assertThat(savedStores).hasSize(2)

        val count = postgisDataStoreRepository.count()
        assertThat(count).isEqualTo(2)

        val retrievedStore = postgisDataStoreRepository.findByStoreId(savedStores!![0].id)
        assertThat(retrievedStore).isNotNull
        assertThat(retrievedStore!!.addressName).isEqualTo(testStores[0].name)
    }
}
