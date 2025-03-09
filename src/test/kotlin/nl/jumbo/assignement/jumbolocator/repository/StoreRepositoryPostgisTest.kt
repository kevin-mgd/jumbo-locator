package nl.jumbo.assignement.jumbolocator.repository

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.jumbo.assignement.jumbolocator.domain.model.GeoCoordinates
import nl.jumbo.assignement.jumbolocator.domain.model.Store
import nl.jumbo.assignement.jumbolocator.repository.entity.StoreEntity
import nl.jumbo.assignement.jumbolocator.repository.projection.StoreWithDistanceProjection
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(SpringExtension::class)
class StoreRepositoryPostgisTest {

    private lateinit var springDataRepository: PostgisDataStoreRepository
    private lateinit var storeRepository: StoreRepositoryPostgis

    private lateinit var geometryFactory: GeometryFactory
    private lateinit var testStoreEntity: StoreEntity
    private lateinit var testCoordinates: GeoCoordinates

    @BeforeEach
    fun setup() {
        springDataRepository = mockk()
        storeRepository = StoreRepositoryPostgis(springDataRepository)

        geometryFactory = GeometryFactory(PrecisionModel(), 4326)

        testCoordinates = GeoCoordinates(52.0, 5.0)

        testStoreEntity = StoreEntity(
            id = 1L,
            storeId = 1001L,
            city = "Amsterdam",
            postalCode = "1000AA",
            street = "Test Street",
            street2 = "Building 123",
            street3 = null,
            addressName = "Jumbo Amsterdam",
            longitude = 4.9041,
            latitude = 52.3676,
            location = geometryFactory.createPoint(Coordinate(4.9041, 52.3676)),
            complexNumber = "1001",
            showWarningMessage = false,
            todayOpen = "08:00",
            locationType = "STORE",
            collectionPoint = false,
            sapStoreID = "1001",
            todayClose = "22:00",
            uuid = "123e4567-e89b-12d3-a456-426614174000"
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("Find Nearest Stores Tests")
    inner class FindNearestStoresTests {

        @Test
        @DisplayName("Should return stores with distance")
        fun findNearestStoresSuccess() {
            // Given
            val limit = 5
            val distance = 2500.0

            val projection = createMockProjection(testStoreEntity, distance)

            every {
                springDataRepository.findNearestStoresWithDistance(
                    testCoordinates.latitude,
                    testCoordinates.longitude,
                    limit
                )
            } returns listOf(projection)

            // When
            val result = storeRepository.findNearestStores(testCoordinates, limit)

            // Then
            assertTrue(result.isRight())
            result.map { storesWithDistance ->
                assertEquals(1, storesWithDistance.size)

                val storeWithDistance = storesWithDistance.first()
                assertEquals(testStoreEntity.storeId, storeWithDistance.store.id)
                assertEquals(testStoreEntity.addressName, storeWithDistance.store.name)
                assertEquals(testStoreEntity.city, storeWithDistance.store.address.city)
                assertEquals(2.5, storeWithDistance.distanceInKm)
                assertEquals(2500, storeWithDistance.distanceInMeters)
            }

            verify(exactly = 1) {
                springDataRepository.findNearestStoresWithDistance(
                    testCoordinates.latitude,
                    testCoordinates.longitude,
                    limit
                )
            }
        }

        @Test
        @DisplayName("Should handle repository exceptions")
        fun handleRepositoryExceptions() {
            // Given
            val limit = 5

            every {
                springDataRepository.findNearestStoresWithDistance(
                    testCoordinates.latitude,
                    testCoordinates.longitude,
                    limit
                )
            } throws RuntimeException("Database error")

            // When
            val result = storeRepository.findNearestStores(testCoordinates, limit)

            // Then
            assertTrue(result.isLeft())
            result.mapLeft { error ->
                assertTrue(error.message.contains("Failed to retrieve stores from database"))
            }
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    inner class FindByIdTests {

        @Test
        @DisplayName("Should return store when found")
        fun findByIdSuccess() {
            // Given
            val storeId = 1001L

            every { springDataRepository.findByStoreId(storeId) } returns testStoreEntity

            // When
            val result = storeRepository.findById(storeId)

            // Then
            assertTrue(result.isRight())
            result.map { store ->
                assertEquals(testStoreEntity.storeId, store.id)
                assertEquals(testStoreEntity.addressName, store.name)
                assertEquals(testStoreEntity.city, store.address.city)
            }

            verify(exactly = 1) { springDataRepository.findByStoreId(storeId) }
        }

        @Test
        @DisplayName("Should return not found error when store doesn't exist")
        fun handleStoreNotFound() {
            // Given
            val storeId = 999L

            every { springDataRepository.findByStoreId(storeId) } returns null

            // When
            val result = storeRepository.findById(storeId)

            // Then
            assertTrue(result.isLeft())
            result.mapLeft { error ->
                assertTrue(error.message.contains("Store not found"))
            }

            verify(exactly = 1) { springDataRepository.findByStoreId(storeId) }
        }

        @Test
        @DisplayName("Should handle exceptions")
        fun handleExceptions() {
            // Given
            val storeId = 1001L

            every { springDataRepository.findByStoreId(storeId) } throws RuntimeException("Database error")

            // When
            val result = storeRepository.findById(storeId)

            // Then
            assertTrue(result.isLeft())
            result.mapLeft { error ->
                assertTrue(error.message.contains("Failed to retrieve store from database"))
            }
        }
    }

    @Nested
    @DisplayName("Save All Tests")
    inner class SaveAllTests {

        @Test
        @DisplayName("Should save stores")
        fun saveAllSuccess() {
            // Given
            val store = mapEntityToDomain(testStoreEntity)
            val stores = listOf(store)

            every { springDataRepository.saveAll(any<List<StoreEntity>>()) } returns listOf(testStoreEntity)

            // When
            val result = storeRepository.saveAll(stores)

            // Then
            assertTrue(result.isRight())
            result.map { savedStores ->
                assertEquals(1, savedStores.size)
                assertEquals(store.id, savedStores.first().id)
            }

            verify(exactly = 1) { springDataRepository.saveAll(any<List<StoreEntity>>()) }
        }

        @Test
        @DisplayName("Should handle exceptions")
        fun handleExceptions() {
            // Given
            val store = mapEntityToDomain(testStoreEntity)
            val stores = listOf(store)

            every { springDataRepository.saveAll(any<List<StoreEntity>>()) } throws RuntimeException("Database error")

            // When
            val result = storeRepository.saveAll(stores)

            // Then
            assertTrue(result.isLeft())
            result.mapLeft { error ->
                assertTrue(error.message.contains("Failed to save stores to database"))
            }
        }
    }

    @Nested
    @DisplayName("Count Tests")
    inner class CountTests {

        @Test
        @DisplayName("Should return store count")
        fun countSuccess() {
            // Given
            val count = 42L

            every { springDataRepository.count() } returns count

            // When
            val result = storeRepository.count()

            // Then
            assertTrue(result.isRight())
            result.map { actualCount ->
                assertEquals(count, actualCount)
            }

            verify(exactly = 1) { springDataRepository.count() }
        }

        @Test
        @DisplayName("Should handle exceptions")
        fun handleExceptions() {
            // Given
            every { springDataRepository.count() } throws RuntimeException("Database error")

            // When
            val result = storeRepository.count()

            // Then
            assertTrue(result.isLeft())
            result.mapLeft { error ->
                assertTrue(error.message.contains("Failed to count stores in database"))
            }
        }
    }

    // Helper methods
    private fun createMockProjection(entity: StoreEntity, distance: Double): StoreWithDistanceProjection {
        return object : StoreWithDistanceProjection {
            override val id: Long? = entity.id
            override val storeId: Long = entity.storeId
            override val city: String = entity.city
            override val postalCode: String = entity.postalCode
            override val street: String = entity.street
            override val street2: String? = entity.street2
            override val street3: String? = entity.street3
            override val addressName: String = entity.addressName
            override val longitude: Double = entity.longitude
            override val latitude: Double = entity.latitude
            override val complexNumber: String = entity.complexNumber
            override val showWarningMessage: Boolean = entity.showWarningMessage
            override val todayOpen: String = entity.todayOpen
            override val locationType: String = entity.locationType
            override val collectionPoint: Boolean = entity.collectionPoint
            override val sapStoreID: String = entity.sapStoreID.toString()
            override val todayClose: String = entity.todayClose
            override val uuid: String = entity.uuid
            override val distance: Double = distance
        }
    }

    private fun mapEntityToDomain(entity: StoreEntity): Store {
        return Store(
            id = entity.storeId,
            name = entity.addressName,
            address = nl.jumbo.assignement.jumbolocator.domain.model.Address(
                street = entity.street,
                street2 = entity.street2,
                street3 = entity.street3,
                city = entity.city,
                postalCode = entity.postalCode
            ),
            coordinates = GeoCoordinates(
                latitude = entity.latitude,
                longitude = entity.longitude
            ),
            openingHours = nl.jumbo.assignement.jumbolocator.domain.model.OpeningHours(
                open = entity.todayOpen,
                close = entity.todayClose
            ),
            locationType = entity.locationType,
            isCollectionPoint = entity.collectionPoint
        )
    }
}
