package com.hiz.auth

import com.hiz.plugins.validateAndThrowOnFailure
import io.konform.validation.Validation
import io.konform.validation.jsonschema.pattern
import kotlinx.serialization.Serializable

const val EmailRegex = ".+@.+\\.[a-z]+"

@Serializable
data class UserCreationDTO(
    var firstName: String,
    var lastName: String,
    var email: String,
    var password: String
) {
    init {
        Validation<UserCreationDTO> {
            UserCreationDTO::email {
                pattern(EmailRegex)
            }
        }.validateAndThrowOnFailure(this)
    }
}

@Serializable
data class UserResponse(
    var id: String,
    var firstName: String,
    var lastName: String,
    var email: String,
) {
    companion object {

        @JvmStatic
        fun fromUser(user: User): UserResponse {
            return UserResponse(
                firstName = user.firstName,
                lastName = user.lastName,
                email = user.email,
                id = user.id.value.toString(),
            )
        }

    }
}

@Serializable
data class LoginDTO(val email: String, val password: String)

@Serializable
data class AuthToken(val authToken: String, val refreshToken: String)

@Serializable
data class TokenRefreshRequest(val refreshToken: String)

@Serializable
data class PasswordChangeRequest(val oldPassword: String, val newPassword: String)

@Serializable
data class PasswordResetInitiationRequest(val email: String)

@Serializable
data class PasswordResetRequest(val password: String, val signature: String, val otp: String)

@Serializable
data class OTPSignature(val signature: String)
