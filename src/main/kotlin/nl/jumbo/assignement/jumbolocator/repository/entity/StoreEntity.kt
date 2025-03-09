package nl.jumbo.assignement.jumbolocator.repository.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.locationtech.jts.geom.Point
import java.time.LocalDateTime

@Entity
@Table(
    name = "stores",
    indexes = [
        Index(name = "idx_store_location", columnList = "location"),
        Index(name = "idx_store_store_id", columnList = "store_id", unique = true)
    ]
)
data class StoreEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val city: String,

    @Column(name = "postal_code", nullable = false)
    val postalCode: String,

    @Column(nullable = false)
    val street: String,

    @Column(name = "street2")
    val street2: String? = null,

    @Column(name = "street3")
    val street3: String? = null,

    @Column(name = "address_name", nullable = false)
    val addressName: String,

    @Column(nullable = false)
    val longitude: Double,

    @Column(nullable = false)
    val latitude: Double,

    @Column(name = "complex_number")
    val complexNumber: String,

    @Column(name = "show_warning_message")
    val showWarningMessage: Boolean,

    @Column(name = "today_open", nullable = false)
    val todayOpen: String,

    @Column(name = "location_type")
    val locationType: String,

    @Column(name = "collection_point")
    val collectionPoint: Boolean,

    @Column(name = "sap_store_id")
    val sapStoreID: String? = null,

    @Column(name = "today_close", nullable = false)
    val todayClose: String,

    @Column(columnDefinition = "geometry(Point,4326)")
    val location: Point,

    @Column(nullable = false)
    val uuid: String,

    @Column(name = "store_id", nullable = false)
    val storeId: Long,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
