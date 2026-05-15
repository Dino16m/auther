package com.hiz.plugins

import io.ktor.server.application.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class Config(private val environment: ApplicationEnvironment) {
    val jwtAudience by lazy { environment.config.property("jwt.audience").getString() }
    val jwtDomain by lazy { environment.config.property("jwt.domain").getString() }
    val jwtSecret by lazy { environment.config.property("jwt.secret").getString() }
    val jwtValidity by lazy {
        environment.config.property("jwt.validityMinutes").getString().toDouble().toDuration(DurationUnit.MINUTES)
    }
    val jwtRefreshValidity by lazy {
        environment.config.property("jwt.refreshTokenValidityMinutes").getString().toDouble()
            .toDuration(DurationUnit.MINUTES)
    }

    val databaseURL by lazy {
        environment.config.property("db.url").toString()
    }
    val databaseUser by lazy {
        environment.config.property("db.user").toString()
    }

    val databasePassword by lazy {
        environment.config.property("db.password").toString()
    }

    val secretKey by lazy { environment.config.property("jwt.secret").getString() }
}