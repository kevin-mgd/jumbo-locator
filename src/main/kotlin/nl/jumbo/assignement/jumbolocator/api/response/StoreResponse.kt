package nl.jumbo.assignement.jumbolocator.api.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Store information with distance from requested coordinates")
data class StoreResponse(
    @Schema(description = "Unique identifier for the store")
    val storeId: Long,

    @Schema(description = "Store name")
    val name: String,

    @Schema(description = "Full address")
    val address: String,

    @Schema(description = "City name")
    val city: String,

    @Schema(description = "Postal code")
    val postalCode: String,

    @Schema(description = "Latitude coordinate")
    val latitude: Double,

    @Schema(description = "Longitude coordinate")
    val longitude: Double,

    @Schema(description = "Distance in kilometers from requested coordinates")
    val distanceInKm: Double,
    @Schema(description = "Distance in meters from requested coordinates")
    val distanceInMeters: Int,
    @Schema(description = "Opening hours")
    val openingHours: String,

    @Schema(description = "Closing hours")
    val closingHours: String,

    @Schema(description = "Whether this location is a collection point")
    val isCollectionPoint: Boolean,

    @Schema(description = "When this data was last updated")
    val lastUpdated: LocalDateTime = LocalDateTime.now(),

)
