package com.hiz.auth

import com.hiz.plugins.Container
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*


fun Route.authRoutes() {
    route("/auth") {
        val container = Container.instance
        val authService = container.authService
        post("/login") {
            val body = call.receive<LoginDTO>()
            val token = authService.login(body)
            call.respond(token)
        }

        post("/logout") {
            val token = call.request.headers["Authorization"]
            if (token != null) {
                authService.logout(token)
            }
            call.respond(HttpStatusCode.OK)
        }

        post("/register") {
            val body = call.receive<UserCreationDTO>()
            authService.register(body)
            call.respond(HttpStatusCode.OK)
        }
        post("/refresh") {
            val body = call.receive<TokenRefreshRequest>()
            val jwtService = container.jwtService
            val token = jwtService.refreshToken(body.refreshToken)
            call.respond(token)
        }
        post("/password/reset/request") {
            val body = call.receive<PasswordResetInitiationRequest>()
            val passwordResetService = container.passwordResetService
            val signature = passwordResetService.sendResetCode(body.email)
            call.respond(OTPSignature(signature))
        }
        post("/password/reset") {
            val body = call.receive<PasswordResetRequest>()
            val passwordResetService = container.passwordResetService
            passwordResetService.resetPassword(body.otp, body.signature, body.password)
            call.respond(HttpStatusCode.OK)
        }
        authenticate {
            get("/me") {
                val userRepo = container.userRepository
                val principal = call.principal<JWTPrincipal>()

                val user = userRepo.findById(UUID.fromString(principal!!.subject!!))

                call.respond<UserResponse>(UserResponse.fromUser(user!!))
            }

            post("/password/change") {
                val body = call.receive<PasswordChangeRequest>()
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal!!.subject!!)
                authService.changePassword(userId, body.oldPassword, body.newPassword)
                call.respond(HttpStatusCode.OK)
            }
        }

    }
}