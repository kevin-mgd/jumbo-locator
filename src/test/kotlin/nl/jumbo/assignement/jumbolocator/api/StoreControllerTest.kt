package nl.jumbo.assignement.jumbolocator.api

import arrow.core.left
import arrow.core.right
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.jumbo.assignement.jumbolocator.api.response.ApiErrorResponse
import nl.jumbo.assignement.jumbolocator.api.response.ErrorResponseFactory
import nl.jumbo.assignement.jumbolocator.api.response.StoreResponse
import nl.jumbo.assignement.jumbolocator.domain.error.DomainError
import nl.jumbo.assignement.jumbolocator.service.StoreService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class StoreControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var storeService: StoreService
    private lateinit var errorResponseFactory: ErrorResponseFactory
    private lateinit var storeController: StoreController

    @BeforeEach
    fun setUp() {
        storeService = mockk()
        errorResponseFactory = mockk()
        storeController = StoreController(storeService, errorResponseFactory)

        mockMvc = MockMvcBuilders.standaloneSetup(storeController)
            .setControllerAdvice()
            .build()
    }

    @Nested
    @DisplayName("Find Nearest Stores")
    inner class FindNearestStoresTests {
        private val validLatitude = 52.3702
        private val validLongitude = 4.8952
        private val validLimit = 5

        @Test
        fun `should return nearest stores when valid parameters are provided`() {
            val stores = listOf(
                createSampleStore(1, "Amsterdam Store", validLatitude, validLongitude, 0.0),
                createSampleStore(2, "Rotterdam Store", 51.9244, 4.4777, 57.6)
            )

            every { storeService.findNearestStores(validLatitude, validLongitude, validLimit) } returns stores.right()

            mockMvc.perform(
                get("/api/v1/stores/nearest")
                    .param("latitude", validLatitude.toString())
                    .param("longitude", validLongitude.toString())
                    .param("limit", validLimit.toString())
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].storeId").value(1))
                .andExpect(jsonPath("$[0].name").value("Amsterdam Store"))
                .andExpect(jsonPath("$[1].storeId").value(2))
                .andExpect(jsonPath("$[1].name").value("Rotterdam Store"))

            verify(exactly = 1) { storeService.findNearestStores(validLatitude, validLongitude, validLimit) }
        }

        @Test
        fun `should return bad request when service returns validation error`() {
            val validationError = DomainError.ValidationError("Some validation error")
            val errorResponse = createErrorResponse(HttpStatus.BAD_REQUEST, "Validation error")

            every { storeService.findNearestStores(validLatitude, validLongitude, validLimit) } returns validationError.left()
            every {
                errorResponseFactory.createErrorResponse(validationError, any())
            } returns ResponseEntity.badRequest().body(errorResponse)

            mockMvc.perform(
                get("/api/v1/stores/nearest")
                    .param("latitude", validLatitude.toString())
                    .param("longitude", validLongitude.toString())
                    .param("limit", validLimit.toString())
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))

            verify(exactly = 1) { storeService.findNearestStores(validLatitude, validLongitude, validLimit) }
            verify(exactly = 1) { errorResponseFactory.createErrorResponse(validationError, any()) }
        }

        @Test
        fun `should return bad request when latitude exceeds maximum value`() {
            validateInvalidParameterRequest(
                latitude = "91.0",
                longitude = validLongitude.toString(),
                limit = validLimit.toString(),
                errorMessage = "Invalid latitude value"
            )
        }

        @Test
        fun `should return bad request when latitude is below minimum value`() {
            validateInvalidParameterRequest(
                latitude = "-91.0",
                longitude = validLongitude.toString(),
                limit = validLimit.toString(),
                errorMessage = "Invalid latitude value"
            )
        }

        @Test
        fun `should return bad request when longitude exceeds maximum value`() {
            validateInvalidParameterRequest(
                latitude = validLatitude.toString(),
                longitude = "181.0",
                limit = validLimit.toString(),
                errorMessage = "Invalid longitude value"
            )
        }

        @Test
        fun `should return bad request when longitude is below minimum value`() {
            validateInvalidParameterRequest(
                latitude = validLatitude.toString(),
                longitude = "-181.0",
                limit = validLimit.toString(),
                errorMessage = "Invalid longitude value"
            )
        }

        @Test
        fun `should return bad request when limit is below minimum value`() {
            validateInvalidParameterRequest(
                latitude = validLatitude.toString(),
                longitude = validLongitude.toString(),
                limit = "0",
                errorMessage = "Invalid limit value"
            )
        }

        @Test
        fun `should return bad request when limit exceeds maximum value`() {
            validateInvalidParameterRequest(
                latitude = validLatitude.toString(),
                longitude = validLongitude.toString(),
                limit = "21",
                errorMessage = "Invalid limit value"
            )
        }

        @Test
        fun `should return bad request when parameter has invalid format`() {
            mockMvc.perform(
                get("/api/v1/stores/nearest")
                    .param("latitude", "not-a-number")
                    .param("longitude", validLongitude.toString())
                    .param("limit", validLimit.toString())
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should return bad request when required parameter is missing`() {
            val missingParamError = DomainError.ValidationError("Required parameter 'latitude' is missing")
            val errorResponse = createErrorResponse(HttpStatus.BAD_REQUEST, "Required parameter 'latitude' is missing")

            every {
                errorResponseFactory.createErrorResponse(any(), any())
            } returns ResponseEntity.badRequest().body(errorResponse)

            mockMvc.perform(
                get("/api/v1/stores/nearest")
                    .param("longitude", validLongitude.toString())
                    .param("limit", validLimit.toString())
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
        }

        private fun validateInvalidParameterRequest(
            latitude: String,
            longitude: String,
            limit: String,
            errorMessage: String
        ) {
            val validationError = DomainError.ValidationError(errorMessage)
            val errorResponse = createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage)

            every {
                storeService.findNearestStores(any(), any(), any())
            } returns validationError.left()

            every {
                errorResponseFactory.createErrorResponse(any(), any())
            } returns ResponseEntity.badRequest().body(errorResponse)

            mockMvc.perform(
                get("/api/v1/stores/nearest")
                    .param("latitude", latitude)
                    .param("longitude", longitude)
                    .param("limit", limit)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("Get Store Details")
    inner class GetStoreDetailsTests {
        @Test
        fun `should return store details when valid store ID is provided`() {
            val storeId = 1L
            val store = createSampleStore(storeId.toInt(), "Amsterdam Store", 52.3702, 4.8952, 0.0)

            every { storeService.getStoreDetails(storeId) } returns store.right()

            mockMvc.perform(
                get("/api/v1/stores/$storeId")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.storeId").value(storeId.toInt()))
                .andExpect(jsonPath("$.name").value("Amsterdam Store"))
                .andExpect(jsonPath("$.latitude").value(52.3702))
                .andExpect(jsonPath("$.longitude").value(4.8952))

            verify(exactly = 1) { storeService.getStoreDetails(storeId) }
        }

        @Test
        fun `should return not found when store ID does not exist`() {
            val storeId = 999L
            val notFoundError = DomainError.ResourceNotFound(
                "Store not found with id: $storeId",
                "store",
                storeId.toString()
            )
            val errorResponse = createErrorResponse(HttpStatus.NOT_FOUND, "Store not found with id: $storeId")

            every { storeService.getStoreDetails(storeId) } returns notFoundError.left()
            every {
                errorResponseFactory.createErrorResponse(notFoundError, any())
            } returns ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)

            mockMvc.perform(
                get("/api/v1/stores/$storeId")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Store not found with id: $storeId"))

            verify(exactly = 1) { storeService.getStoreDetails(storeId) }
            verify(exactly = 1) { errorResponseFactory.createErrorResponse(notFoundError, any()) }
        }

        @Test
        fun `should return bad request when store ID has invalid format`() {
            mockMvc.perform(
                get("/api/v1/stores/not-a-number")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
        }
    }

    private fun createSampleStore(
        id: Int,
        name: String,
        lat: Double,
        lng: Double,
        distance: Double
    ): StoreResponse = StoreResponse(
        storeId = id.toLong(),
        name = name,
        address = "Main Street",
        city = "Amsterdam",
        postalCode = "1000 AA",
        latitude = lat,
        longitude = lng,
        distanceInKm = distance,
        distanceInMeters = (distance * 1000).toInt(),
        openingHours = "8:00",
        closingHours = "22:00",
        isCollectionPoint = false,
        lastUpdated = LocalDateTime.now()
    )

    private fun createErrorResponse(
        httpStatus: HttpStatus,
        message: String
    ): ApiErrorResponse = ApiErrorResponse(
        status = httpStatus.value(),
        error = httpStatus.reasonPhrase,
        message = message,
        path = "/api/v1/stores/nearest",
        timestamp = LocalDateTime.now()
    )
}
