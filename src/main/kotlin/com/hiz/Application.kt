package com.hiz

import com.hiz.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureContainer()
    configureHTTP()
    configureSecurity()
    configureSerialization()
    configureRouting()
    configureErrorHandling()
}
