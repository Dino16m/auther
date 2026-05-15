package com.hiz.plugins

import io.konform.validation.ValidationError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

open class ApplicationException(override val message: String) : Exception(message) {
    open val statusCode = HttpStatusCode.InternalServerError
}

class InvalidTokenException(override val message: String = "Invalid token") : ApplicationException(message) {
    override val statusCode = HttpStatusCode.Unauthorized
}

class Unauthenticated(override val message: String) : ApplicationException(message) {
    override val statusCode = HttpStatusCode.Unauthorized
}

class BadRequest(override val message: String) : ApplicationException(message) {
    override val statusCode = HttpStatusCode.BadRequest
}

class NotFound(override val message: String) : ApplicationException(message) {
    override val statusCode = HttpStatusCode.NotFound
}

class ValidationException(val errors: List<ValidationError>) : ApplicationException("Validation error") {
    override val statusCode = HttpStatusCode.BadRequest

    constructor(error: ValidationError) : this(listOf(error))
}

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            val errors = cause.errors.map { it.dataPath to it.message }
            call.respond(cause.statusCode, mapOf("errors" to errors))
        }
        exception<BadRequestException> { call, cause ->
            val message = cause.cause?.message ?: "Bad request"
            call.respond(HttpStatusCode.BadRequest, mapOf("message" to message))
        }
        exception<ApplicationException> { call, cause ->
            val message = cause.message.ifEmpty { "an error occurred" }
            call.respond(cause.statusCode, mapOf("message" to message))
        }

    }
}