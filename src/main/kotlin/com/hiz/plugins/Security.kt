package com.hiz.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    val jwtService = Container.instance.jwtService
    authentication {
        jwt {
            verifier(
                jwtService.getAuthVerifier()
            )
            validate { credential -> jwtService.validateAuthToken(credential) }
        }
    }
}
