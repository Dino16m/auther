package com.hiz.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.hiz.plugins.BadRequest
import com.hiz.plugins.InvalidTokenException
import com.hiz.plugins.NotFound
import com.hiz.plugins.Unauthenticated
import io.ktor.util.*
import java.time.Instant
import java.util.*
import kotlin.text.toCharArray

fun interface PasswordResetMessenger {
    fun sendMessage(user: User, code: String)
}

class PasswordResetService(
    private val userRepo: UserRepository,
    private val otpGenerator: OTPGenerator,
    private val signer: Signer,
    private val passwordResetMessenger: PasswordResetMessenger
) {
    fun resetPassword(code: String, signature: String, newPassword: String) {
        val otp = OTP(code = code, signature = signature)
        val data = otpGenerator.validateOTP(otp, "password-reset")
        val sub = data["sub"]!!
        val key = data["key"]!!
        val user = userRepo.findByEmail(sub) ?: throw NotFound("User not found")

        val valid = signer.verify(user.updatedAt.toString(), key.decodeBase64Bytes())
        if (!valid) throw InvalidTokenException("Invalid password reset token")

        user.updatePassword(newPassword)
        userRepo.save(user)
    }

    fun sendResetCode(email: String): String {
        var user = userRepo.findByEmail(email)
        if (user != null) user = userRepo.save(user)
        val lastModified = user?.updatedAt ?: Instant.now()
        val otp = generateOTP(email, lastModified)


        if (user != null) passwordResetMessenger.sendMessage(user, otp.code)

        return otp.signature

    }

    private fun generateOTP(email: String, lastModified: Instant): OTP {
        val key = signer.sign(lastModified.toString())
        val data = mapOf("sub" to email, "key" to key.encodeBase64())
        return otpGenerator.generateOTP(data, "password-reset")
    }
}

class AuthService(private val userRepo: UserRepository, private val jwtService: JWTService) {


    fun register(dto: UserCreationDTO): User {
        val user = User(
            id = UserId(UUID.randomUUID()),
            email = dto.email,
            firstName = dto.firstName,
            lastName = dto.lastName,
            password = "",
        )
        user.updatePassword(dto.password)
        userRepo.add(user)
        return user
    }

    fun changePassword(userId: UUID, oldPassword: String, newPassword: String) {
        val user = userRepo.findById(userId) ?: throw NotFound("User not found")

        user.checkPassword(oldPassword) || throw BadRequest("Invalid password")
        user.updatePassword(newPassword)
        userRepo.save(user)
    }

    fun login(dto: LoginDTO): AuthToken {
        val user = userRepo.findByEmail(dto.email) ?: throw Unauthenticated("Invalid username/password")
        val result = BCrypt.verifyer().verify(dto.password.toCharArray(), user.password.toCharArray())
        result.verified || throw Unauthenticated("Invalid username/password")
        return jwtService.generateToken(user)
    }

    fun logout(token: String) {
        jwtService.logout(token)
    }
}