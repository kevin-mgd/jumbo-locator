package nl.jumbo.assignement.jumbolocator.api.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Standardized error response")
data class ApiErrorResponse(
    @Schema(description = "Timestamp when the error occurred")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "HTTP status code")
    val status: Int,

    @Schema(description = "Error type")
    val error: String,

    @Schema(description = "Error message")
    val message: String,

    @Schema(description = "Request path that produced the error")
    val path: String
)
