package com.hiz.plugins

import com.hiz.auth.*
import io.ktor.server.application.*
import org.ktorm.database.Database
import java.time.Instant

class Container(private val environment: ApplicationEnvironment) {
    private val config by lazy { Config(environment) }
    private val tokenBlacklister by lazy { InMemoryTokenBlacklister() }
    val userRepository by lazy { DBUserRepository(database = database) }
    private val dateFactory by lazy {
        DateFactory { Instant.now() }
    }
    private val database by lazy {
        Database.connect(
            url = config.databaseURL,
            driver = "org.postgresql.Driver",
            user = config.databaseUser,
            password = config.databasePassword
        )
    }
    val jwtService by lazy {
        JWTService(
            config = config,
            blacklister = tokenBlacklister,
            dateFactory = dateFactory
        )
    }

    private val signer by lazy {
        HMACSigner(
            secretKey = config.secretKey
        )
    }

    private val otpGenerator by lazy {
        OTPGenerator(signer, dateFactory)
    }

    private val passwordResetMessenger by lazy {
        PasswordResetMessenger { user, code -> println("sending code $code to user ${user.firstName}:${user.email}") }
    }

    val passwordResetService by lazy {
        PasswordResetService(
            userRepository,
            otpGenerator,
            signer,
            passwordResetMessenger
        )
    }
    val authService by lazy {
        AuthService(
            userRepository, jwtService
        )
    }

    companion object {
        private var _instance: Container? = null

        val instance: Container
            get() = _instance ?: throw RuntimeException("Container not initialized")

        fun init(environment: ApplicationEnvironment) {
            if (_instance == null) {
                _instance = Container(environment)
            }
        }
    }
}

fun Application.configureContainer() {
    Container.init(environment)
}