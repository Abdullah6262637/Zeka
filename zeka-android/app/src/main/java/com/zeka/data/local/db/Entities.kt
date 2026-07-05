package com.zeka.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val personaId: String?,
    val pinned: Boolean,
    val archived: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val providerUsed: String?,
    val modelUsed: String?,
    val time: String,
    val isPendingSync: Boolean = false
)
