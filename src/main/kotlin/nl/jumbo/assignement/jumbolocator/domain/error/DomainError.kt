package nl.jumbo.assignement.jumbolocator.domain.error

sealed class DomainError {
    abstract val message: String

    data class ResourceNotFound(
        override val message: String,
        val resourceType: String,
        val identifier: String
    ) : DomainError()

    data class ValidationError(
        override val message: String,
        val field: String? = null,
        val invalidValue: Any? = null
    ) : DomainError()

    data class DataAccessError(
        override val message: String,
        val cause: Throwable? = null
    ) : DomainError()

    data class GeospatialError(
        override val message: String,
        val latitude: Double? = null,
        val longitude: Double? = null
    ) : DomainError()

    data class UnexpectedError(
        override val message: String = "An unexpected error occurred",
        val cause: Throwable? = null
    ) : DomainError()
}
