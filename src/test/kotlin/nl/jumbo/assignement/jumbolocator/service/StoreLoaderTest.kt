package nl.jumbo.assignement.jumbolocator.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import nl.jumbo.assignement.jumbolocator.domain.error.DomainError
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.ReflectionTestUtils
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(SpringExtension::class)
class StoreLoaderTest {
    private lateinit var objectMapper: ObjectMapper
    private lateinit var storeLoader: StoreLoader
    private lateinit var validStoreJson: String

    @BeforeEach
    fun setup() {
        objectMapper = spyk(ObjectMapper())
        storeLoader = StoreLoader(objectMapper)

        validStoreJson = createValidStoreJson()

        // Mock the JSON resource
        val testResource = ByteArrayResource(validStoreJson.toByteArray())
        ReflectionTestUtils.setField(storeLoader, "storesJsonResource", testResource)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should successfully parse valid JSON`() {
        val result = storeLoader.loadStores()

        assertTrue(result.isRight())
        result.map { stores ->
            assertEquals(1, stores.size)

            val store = stores.first()
            assertStoreDetails(store)
        }
    }

    @Test
    fun `should handle resource access error`() {
        val failingResource: Resource = mockk {
            every { inputStream } throws IOException("Resource access error")
            every { description } returns "Test resource"
        }

        ReflectionTestUtils.setField(storeLoader, "storesJsonResource", failingResource)

        val result = storeLoader.loadStores()

        assertTrue(result.isLeft())
        result.mapLeft { error ->
            assertTrue(error is DomainError.UnexpectedError)
            assertTrue(error.message.contains("Failed to load stores from JSON"))
            assertTrue(error.cause is IOException)
        }
    }

    @Test
    fun `should handle malformed JSON`() {
        val malformedJson = "{invalid json content}"
        val malformedResource = ByteArrayResource(malformedJson.toByteArray())
        ReflectionTestUtils.setField(storeLoader, "storesJsonResource", malformedResource)

        val result = storeLoader.loadStores()

        assertTrue(result.isLeft())
        result.mapLeft { error ->
            assertTrue(error is DomainError.UnexpectedError)
            assertTrue(error.message.contains("Failed to load stores from JSON"))
        }
    }

    @Test
    fun `should handle JSON with missing required fields`() {
        val incompleteJson = """
            {
              "stores": [
                {
                  "city": "Amsterdam",
                  "postalCode": "1000AA"
                }
              ]
            }
        """.trimIndent()

        val incompleteResource = ByteArrayResource(incompleteJson.toByteArray())
        ReflectionTestUtils.setField(storeLoader, "storesJsonResource", incompleteResource)

        val result = storeLoader.loadStores()

        assertTrue(result.isLeft())
        result.mapLeft { error ->
            assertTrue(error is DomainError.UnexpectedError)
            assertTrue(error.message.contains("Failed to load stores from JSON"))
        }
    }

    // Helper methods
    private fun createValidStoreJson(): String = """
        {
          "stores": [
            {
              "city": "Amsterdam",
              "postalCode": "1000AA",
              "street": "Test Street",
              "street2": "Building 123",
              "street3": null,
              "addressName": "Jumbo Amsterdam",
              "longitude": "4.9041",
              "latitude": "52.3676",
              "complexNumber": "10001",
              "showWarningMessage": false,
              "todayOpen": "08:00",
              "locationType": "STORE",
              "collectionPoint": false,
              "sapStoreID": "10001",
              "todayClose": "22:00",
              "uuid": "123e4567-e89b-12d3-a456-426614174000"
            }
          ]
        }
    """.trimIndent()

    private fun assertStoreDetails(store: nl.jumbo.assignement.jumbolocator.domain.model.Store) {
        assertEquals(10001L, store.id)
        assertEquals("Jumbo Amsterdam", store.name)
        assertEquals("Amsterdam", store.address.city)
        assertEquals("1000AA", store.address.postalCode)
        assertEquals("Test Street", store.address.street)
        assertEquals("Building 123", store.address.street2)
        assertEquals(null, store.address.street3)
        assertEquals(52.3676, store.coordinates.latitude)
        assertEquals(4.9041, store.coordinates.longitude)
        assertEquals("08:00", store.openingHours.open)
        assertEquals("22:00", store.openingHours.close)
        assertEquals("STORE", store.locationType)
        assertEquals(false, store.isCollectionPoint)
    }
}
