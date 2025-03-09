package nl.jumbo.assignement.jumbolocator.service

import arrow.core.left
import arrow.core.right
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import nl.jumbo.assignement.jumbolocator.api.response.StoreResponse
import nl.jumbo.assignement.jumbolocator.domain.error.DomainError
import nl.jumbo.assignement.jumbolocator.domain.model.Address
import nl.jumbo.assignement.jumbolocator.domain.model.GeoCoordinates
import nl.jumbo.assignement.jumbolocator.domain.model.OpeningHours
import nl.jumbo.assignement.jumbolocator.domain.model.Store
import nl.jumbo.assignement.jumbolocator.domain.model.StoreWithDistance
import nl.jumbo.assignement.jumbolocator.repository.StoreRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class StoreServiceTest {
    @MockK
    private lateinit var storeRepository: StoreRepository

    @MockK
    private lateinit var storeLoader: StoreLoader

    @InjectMockKs
    private lateinit var storeService: StoreService

    private val testStore = createTestStore()
    private val storeWithDistance = createTestStoreWithDistance()

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Nested
    inner class InitializeStoresTests {
        @Test
        fun `should load stores when repository is empty`() {
            val testStores = listOf(testStore)

            every { storeRepository.count() } returns 0L.right()
            every { storeLoader.loadStores() } returns testStores.right()
            every { storeRepository.saveAll(testStores) } returns testStores.right()

            storeService.initializeStores()

            verify { storeRepository.count() }
            verify { storeLoader.loadStores() }
            verify { storeRepository.saveAll(testStores) }
        }

        @Test
        fun `should skip loading when stores exist`() {
            val existingCount = 10L
            every { storeRepository.count() } returns existingCount.right()

            storeService.initializeStores()

            verify { storeRepository.count() }
            verify(exactly = 0) { storeLoader.loadStores() }
            verify(exactly = 0) { storeRepository.saveAll(any()) }
        }

        @Test
        fun `should handle repository count error gracefully`() {
            val error = DomainError.DataAccessError("Count failed")
            every { storeRepository.count() } returns error.left()

            storeService.initializeStores()

            verify { storeRepository.count() }
        }

        @Test
        fun `should handle store loading error gracefully`() {
            val error = DomainError.UnexpectedError("Loading failed")
            every { storeRepository.count() } returns 0L.right()
            every { storeLoader.loadStores() } returns error.left()

            storeService.initializeStores()

            verify { storeLoader.loadStores() }
        }

        @Test
        fun `should handle store saving error gracefully`() {
            val testStores = listOf(testStore)
            val error = DomainError.DataAccessError("Save failed")

            every { storeRepository.count() } returns 0L.right()
            every { storeLoader.loadStores() } returns testStores.right()
            every { storeRepository.saveAll(testStores) } returns error.left()

            storeService.initializeStores()

            verify { storeRepository.saveAll(testStores) }
        }
    }

    @Nested
    inner class FindNearestStoresTests {
        @Test
        fun `should return nearest stores with detailed information`() {
            val latitude = 52.3702
            val longitude = 4.8952
            val limit = 5
            val storesWithDistance = listOf(storeWithDistance)

            val coordinatesSlot = slot<GeoCoordinates>()
            val limitSlot = slot<Int>()

            every {
                storeRepository.findNearestStores(
                    capture(coordinatesSlot),
                    capture(limitSlot)
                )
            } returns storesWithDistance.right()

            val result = storeService.findNearestStores(latitude, longitude, limit)

            assertEquals(latitude, coordinatesSlot.captured.latitude)
            assertEquals(longitude, coordinatesSlot.captured.longitude)
            assertEquals(limit, limitSlot.captured)

            assertTrue(result.isRight())
            result.map { stores ->
                assertEquals(1, stores.size)
                with(stores.first()) {
                    assertEquals(testStore.id, storeId)
                    assertEquals(testStore.name, name)
                    assertEquals(testStore.address.asFormattedString(), address)
                    assertEquals(testStore.address.city, city)
                    assertEquals(testStore.address.postalCode, postalCode)
                    assertEquals(testStore.coordinates.latitude, latitude)
                    assertEquals(testStore.coordinates.longitude, longitude)
                    assertEquals(storeWithDistance.distanceInKm, distanceInKm)
                    assertEquals(storeWithDistance.distanceInMeters, distanceInMeters)
                    assertEquals(testStore.openingHours.open, openingHours)
                    assertEquals(testStore.openingHours.close, closingHours)
                    assertEquals(testStore.isCollectionPoint, isCollectionPoint)
                    assertNotNull(lastUpdated)
                    assertTrue(lastUpdated.isAfter(LocalDateTime.now().minusMinutes(1)))
                    assertTrue(lastUpdated.isBefore(LocalDateTime.now().plusMinutes(1)))
                }
            }
        }

        @Test
        fun `should validate coordinate boundaries`() {
            val testCases = listOf(
                Pair(-91.0, 0.0),
                Pair(91.0, 0.0),
                Pair(0.0, -181.0),
                Pair(0.0, 181.0)
            )

            testCases.forEach { (latitude, longitude) ->
                val result = storeService.findNearestStores(latitude, longitude, 5)

                assertTrue(result.isLeft(), "Expected error for coordinates ($latitude, $longitude)")
                result.mapLeft { error ->
                    assertTrue(error is DomainError.ValidationError)
                    assertEquals("coordinates", error.field)
                }
            }

            verify(exactly = 0) { storeRepository.findNearestStores(any(), any()) }
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1, 21, 100])
        fun `should validate limit boundaries`(limit: Int) {
            val result = storeService.findNearestStores(52.3702, 4.8952, limit)

            assertTrue(result.isLeft())
            result.mapLeft { error ->
                assertTrue(error is DomainError.ValidationError)
                assertEquals("limit", (error as DomainError.ValidationError).field)
                assertEquals(limit, error.invalidValue)
            }

            verify(exactly = 0) { storeRepository.findNearestStores(any(), any()) }
        }

        @Test
        fun `should propagate repository errors`() {
            val latitude = 52.3702
            val longitude = 4.8952
            val limit = 5
            val error = DomainError.DataAccessError("Query failed")

            every {
                storeRepository.findNearestStores(
                    GeoCoordinates(latitude, longitude),
                    limit
                )
            } returns error.left()

            val result = storeService.findNearestStores(latitude, longitude, limit)

            assertTrue(result.isLeft())
            assertEquals(error, result.leftOrNull())
        }

        @Test
        fun `should handle empty results gracefully`() {
            val latitude = 52.3702
            val longitude = 4.8952
            val limit = 5

            every {
                storeRepository.findNearestStores(
                    GeoCoordinates(latitude, longitude),
                    limit
                )
            } returns emptyList<StoreWithDistance>().right()

            val result = storeService.findNearestStores(latitude, longitude, limit)

            assertTrue(result.isRight())
            result.map { stores ->
                assertTrue(stores.isEmpty())
            }
        }
    }

    @Nested
    inner class GetStoreDetailsTests {
        @Test
        fun `should return complete store details`() {
            val storeId = 1L
            val storeIdSlot = slot<Long>()

            every { storeRepository.findById(capture(storeIdSlot)) } returns testStore.right()

            val result = storeService.getStoreDetails(storeId)

            assertEquals(storeId, storeIdSlot.captured)

            assertTrue(result.isRight())
            result.map { storeResponse ->
                assertStoreResponseDetails(storeResponse)
            }
        }

        @ParameterizedTest
        @ValueSource(longs = [0L, -1L, -100L])
        fun `should validate store id and return appropriate error`(storeId: Long) {
            val result = storeService.getStoreDetails(storeId)

            assertTrue(result.isLeft())
            result.mapLeft { error ->
                assertTrue(error is DomainError.ValidationError)
                assertEquals("storeId", (error as DomainError.ValidationError).field)
                assertEquals(storeId, error.invalidValue)
            }

            verify(exactly = 0) { storeRepository.findById(any()) }
        }

        @Test
        fun `should propagate data access errors`() {
            val storeId = 1L
            val error = DomainError.DataAccessError("Query failed")

            every { storeRepository.findById(storeId) } returns error.left()

            val result = storeService.getStoreDetails(storeId)

            assertTrue(result.isLeft())
            assertEquals(error, result.leftOrNull())
        }

        @Test
        fun `should handle not found store properly`() {
            val storeId = 999L
            val error = DomainError.ResourceNotFound(
                message = "Store not found with ID: $storeId",
                resourceType = "Store",
                identifier = storeId.toString()
            )

            every { storeRepository.findById(storeId) } returns error.left()

            val result = storeService.getStoreDetails(storeId)

            assertTrue(result.isLeft())
            result.mapLeft { domainError ->
                assertTrue(domainError is DomainError.ResourceNotFound)
                with(domainError as DomainError.ResourceNotFound) {
                    assertEquals("Store", resourceType)
                    assertEquals(storeId.toString(), identifier)
                    assertTrue(message.contains(storeId.toString()))
                }
            }
        }
    }

    // Helper method to centralize store response assertions
    private fun assertStoreResponseDetails(storeResponse: StoreResponse) {
        assertEquals(testStore.id, storeResponse.storeId)
        assertEquals(testStore.name, storeResponse.name)
        assertEquals(testStore.address.asFormattedString(), storeResponse.address)
        assertEquals(testStore.address.city, storeResponse.city)
        assertEquals(testStore.address.postalCode, storeResponse.postalCode)
        assertEquals(testStore.coordinates.latitude, storeResponse.latitude)
        assertEquals(testStore.coordinates.longitude, storeResponse.longitude)
        assertEquals(0.0, storeResponse.distanceInKm)
        assertEquals(0, storeResponse.distanceInMeters)
        assertEquals(testStore.openingHours.open, storeResponse.openingHours)
        assertEquals(testStore.openingHours.close, storeResponse.closingHours)
        assertEquals(testStore.isCollectionPoint, storeResponse.isCollectionPoint)
        assertNotNull(storeResponse.lastUpdated)
        assertTrue(storeResponse.lastUpdated.isAfter(LocalDateTime.now().minusMinutes(1)))
        assertTrue(storeResponse.lastUpdated.isBefore(LocalDateTime.now().plusMinutes(1)))
    }

    private fun createTestStore() = Store(
        id = 1L,
        name = "Jumbo Test Store",
        address = Address(
            street = "Test Street",
            postalCode = "1234AB",
            city = "TestCity"
        ),
        coordinates = GeoCoordinates(52.3702, 4.8952),
        openingHours = OpeningHours(
            open = "08:00",
            close = "22:00"
        ),
        locationType = "SUPERMARKET",
        isCollectionPoint = false
    )

    private fun createTestStoreWithDistance() = StoreWithDistance(
        store = createTestStore(),
        distanceInKm = 1.5,
        distanceInMeters = 1500
    )
}
