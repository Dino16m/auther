package com.hiz.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.JWTVerifier
import com.hiz.plugins.Config
import com.hiz.plugins.InvalidTokenException
import io.ktor.server.auth.jwt.*
import java.time.Instant
import java.util.*

enum class JWTPurpose {
    REFRESH, AUTH
}

fun interface DateFactory {
    fun now(): Instant
}

interface TokenBlacklister {
    fun blacklistFamily(family: String)
    fun blacklistJTI(jti: String)

    fun validateFamily(family: String): Boolean

    fun validateJTI(jti: String): Boolean
}

class InMemoryTokenBlacklister : TokenBlacklister {
    private val blacklistedFamilies: MutableSet<String> = mutableSetOf()
    private val blacklistedJTIs: MutableSet<String> = mutableSetOf()

    override fun blacklistFamily(family: String) {
        blacklistedFamilies.add(family)
    }

    override fun blacklistJTI(jti: String) {
        blacklistedJTIs.add(jti)
    }

    override fun validateFamily(family: String): Boolean {
        return !blacklistedFamilies.contains(family)
    }

    override fun validateJTI(jti: String): Boolean {
        return !blacklistedJTIs.contains(jti)
    }

}

class JWTService(
    private val config: Config,
    private val blacklister: TokenBlacklister,
    private val dateFactory: DateFactory
) {
    fun generateToken(user: User): AuthToken {
        val userId = user.id.value.toString()
        val tokenFamily = UUID.randomUUID().toString()
        return AuthToken(
            refreshToken = generateRefreshToken(userId, tokenFamily),
            authToken = generateAuthToken(userId, tokenFamily)
        )
    }

    fun logout(token: String) {
        try {
            val decoded = JWT.decode(token)
            val id = decoded.id ?: return
            val family = decoded.getClaim("fam").toString()
            blacklister.blacklistJTI(id)
            blacklister.blacklistFamily(family)
        } catch (e: JWTDecodeException) {
            return
        }

    }

    fun validateAuthToken(credential: JWTCredential): JWTPrincipal? {
        val tokenFamily = credential["fam"] ?: return null
        blacklister.validateFamily(tokenFamily) || return null
        val jwtId = credential.jwtId ?: return null
        blacklister.validateJTI(jwtId) || return null

        return JWTPrincipal(credential.payload)
    }

    fun refreshToken(token: String): AuthToken {
        try {
            val decodedToken = getRefreshVerifier().verify(token)
            val tokenFamily = decodedToken.getClaim("fam").toString()

            val hasValidFamily = blacklister.validateFamily(tokenFamily)
            if (!hasValidFamily) throw InvalidTokenException()

            val hasValidJTI = blacklister.validateJTI(decodedToken.id)
            if (!hasValidJTI) {
                blacklister.blacklistFamily(tokenFamily)
                throw InvalidTokenException()
            }
            blacklister.blacklistJTI(decodedToken.id)
            val userId = decodedToken.subject

            return AuthToken(
                refreshToken = generateRefreshToken(userId, tokenFamily),
                authToken = generateAuthToken(userId, tokenFamily)
            )
        } catch (e: JWTVerificationException) {
            throw InvalidTokenException()
        }

    }

    fun getAuthVerifier(): JWTVerifier {
        return JWT.require(Algorithm.HMAC256(config.jwtSecret))
            .withAudience(config.jwtAudience)
            .withIssuer(config.jwtDomain)
            .withClaim("use", JWTPurpose.AUTH.name)
            .build()
    }

    private fun getRefreshVerifier(): JWTVerifier {
        return JWT.require(Algorithm.HMAC256(config.jwtSecret))
            .withAudience(config.jwtAudience)
            .withIssuer(config.jwtDomain)
            .withClaim("use", JWTPurpose.REFRESH.name)
            .build()
    }

    private fun generateAuthToken(userId: String, tokenFamily: String): String {
        return JWT.create()
            .withAudience(config.jwtAudience)
            .withIssuer(config.jwtDomain)
            .withSubject(userId)
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("use", JWTPurpose.AUTH.name)
            .withClaim("fam", tokenFamily)
            .withExpiresAt(
                Date.from(dateFactory.now().plusMillis(config.jwtValidity.inWholeMilliseconds))
            ).sign(Algorithm.HMAC256(config.jwtSecret))
    }

    private fun generateRefreshToken(userId: String, tokenFamily: String): String {
        return JWT.create()
            .withAudience(config.jwtAudience)
            .withIssuer(config.jwtDomain)
            .withSubject(userId)
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("use", JWTPurpose.REFRESH.name)
            .withClaim("fam", tokenFamily)
            .withExpiresAt(
                Date.from(dateFactory.now().plusMillis(config.jwtRefreshValidity.inWholeMilliseconds))
            ).sign(Algorithm.HMAC256(config.jwtSecret))
    }


}