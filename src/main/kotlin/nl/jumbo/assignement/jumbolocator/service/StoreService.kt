package nl.jumbo.assignement.jumbolocator.service

import arrow.core.Either
import arrow.core.left
import jakarta.annotation.PostConstruct
import nl.jumbo.assignement.jumbolocator.api.response.StoreResponse
import nl.jumbo.assignement.jumbolocator.domain.error.DomainError
import nl.jumbo.assignement.jumbolocator.domain.model.GeoCoordinates
import nl.jumbo.assignement.jumbolocator.domain.model.StoreWithDistance
import nl.jumbo.assignement.jumbolocator.repository.StoreRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class StoreService(
    private val storeRepository: StoreRepository,
    private val storeLoader: StoreLoader
) {
    private val logger = LoggerFactory.getLogger(StoreService::class.java)

    @PostConstruct
    fun initializeStores() {
        storeRepository.count().fold(
            ifLeft = { error ->
                logger.error("Failed to check store count: ${error.message}")
            },
            ifRight = { count ->
                if (count == 0L) {
                    logger.info("No stores found in database, loading from JSON")
                    storeLoader.loadStores().fold(
                        ifLeft = { error ->
                            logger.error("Failed to load stores: ${error.message}")
                        },
                        ifRight = { stores ->
                            storeRepository.saveAll(stores).fold(
                                ifLeft = { error ->
                                    logger.error("Failed to save stores: ${error.message}")
                                },
                                ifRight = { savedStores ->
                                    logger.info("Successfully loaded ${savedStores.size} stores")
                                }
                            )
                        }
                    )
                } else {
                    logger.info("Found $count stores, skipping initialization")
                }
            }
        )
    }

    @Cacheable(value = ["nearestStores"], key = "#latitude.toString() + '-' + #longitude.toString() + '-' + #limit")
    fun findNearestStores(latitude: Double, longitude: Double, limit: Int): Either<DomainError, List<StoreResponse>> {
        logger.info("Finding $limit nearest stores to ($latitude, $longitude)")

        val coordinates = try {
            GeoCoordinates(latitude, longitude)
        } catch (e: IllegalArgumentException) {
            return DomainError.ValidationError(
                message = e.message ?: "Invalid coordinates",
                field = "coordinates"
            ).left()
        }
        if (limit <= 0 || limit > 20) {
            return DomainError.ValidationError(
                message = "Limit must be between 1 and 20",
                field = "limit",
                invalidValue = limit
            ).left()
        }

        return storeRepository.findNearestStores(coordinates, limit)
            .map { storesWithDistance ->
                storesWithDistance.map { storeWithDistance ->
                    mapToStoreResponse(storeWithDistance)
                }
            }
    }

    @Cacheable(value = ["storeDetails"], key = "#storeId")
    fun getStoreDetails(storeId: Long): Either<DomainError, StoreResponse> {
        logger.info("Getting details for store $storeId")

        if (storeId <= 0) {
            return DomainError.ValidationError(
                message = "Store ID must be positive",
                field = "storeId",
                invalidValue = storeId
            ).left()
        }

        return storeRepository.findById(storeId)
            .map { store ->
                mapToStoreResponse(StoreWithDistance(store, 0.0, 0))
            }
    }

    private fun mapToStoreResponse(storeWithDistance: StoreWithDistance): StoreResponse {
        val store = storeWithDistance.store
        return StoreResponse(
            storeId = store.id,
            name = store.name,
            address = store.address.asFormattedString(),
            city = store.address.city,
            postalCode = store.address.postalCode,
            latitude = store.coordinates.latitude,
            longitude = store.coordinates.longitude,
            distanceInKm = storeWithDistance.distanceInKm,
            distanceInMeters = storeWithDistance.distanceInMeters,
            openingHours = store.openingHours.open,
            closingHours = store.openingHours.close,
            isCollectionPoint = store.isCollectionPoint,
            lastUpdated = LocalDateTime.now()
        )
    }
}
