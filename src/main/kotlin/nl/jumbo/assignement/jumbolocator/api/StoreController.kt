package nl.jumbo.assignement.jumbolocator.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import nl.jumbo.assignement.jumbolocator.api.response.ApiErrorResponse
import nl.jumbo.assignement.jumbolocator.api.response.ErrorResponseFactory
import nl.jumbo.assignement.jumbolocator.service.StoreService
import org.slf4j.LoggerFactory
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/v1/stores")
@Tag(name = "Store API", description = "API for Jumbo store locations")
@Validated
class StoreController(
    private val storeService: StoreService,
    private val errorResponseFactory: ErrorResponseFactory
) {
    private val logger = LoggerFactory.getLogger(StoreController::class.java)

    @Operation(
        summary = "Find nearest stores",
        description = "Returns the closest Jumbo stores to a given position"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved nearest stores"
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid input parameters",
        content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
    )
    @GetMapping("/nearest")
    fun findNearestStores(
        @Parameter(description = "Latitude coordinate")
        @RequestParam @Min(-90) @Max(90) latitude: Double,

        @Parameter(description = "Longitude coordinate")
        @RequestParam @Min(-180) @Max(180) longitude: Double,

        @Parameter(description = "Maximum number of stores to return (default: 5)")
        @RequestParam(defaultValue = "5") @Min(1) @Max(20) limit: Int,

        request: HttpServletRequest?
    ): ResponseEntity<*> {
        logger.info("Finding nearest stores at coordinates: lat=$latitude, lng=$longitude, limit=$limit")

        return storeService.findNearestStores(latitude, longitude, limit).fold(
            ifLeft = { error ->
                errorResponseFactory.createErrorResponse(error, request?.requestURI ?: "")
            },
            ifRight = { stores ->
                ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.MINUTES))
                    .body(stores)
            }
        )
    }

    @Operation(
        summary = "Get store details",
        description = "Returns detailed information about a specific store"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved store details"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Store not found",
        content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
    )
    @GetMapping("/{storeId}")
    fun getStoreDetails(
        @PathVariable storeId: Long,
        request: HttpServletRequest?
    ): ResponseEntity<*> {
        logger.info("Fetching details for store with ID: $storeId")

        return storeService.getStoreDetails(storeId).fold(
            ifLeft = { error ->
                errorResponseFactory.createErrorResponse(error, request?.requestURI ?: "")
            },
            ifRight = { store ->
                ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                    .body(store)
            }
        )
    }
}
