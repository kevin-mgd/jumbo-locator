package nl.jumbo.assignement.jumbolocator.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import nl.jumbo.assignement.jumbolocator.domain.error.DomainError
import nl.jumbo.assignement.jumbolocator.domain.model.Address
import nl.jumbo.assignement.jumbolocator.domain.model.GeoCoordinates
import nl.jumbo.assignement.jumbolocator.domain.model.OpeningHours
import nl.jumbo.assignement.jumbolocator.domain.model.Store
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class StoreLoader(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(StoreLoader::class.java)

    @Value("classpath:stores.json")
    private lateinit var storesJsonResource: Resource

    fun loadStores(): Either<DomainError, List<Store>> {
        return try {
            logger.info("Loading stores from JSON file")
            val storesJson = storesJsonResource.inputStream.bufferedReader().use { it.readText() }

            val storesWrapper: StoresWrapper = objectMapper
                .registerKotlinModule()
                .readValue(storesJson)

            val stores = storesWrapper.stores.map { dto ->
                mapJsonDtoToDomain(dto)
            }

            logger.info("Successfully parsed ${stores.size} stores from JSON")
            stores.right()
        } catch (e: Exception) {
            logger.error("Failed to load stores from JSON file", e)
            DomainError.UnexpectedError(
                message = "Failed to load stores from JSON: ${e.message}",
                cause = e
            ).left()
        }
    }

    private fun mapJsonDtoToDomain(dto: StoreJsonDto): Store {
        val coordinates = GeoCoordinates.fromString(dto.latitude, dto.longitude)

        return Store(
            id = dto.complexNumber.toLong(),
            name = dto.addressName,
            address = Address(
                street = dto.street,
                street2 = dto.street2,
                street3 = dto.street3,
                city = dto.city,
                postalCode = dto.postalCode
            ),
            coordinates = coordinates,
            openingHours = OpeningHours(
                open = dto.todayOpen,
                close = dto.todayClose
            ),
            locationType = dto.locationType,
            isCollectionPoint = dto.collectionPoint
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class StoresWrapper(val stores: List<StoreJsonDto>)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class StoreJsonDto(
        val city: String,
        val postalCode: String,
        val street: String,
        val street2: String? = null,
        val street3: String? = null,
        val addressName: String,
        val longitude: String,
        val latitude: String,
        val complexNumber: String,
        val showWarningMessage: Boolean,
        val todayOpen: String,
        val locationType: String,
        val collectionPoint: Boolean,
        val sapStoreID: String,
        val todayClose: String,
        val uuid: String
    )
}
