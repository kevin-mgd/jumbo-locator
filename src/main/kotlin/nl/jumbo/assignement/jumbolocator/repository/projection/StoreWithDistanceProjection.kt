package nl.jumbo.assignement.jumbolocator.repository.projection

interface StoreWithDistanceProjection {
    // Store entity properties
    val id: Long?
    val storeId: Long
    val city: String
    val postalCode: String
    val street: String
    val street2: String?
    val street3: String?
    val addressName: String
    val longitude: Double
    val latitude: Double
    val complexNumber: String
    val showWarningMessage: Boolean
    val todayOpen: String
    val locationType: String
    val collectionPoint: Boolean
    val sapStoreID: String
    val todayClose: String
    val uuid: String

    // Distance calculation from PostGIS
    val distance: Double
}
