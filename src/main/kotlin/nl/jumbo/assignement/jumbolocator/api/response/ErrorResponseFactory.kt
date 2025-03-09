package nl.jumbo.assignement.jumbolocator.api.response

import nl.jumbo.assignement.jumbolocator.domain.error.DomainError
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ErrorResponseFactory {
    private val logger = LoggerFactory.getLogger(ErrorResponseFactory::class.java)

    fun createErrorResponse(
        error: DomainError,
        path: String
    ): ResponseEntity<ApiErrorResponse> {
        logger.warn("Domain error on path $path: ${error.message}")

        return when (error) {
            is DomainError.ResourceNotFound -> {
                val errorResponse = ApiErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = HttpStatus.NOT_FOUND.value(),
                    error = "Not Found",
                    message = error.message,
                    path = path
                )
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
            }

            is DomainError.ValidationError -> {
                val fieldInfo = error.field?.let { " (field: $it)" } ?: ""
                val errorResponse = ApiErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Validation Error",
                    message = "${error.message}$fieldInfo",
                    path = path
                )
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
            }

            is DomainError.GeospatialError -> {
                val errorResponse = ApiErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Geospatial Error",
                    message = error.message,
                    path = path
                )
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
            }

            is DomainError.DataAccessError -> {
                logger.error("Data access error", error.cause)
                val errorResponse = ApiErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    error = "Data Access Error",
                    message = "A database error occurred while processing your request",
                    path = path
                )
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
            }

            is DomainError.UnexpectedError -> {
                logger.error("Unexpected error", error.cause)
                val errorResponse = ApiErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    error = "Internal Server Error",
                    message = "An unexpected error occurred while processing your request",
                    path = path
                )
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
            }
        }
    }
}
