package nl.jumbo.assignement.jumbolocator.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import jakarta.transaction.Transactional
import nl.jumbo.assignement.jumbolocator.domain.error.DomainError
import nl.jumbo.assignement.jumbolocator.domain.model.Address
import nl.jumbo.assignement.jumbolocator.domain.model.GeoCoordinates
import nl.jumbo.assignement.jumbolocator.domain.model.OpeningHours
import nl.jumbo.assignement.jumbolocator.domain.model.Store
import nl.jumbo.assignement.jumbolocator.domain.model.StoreWithDistance
import nl.jumbo.assignement.jumbolocator.repository.entity.StoreEntity
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

@Repository
class StoreRepositoryPostgis(
    private val springDataRepository: PostgisDataStoreRepository
) : StoreRepository {

    private val logger = LoggerFactory.getLogger(StoreRepositoryPostgis::class.java)
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    override fun findNearestStores(
        coordinates: GeoCoordinates,
        limit: Int
    ): Either<DomainError, List<StoreWithDistance>> = runCatching {
        springDataRepository.findNearestStoresWithDistance(
            coordinates.latitude,
            coordinates.longitude,
            limit
        ).map { projection ->
            val distanceInKm = BigDecimal(projection.distance / 1000.0)
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            StoreWithDistance(
                store = Store(
                    id = projection.storeId,
                    name = projection.addressName,
                    address = Address(
                        street = projection.street,
                        street2 = projection.street2,
                        street3 = projection.street3,
                        city = projection.city,
                        postalCode = projection.postalCode
                    ),
                    coordinates = GeoCoordinates(
                        latitude = projection.latitude,
                        longitude = projection.longitude
                    ),
                    openingHours = OpeningHours(
                        open = projection.todayOpen,
                        close = projection.todayClose
                    ),
                    locationType = projection.locationType,
                    isCollectionPoint = projection.collectionPoint
                ),
                distanceInKm = distanceInKm,
                distanceInMeters = projection.distance.toInt()
            )
        }
    }.fold(
        onSuccess = { it.right() },
        onFailure = { e ->
            logger.error("Error finding nearest stores", e)
            DomainError.DataAccessError(
                message = "Failed to retrieve stores from database: ${e.message}",
                cause = e
            ).left()
        }
    )

    override fun findById(id: Long): Either<DomainError, Store> = runCatching {
        springDataRepository.findByStoreId(id)?.let { mapEntityToDomain(it) }
            ?: throw NoSuchElementException("Store with ID $id not found")
    }.fold(
        onSuccess = { it.right() },
        onFailure = { e ->
            when (e) {
                is NoSuchElementException -> DomainError.ResourceNotFound(
                    message = e.message ?: "Store not found",
                    resourceType = "Store",
                    identifier = id.toString()
                ).left()
                else -> {
                    logger.error("Error finding store by id $id", e)
                    DomainError.DataAccessError(
                        message = "Failed to retrieve store from database",
                        cause = e
                    ).left()
                }
            }
        }
    )

    @Transactional
    override fun saveAll(stores: List<Store>): Either<DomainError, List<Store>> = runCatching {
        springDataRepository.saveAll(stores.map { mapDomainToEntity(it) })
            .map { mapEntityToDomain(it) }
    }.fold(
        onSuccess = { it.right() },
        onFailure = { e ->
            logger.error("Error saving stores", e)
            DomainError.DataAccessError(
                message = "Failed to save stores to database",
                cause = e
            ).left()
        }
    )

    override fun count(): Either<DomainError, Long> = runCatching {
        springDataRepository.count()
    }.fold(
        onSuccess = { it.right() },
        onFailure = { e ->
            logger.error("Error counting stores", e)
            DomainError.DataAccessError(
                message = "Failed to count stores in database",
                cause = e
            ).left()
        }
    )

    private fun mapEntityToDomain(entity: StoreEntity): Store = Store(
        id = entity.storeId,
        name = entity.addressName,
        address = Address(
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
        openingHours = OpeningHours(
            open = entity.todayOpen,
            close = entity.todayClose
        ),
        locationType = entity.locationType,
        isCollectionPoint = entity.collectionPoint
    )

    private fun mapDomainToEntity(domain: Store): StoreEntity = StoreEntity(
        storeId = domain.id,
        addressName = domain.name,
        street = domain.address.street,
        street2 = domain.address.street2,
        street3 = domain.address.street3,
        city = domain.address.city,
        postalCode = domain.address.postalCode,
        latitude = domain.coordinates.latitude,
        longitude = domain.coordinates.longitude,
        location = geometryFactory.createPoint(
            Coordinate(domain.coordinates.longitude, domain.coordinates.latitude)
        ),
        todayOpen = domain.openingHours.open,
        todayClose = domain.openingHours.close,
        locationType = domain.locationType,
        collectionPoint = domain.isCollectionPoint,
        complexNumber = domain.id.toString(),
        showWarningMessage = false,
        sapStoreID = domain.id.toString(),
        uuid = UUID.randomUUID().toString()
    )
}
