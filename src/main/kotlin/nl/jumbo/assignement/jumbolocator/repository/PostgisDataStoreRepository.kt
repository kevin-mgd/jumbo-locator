package nl.jumbo.assignement.jumbolocator.repository

import nl.jumbo.assignement.jumbolocator.repository.entity.StoreEntity
import nl.jumbo.assignement.jumbolocator.repository.projection.StoreWithDistanceProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PostgisDataStoreRepository : JpaRepository<StoreEntity, Long> {

    fun findByStoreId(storeId: Long): StoreEntity?

    /**
     * Find nearest stores using PostGIS spatial functions
     * Returns stores sorted by distance from the provided coordinates
     */
    @Query(value = """
        SELECT s.*,
               ST_Distance(s.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distance
        FROM stores s 
        ORDER BY distance
        LIMIT :limit
    """, nativeQuery = true)
    fun findNearestStoresWithDistance(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("limit") limit: Int
    ): List<StoreWithDistanceProjection>
}
