package nl.jumbo.assignement.jumbolocator.domain.model

data class Store(
    val id: Long,
    val name: String,
    val address: Address,
    val coordinates: GeoCoordinates,
    val openingHours: OpeningHours,
    val locationType: String,
    val isCollectionPoint: Boolean
)

data class Address(
    val street: String,
    val street2: String? = null,
    val street3: String? = null,
    val city: String,
    val postalCode: String
) {
    fun asFormattedString(): String = buildString {
        append(street)
        if (!street2.isNullOrBlank()) {
            append(" ")
            append(street2)
        }
        if (!street3.isNullOrBlank()) {
            append(" ")
            append(street3)
        }
    }.trim()
}

data class GeoCoordinates(val latitude: Double, val longitude: Double) {
    init {
        require(latitude >= -90 && latitude <= 90) { "Latitude must be between -90 and 90" }
        require(longitude >= -180 && longitude <= 180) { "Longitude must be between -180 and 180" }
    }

    companion object {
        fun fromString(lat: String, lon: String): GeoCoordinates {
            return try {
                GeoCoordinates(lat.toDouble(), lon.toDouble())
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Invalid coordinate format: lat=$lat, lon=$lon", e)
            }
        }
    }
}

data class OpeningHours(val open: String, val close: String)

data class StoreWithDistance(
    val store: Store,
    val distanceInKm: Double,
    val distanceInMeters: Int
)
