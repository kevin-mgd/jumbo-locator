package nl.jumbo.assignement.jumbolocator.repository

import arrow.core.Either
import nl.jumbo.assignement.jumbolocator.domain.error.DomainError
import nl.jumbo.assignement.jumbolocator.domain.model.GeoCoordinates
import nl.jumbo.assignement.jumbolocator.domain.model.Store
import nl.jumbo.assignement.jumbolocator.domain.model.StoreWithDistance


interface StoreRepository {
    /**
     * Find nearest stores to given coordinates
     *
     * @param coordinates The location to search from
     * @param limit Maximum number of results to return
     * @return Either a DomainError or a List of stores with distances
     */
    fun findNearestStores(coordinates: GeoCoordinates, limit: Int): Either<DomainError, List<StoreWithDistance>>

    /**
     * Find a store by its unique ID
     *
     * @param id The store ID to search for
     * @return Either a DomainError or the Store if found
     */
    fun findById(id: Long): Either<DomainError, Store>

    /**
     * Save a list of stores to the repository
     *
     * @param stores The stores to save
     * @return Either a DomainError or the saved stores
     */
    fun saveAll(stores: List<Store>): Either<DomainError, List<Store>>

    /**
     * Count the number of stores in the repository
     *
     * @return Either a DomainError or the count
     */
    fun count(): Either<DomainError, Long>
}
