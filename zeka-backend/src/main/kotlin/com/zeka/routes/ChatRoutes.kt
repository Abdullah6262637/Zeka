package com.zeka.routes

import com.zeka.config.SecurityConfig
import com.zeka.data.db.tables.Conversations
import com.zeka.data.db.tables.Messages
import com.zeka.data.db.tables.ProviderKeys
import com.zeka.llm.LlmProvider
import com.zeka.llm.ProviderRouter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ChatRequest(
    val conversationId: String,
    val provider: String, // openai, anthropic, google, deepseek, openrouter, ollama
    val model: String,
    val message: String,
    val systemPrompt: String? = null,
    val temperature: Double = 0.7
)

@Serializable
data class ProviderKeyRequest(
    val provider: String,
    val apiKey: String,
    val baseUrl: String? = null,
    val defaultModel: String
)

@Serializable
data class ProviderKeyResponse(
    val id: String,
    val provider: String,
    val maskedKey: String,
    val baseUrl: String?,
    val defaultModel: String,
    val isActive: Boolean
)

fun Route.chatRoutes() {
    authenticate("auth-jwt") {
        route("/api/v1/providers") {
            // Get all provider keys for user
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("userId")?.asString())
                
                val keys = transaction {
                    ProviderKeys.select { ProviderKeys.userId eq userId }
                        .map {
                            val decrypted = SecurityConfig.decrypt(it[ProviderKeys.encryptedApiKey])
                            ProviderKeyResponse(
                                id = it[ProviderKeys.id].value.toString(),
                                provider = it[ProviderKeys.provider],
                                maskedKey = SecurityConfig.maskApiKey(decrypted),
                                baseUrl = it[ProviderKeys.baseUrl],
                                defaultModel = it[ProviderKeys.defaultModel],
                                isActive = it[ProviderKeys.isActive]
                            )
                        }
                }
                call.respond(keys)
            }

            // Save new provider key
            post("/keys") {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("userId")?.asString())
                val req = call.receive<ProviderKeyRequest>()

                val encryptedKey = SecurityConfig.encrypt(req.apiKey)
                
                val keyId = transaction {
                    ProviderKeys.insert {
                        it[ProviderKeys.userId] = userId
                        it[ProviderKeys.provider] = req.provider.lowercase()
                        it[ProviderKeys.encryptedApiKey] = encryptedKey
                        it[ProviderKeys.baseUrl] = req.baseUrl
                        it[ProviderKeys.defaultModel] = req.defaultModel
                        it[ProviderKeys.isActive] = true
                    } get ProviderKeys.id
                }

                call.respond(HttpStatusCode.Created, mapOf("id" to keyId.toString()))
            }
        }

        route("/api/v1/chat") {
            post("/stream") {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("userId")?.asString())
                val req = call.receive<ChatRequest>()

                val convId = UUID.fromString(req.conversationId)

                // Verify user owns conversation
                val ownsConv = transaction {
                    Conversations.select { 
                        (Conversations.id eq convId) and (Conversations.userId eq userId) 
                    }.count() > 0
                }
                if (!ownsConv) {
                    call.respond(HttpStatusCode.Forbidden, "Conversation not found or access denied")
                    return@post
                }

                // Fetch API Key from DB
                val providerEnum = try {
                    LlmProvider.valueOf(req.provider.uppercase())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Unsupported provider: ${req.provider}")
                    return@post
                }

                val apiKey = ProviderRouter.getDecryptedKey(userId, req.provider.lowercase())
                if (apiKey == null) {
                    call.respond(HttpStatusCode.BadRequest, "No active API key found for provider: ${req.provider}")
                    return@post
                }

                // Fetch baseUrl
                val baseUrl = transaction {
                    ProviderKeys.select {
                        (ProviderKeys.userId eq userId) and 
                        (ProviderKeys.provider eq req.provider.lowercase()) and 
                        (ProviderKeys.isActive eq true)
                    }.firstOrNull()?.get(ProviderKeys.baseUrl)
                }

                // Save user message to DB
                transaction {
                    Messages.insert {
                        it[Messages.conversationId] = convId
                        it[role] = "user"
                        it[content] = req.message
                        it[providerUsed] = req.provider
                        it[modelUsed] = req.model
                    }
                }

                // Stream back responses using respondent bytes writer
                call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
                    val fullResponse = StringBuilder()

                    ProviderRouter.streamChat(
                        provider = providerEnum,
                        apiKey = apiKey,
                        baseUrl = baseUrl,
                        modelName = req.model,
                        systemPrompt = req.systemPrompt,
                        prompt = req.message,
                        temperature = req.temperature
                    ).catch { error ->
                        writeStringUtf8("data: Error: ${error.localizedMessage}\n\n")
                        flush()
                    }.collect { token ->
                        fullResponse.append(token)
                        // Escape JSON characters if needed, or send raw token as simple text
                        // Send token in standard SSE format
                        writeStringUtf8("data: $token\n\n")
                        flush()
                    }

                    // Save assistant message to DB
                    if (fullResponse.isNotEmpty()) {
                        transaction {
                            Messages.insert {
                                it[Messages.conversationId] = convId
                                it[role] = "assistant"
                                it[content] = fullResponse.toString()
                                it[providerUsed] = req.provider
                                it[modelUsed] = req.model
                            }
                        }
                    }

                    writeStringUtf8("data: [DONE]\n\n")
                    flush()
                }
            }
        }
    }
}
