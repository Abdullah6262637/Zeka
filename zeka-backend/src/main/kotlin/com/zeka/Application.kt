package com.zeka

import com.auth0.jwt.JWT
import com.zeka.config.DatabaseConfig
import com.zeka.config.SecurityConfig
import com.zeka.routes.authRoutes
import com.zeka.routes.chatRoutes
import com.zeka.routes.fileRoutes
import com.zeka.routes.personaRoutes
import com.zeka.routes.agentRoutes
import com.zeka.plugins.configureMonitoring
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize Database
    DatabaseConfig.init()

    // Configure Monitoring
    configureMonitoring()

    // Configure Content Negotiation
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        })
    }

    // Configure CORS
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost() // Suitable for local/testing. Can narrow down in production.
    }

    // Configure Error Status Pages
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to cause.localizedMessage))
        }
    }

    // Configure JWT Authentication
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                JWT.require(SecurityConfig.makeJwtAlgorithm())
                    .withAudience(SecurityConfig.jwtAudience)
                    .withIssuer(SecurityConfig.jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is invalid or expired")
            }
        }
    }

    // Register Routes
    routing {
        authRoutes()
        chatRoutes()
        fileRoutes()
        personaRoutes()
        agentRoutes()
    }
}
