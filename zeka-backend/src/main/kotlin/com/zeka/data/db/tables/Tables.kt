package com.zeka.data.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val displayName = varchar("display_name", 255)
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val biometricEnabled = bool("biometric_enabled").default(false)
}

object ProviderKeys : UUIDTable("provider_keys") {
    val userId = reference("user_id", Users)
    val provider = varchar("provider", 50) // anthropic, openai, google, deepseek, openrouter, ollama
    val encryptedApiKey = text("encrypted_api_key")
    val baseUrl = varchar("base_url", 500).nullable()
    val defaultModel = varchar("default_model", 100)
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

object Conversations : UUIDTable("conversations") {
    val userId = reference("user_id", Users)
    val title = varchar("title", 255)
    val personaId = uuid("persona_id").nullable()
    val pinned = bool("pinned").default(false)
    val archived = bool("archived").default(false)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}

object Messages : UUIDTable("messages") {
    val conversationId = reference("conversation_id", Conversations)
    val role = varchar("role", 20) // user, assistant, system
    val content = text("content")
    val providerUsed = varchar("provider_used", 50).nullable()
    val modelUsed = varchar("model_used", 100).nullable()
    val tokenCount = integer("token_count").nullable()
    val parentMessageId = uuid("parent_message_id").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

object Attachments : UUIDTable("attachments") {
    val messageId = reference("message_id", Messages)
    val fileType = varchar("file_type", 50) // pdf, image, txt, etc.
    val storageKey = varchar("storage_key", 500)
    val originalName = varchar("original_name", 255)
    val sizeBytes = long("size_bytes")
    val extractedText = text("extracted_text").nullable()
}

object Personas : UUIDTable("personas") {
    val userId = reference("user_id", Users).nullable() // null means global/system default personas
    val name = varchar("name", 100)
    val systemPrompt = text("system_prompt")
    val icon = varchar("icon", 100)
    val temperature = double("temperature").default(0.7)
    val topP = double("top_p").default(0.9)
    val maxTokens = integer("max_tokens").default(2048)
}
