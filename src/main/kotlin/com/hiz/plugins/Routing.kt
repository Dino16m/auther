package com.hiz.plugins

import com.hiz.auth.authRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.webjars.*

fun Application.configureRouting() {
    install(Webjars) {
        path = "/webjars" //defaults to /webjars
    }
    routing {
        authRoutes()
        authenticate {
            get("/protected") {
                call.respondText("Protected Hello World.")
            }
        }
        get("/") {
            call.respondText("Hello World!")
        }
        get("/webjars") {
            call.respondText("<script src='/webjars/jquery/jquery.js'></script>", ContentType.Text.Html)
        }
    }
}
