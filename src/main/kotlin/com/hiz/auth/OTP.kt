package com.hiz.auth

import com.hiz.plugins.InvalidTokenException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class OTP(val code: String, val signature: String)

@Serializable
private data class OTPClaim(val code: String, val data: Map<String, String>, val expiry: Long, val purpose: String)

@Serializable
private data class PublicOTPClaim(val data: Map<String, String>, val expiry: Long, val purpose: String)

class OTPGenerator(private val signer: Signer, private val dateFactory: DateFactory) {

    fun generateOTP(data: Map<String, String>, purpose: String, validity: Duration = 5.minutes): OTP {
        val random = SecureRandom.getInstanceStrong()
        val code = random.nextInt(11110, 99999).toString()
        val expiry = dateFactory.now().plusSeconds(validity.inWholeSeconds).epochSecond
        val claim = OTPClaim(code = code, data = data, purpose = purpose, expiry = expiry)
        val signature = signer.sign(Json.encodeToString(claim))
        val publicClaim = PublicOTPClaim(data = data, expiry = expiry, purpose = purpose)
        val publicClaimJson = Json.encodeToString(publicClaim)

        val base64EncodedSignature = Base64.getUrlEncoder().encodeToString(signature)
        val base64EncodedPublicClaim = Base64.getUrlEncoder().encodeToString(publicClaimJson.encodeToByteArray())
        return OTP(
            code = code,
            signature = "${base64EncodedPublicClaim}.${base64EncodedSignature}"
        )
    }

    @Throws(InvalidTokenException::class)
    fun validateOTP(otp: OTP, purpose: String): Map<String, String> {
        try {
            val parts = otp.signature.split(".")
            if (parts.size != 2) throw InvalidTokenException()

            val base64EncodedPublicClaim = parts[0]
            val base64EncodedSignature = parts[1]

            val publicClaimJson = Base64.getUrlDecoder().decode(base64EncodedPublicClaim).decodeToString()
            val signature = Base64.getUrlDecoder().decode(base64EncodedSignature)
            val publicClaim = Json.decodeFromString<PublicOTPClaim>(publicClaimJson)

            val claim = OTPClaim(
                code = otp.code,
                data = publicClaim.data,
                purpose = purpose,
                expiry = publicClaim.expiry
            )

            val expectedSignature = signer.sign(Json.encodeToString(claim))
            if (!expectedSignature.contentEquals(signature)) throw InvalidTokenException()

            if (dateFactory.now().epochSecond > claim.expiry) throw InvalidTokenException("token expired")

            return claim.data
        } catch (e: IllegalArgumentException) {
            throw InvalidTokenException("Invalid data")
        }

    }

}