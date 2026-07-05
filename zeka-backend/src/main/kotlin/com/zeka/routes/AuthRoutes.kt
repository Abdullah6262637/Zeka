package com.zeka.routes

import com.auth0.jwt.JWT
import com.zeka.config.SecurityConfig
import com.zeka.data.db.tables.Conversations
import com.zeka.data.db.tables.Messages
import com.zeka.data.db.tables.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val displayName: String,
    val email: String
)

@Serializable
data class ConversationCreateRequest(
    val title: String,
    val personaId: String? = null
)

@Serializable
data class ConversationResponse(
    val id: String,
    val title: String,
    val pinned: Boolean,
    val archived: Boolean,
    val createdAt: String
)

@Serializable
data class MessageResponse(
    val id: String,
    val role: String,
    val content: String,
    val providerUsed: String?,
    val modelUsed: String?,
    val createdAt: String
)

fun Route.authRoutes() {
    route("/api/v1/auth") {
        post("/register") {
            val req = call.receive<AuthRequest>()
            if (req.displayName.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Display name is required")
                return@post
            }

            val hash = hashPassword(req.password)
            val alreadyExists = transaction {
                Users.select { Users.email eq req.email }.count() > 0
            }

            if (alreadyExists) {
                call.respond(HttpStatusCode.Conflict, "Email already registered")
                return@post
            }

            val userId = transaction {
                Users.insert {
                    it[email] = req.email
                    it[passwordHash] = hash
                    it[displayName] = req.displayName
                } get Users.id
            }

            val token = generateToken(userId.value, req.displayName, req.email)
            call.respond(HttpStatusCode.Created, AuthResponse(token, userId.value.toString(), req.displayName, req.email))
        }

        post("/login") {
            val req = call.receive<AuthRequest>()
            val hash = hashPassword(req.password)

            val user = transaction {
                Users.select { (Users.email eq req.email) }.firstOrNull()
            }

            if (user == null || user[Users.passwordHash] != hash) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid email or password")
                return@post
            }

            val userId = user[Users.id].value
            val displayName = user[Users.displayName]
            val token = generateToken(userId, displayName, req.email)

            call.respond(AuthResponse(token, userId.toString(), displayName, req.email))
        }
    }

    authenticate("auth-jwt") {
        route("/api/v1/conversations") {
            // Get user's conversations
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("userId")?.asString())

                val convs = transaction {
                    Conversations.select { Conversations.userId eq userId }
                        .sortedByDescending { it[Conversations.updatedAt] }
                        .map {
                            ConversationResponse(
                                id = it[Conversations.id].value.toString(),
                                title = it[Conversations.title],
                                pinned = it[Conversations.pinned],
                                archived = it[Conversations.archived],
                                createdAt = it[Conversations.createdAt].toString()
                            )
                        }
                }
                call.respond(convs)
            }

            // Create new conversation
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("userId")?.asString())
                val req = call.receive<ConversationCreateRequest>()

                val convId = transaction {
                    Conversations.insert {
                        it[Conversations.userId] = userId
                        it[title] = req.title
                        it[personaId] = req.personaId?.let { pId -> UUID.fromString(pId) }
                        it[createdAt] = LocalDateTime.now()
                        it[updatedAt] = LocalDateTime.now()
                    } get Conversations.id
                }

                call.respond(HttpStatusCode.Created, mapOf("id" to convId.value.toString()))
            }

            // Get conversation details / message list
            get("/{id}/messages") {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("userId")?.asString())
                val convId = UUID.fromString(call.parameters["id"])

                // Verify owner
                val belongsToUser = transaction {
                    Conversations.select { 
                        (Conversations.id eq convId) and (Conversations.userId eq userId) 
                    }.count() > 0
                }

                if (!belongsToUser) {
                    call.respond(HttpStatusCode.Forbidden, "Conversation not found or access denied")
                    return@get
                }

                val messages = transaction {
                    Messages.select { Messages.conversationId eq convId }
                        .sortedBy { it[Messages.createdAt] }
                        .map {
                            MessageResponse(
                                id = it[Messages.id].value.toString(),
                                role = it[Messages.role],
                                content = it[Messages.content],
                                providerUsed = it[Messages.providerUsed],
                                modelUsed = it[Messages.modelUsed],
                                createdAt = it[Messages.createdAt].toString()
                            )
                        }
                }

                call.respond(messages)
            }
        }
    }
}

private fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

private fun generateToken(userId: UUID, displayName: String, email: String): String {
    return JWT.create()
        .withAudience(SecurityConfig.jwtAudience)
        .withIssuer(SecurityConfig.jwtIssuer)
        .withClaim("userId", userId.toString())
        .withClaim("displayName", displayName)
        .withClaim("email", email)
        .withExpiresAt(Date(System.currentTimeMillis() + 3600000 * 24)) // 24 hours expiry
        .sign(SecurityConfig.makeJwtAlgorithm())
}
