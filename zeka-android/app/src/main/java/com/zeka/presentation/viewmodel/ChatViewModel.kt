package com.zeka.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeka.data.local.db.MessageEntity
import com.zeka.data.local.db.ZekaDatabase
import com.zeka.presentation.ui.chat.Message
import com.zeka.presentation.ui.chat.Attachment
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID

@Serializable
data class ChatStreamRequest(
    val conversationId: String,
    val provider: String,
    val model: String,
    val message: String,
    val systemPrompt: String? = null,
    val temperature: Double = 0.7
)

class ChatViewModel : ViewModel() {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _toolStatus = MutableStateFlow<String?>(null)
    val toolStatus = _toolStatus.asStateFlow()

    private val _errorFlow = MutableStateFlow<String?>(null)
    val errorFlow = _errorFlow.asStateFlow()

    private val _conversations = MutableStateFlow<List<com.zeka.data.local.db.ConversationEntity>>(emptyList())
    val conversations = _conversations.asStateFlow()

    val messages = mutableStateListOf<Message>()

    private val backendUrl = "http://10.0.2.2:8080"

    private val localModelManager = com.zeka.data.local.ai.LocalModelManager()

    private var db: ZekaDatabase? = null

    fun initDatabase(context: Context) {
        if (db == null) {
            db = ZekaDatabase.getDatabase(context)
            loadConversations()
        }
    }

    fun loadConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = db?.conversationDao() ?: return@launch
            val list = dao.getAllConversations()
            _conversations.value = list
        }
    }

    fun loadCachedMessages(conversationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = db?.messageDao() ?: return@launch
            val entities = dao.getMessagesForConversation(conversationId)
            val uiMessages = entities.map {
                Message(
                    id = it.id,
                    role = it.role,
                    content = it.content,
                    time = it.time
                )
            }
            viewModelScope.launch(Dispatchers.Main) {
                messages.clear()
                messages.addAll(uiMessages)
            }
        }
    }

    fun sendMessageStream(
        context: Context,
        conversationId: String,
        provider: String,
        model: String,
        prompt: String,
        authToken: String,
        attachment: Attachment? = null
    ) {
        val userMsgId = UUID.randomUUID().toString()
        val assistantMsgId = UUID.randomUUID().toString()

        val userTokens = Math.max(1, prompt.length / 4)
        val inputRate = when {
            model.contains("claude", ignoreCase = true) || provider.contains("anthropic", ignoreCase = true) -> 3.0 / 1_000_000.0
            model.contains("gpt", ignoreCase = true) || provider.contains("openai", ignoreCase = true) -> 5.0 / 1_000_000.0
            model.contains("deepseek", ignoreCase = true) -> 0.14 / 1_000_000.0
            model == "Yerel Çevrimdışı Model" -> 0.0
            else -> 2.0 / 1_000_000.0
        }
        val userCost = userTokens * inputRate

        val outputRate = when {
            model.contains("claude", ignoreCase = true) || provider.contains("anthropic", ignoreCase = true) -> 15.0 / 1_000_000.0
            model.contains("gpt", ignoreCase = true) || provider.contains("openai", ignoreCase = true) -> 15.0 / 1_000_000.0
            model.contains("deepseek", ignoreCase = true) -> 0.28 / 1_000_000.0
            model == "Yerel Çevrimdışı Model" -> 0.0
            else -> 10.0 / 1_000_000.0
        }

        // 1. Add User message to local state
        messages.add(
            Message(
                id = userMsgId,
                role = "user",
                content = prompt,
                time = "now",
                attachment = attachment,
                tokens = userTokens,
                cost = userCost
            )
        )

        // 2. Add empty streaming response container
        val responseContainer = Message(
            id = assistantMsgId,
            role = "assistant",
            content = "",
            time = "now",
            isStreaming = true,
            tokens = 0,
            cost = 0.0
        )
        messages.add(responseContainer)

        viewModelScope.launch(Dispatchers.IO) {
            // Check if conversation is new, auto-create it
            val currentConvs = _conversations.value
            if (currentConvs.none { it.id == conversationId }) {
                val title = if (prompt.length > 28) prompt.take(25) + "..." else prompt
                val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                db?.conversationDao()?.insertOrUpdate(
                    com.zeka.data.local.db.ConversationEntity(
                        id = conversationId,
                        title = title,
                        personaId = null,
                        pinned = false,
                        archived = false,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                loadConversations()
            }

            // Save User message to Room database
            db?.messageDao()?.insertOrUpdate(
                MessageEntity(
                    id = userMsgId,
                    conversationId = conversationId,
                    role = "user",
                    content = prompt,
                    providerUsed = provider,
                    modelUsed = model,
                    time = "now"
                )
            )

            // Intercept message for Skills & Local MCP integrations
            var finalPrompt = prompt
            var skillSystemInstruction = ""

            // A. Custom Skills Processing
            val skillsList = com.zeka.data.local.model.ConfiguredSkillStore.loadSkills(context)
            for (skill in skillsList) {
                if (prompt.contains(skill.triggerKeyword, ignoreCase = true)) {
                    skillSystemInstruction = skill.promptInstruction
                    finalPrompt = finalPrompt.replace(skill.triggerKeyword, "", ignoreCase = true).trim()
                    break
                }
            }

            // B. Local MCP Tool Check
            val lowerPrompt = finalPrompt.lowercase()
            if (lowerPrompt.contains("takvim") || lowerPrompt.contains("etkinlik") || lowerPrompt.contains("calendar")) {
                _toolStatus.value = "Yerel Takvim Okunuyor..."
                kotlinx.coroutines.delay(1200)
                val calData = com.zeka.data.local.mcp.LocalMcpEngine.readCalendarEvents(context)
                finalPrompt = "[Yerel MCP Araç Çıktısı - Takvim]:\n$calData\n\nKullanıcı Sorusu:\n$finalPrompt"
                _toolStatus.value = null
            } else if (lowerPrompt.contains("rehber") || lowerPrompt.contains("kişi") || lowerPrompt.contains("contacts")) {
                _toolStatus.value = "Cihaz Rehberi Sorgulanıyor..."
                kotlinx.coroutines.delay(1200)
                var searchName: String? = null
                val words = finalPrompt.split(" ", "'", "\"")
                for (word in words) {
                    if (word.length > 2 && word != "rehber" && word != "rehberden" && word != "kişi" && word != "bul" && word != "ara") {
                        searchName = word
                        break
                    }
                }
                val contactsData = com.zeka.data.local.mcp.LocalMcpEngine.readContacts(context, searchName)
                finalPrompt = "[Yerel MCP Araç Çıktısı - Rehber]:\n$contactsData\n\nKullanıcı Sorusu:\n$finalPrompt"
                _toolStatus.value = null
            }

            if (model == "Yerel Çevrimdışı Model") {
                val localPrompt = if (skillSystemInstruction.isNotBlank()) {
                    "System: $skillSystemInstruction\n\nUser: $finalPrompt"
                } else {
                    finalPrompt
                }
                val responseContent = StringBuilder()
                try {
                    localModelManager.generateLocalResponseStream(localPrompt).collect { token ->
                        responseContent.append(token)
                        val outTokens = Math.max(1, responseContent.length / 4)
                        updateMessageContent(
                            assistantMsgId,
                            responseContent.toString(),
                            isStreaming = true,
                            tokens = outTokens,
                            cost = 0.0
                        )
                    }
                    // Stream finished
                    val finalOutTokens = Math.max(1, responseContent.length / 4)
                    updateMessageContent(
                        assistantMsgId,
                        responseContent.toString(),
                        isStreaming = false,
                        tokens = finalOutTokens,
                        cost = 0.0
                    )

                    // Save Assistant message to Room database
                    db?.messageDao()?.insertOrUpdate(
                        MessageEntity(
                            id = assistantMsgId,
                            conversationId = conversationId,
                            role = "assistant",
                            content = responseContent.toString(),
                            providerUsed = "local",
                            modelUsed = "gemma-2b-local",
                            time = "now"
                        )
                    )
                } catch (e: Exception) {
                    _errorFlow.value = e.localizedMessage
                    val errorMsg = "Yerel çıkarım hatası: ${e.localizedMessage}"
                    updateMessageContent(assistantMsgId, errorMsg, isStreaming = false)
                }
                return@launch
            }

            try {
                client.preparePost("$backendUrl/api/v1/chat/stream") {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        ChatStreamRequest(
                            conversationId = conversationId,
                            provider = provider,
                            model = model,
                            message = finalPrompt,
                            systemPrompt = if (skillSystemInstruction.isNotBlank()) skillSystemInstruction else null
                        )
                    )
                }.execute { response ->
                    if (response.status != HttpStatusCode.OK) {
                        _errorFlow.value = "Server error: ${response.status.value}"
                        updateMessageContent(assistantMsgId, "Hata oluştu: Sunucu yanıt vermedi.", isStreaming = false)
                        return@execute
                    }

                    val channel = response.bodyAsChannel()
                    val responseContent = StringBuilder()

                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.startsWith("data: ")) {
                            val token = line.removePrefix("data: ").trim()
                            if (token == "[DONE]") {
                                break
                            }
                            responseContent.append(token)
                            val outTokens = Math.max(1, responseContent.length / 4)
                            val outCost = outTokens * outputRate
                            updateMessageContent(
                                assistantMsgId,
                                responseContent.toString(),
                                isStreaming = true,
                                tokens = outTokens,
                                cost = outCost
                            )
                        }
                    }
                    // Stream finished
                    val finalOutTokens = Math.max(1, responseContent.length / 4)
                    val finalOutCost = finalOutTokens * outputRate
                    updateMessageContent(
                        assistantMsgId,
                        responseContent.toString(),
                        isStreaming = false,
                        tokens = finalOutTokens,
                        cost = finalOutCost
                    )

                    // Save Assistant message to Room database
                    db?.messageDao()?.insertOrUpdate(
                        MessageEntity(
                            id = assistantMsgId,
                            conversationId = conversationId,
                            role = "assistant",
                            content = responseContent.toString(),
                            providerUsed = provider,
                            modelUsed = model,
                            time = "now"
                        )
                    )
                }
            } catch (e: Exception) {
                _errorFlow.value = e.localizedMessage
                val errorMsg = "Bağlantı hatası: ${e.localizedMessage}"
                updateMessageContent(
                    assistantMsgId,
                    errorMsg,
                    isStreaming = false
                )
                // Save Error response to Room database
                db?.messageDao()?.insertOrUpdate(
                    MessageEntity(
                        id = assistantMsgId,
                        conversationId = conversationId,
                        role = "assistant",
                        content = errorMsg,
                        providerUsed = provider,
                        modelUsed = model,
                        time = "now"
                    )
                )
            }
        }
    }

    fun uploadFile(
        messageId: String,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String,
        authToken: String,
        onSuccess: (Attachment) -> Unit
    ) {
        _isUploading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response: HttpResponse = client.post("$backendUrl/api/v1/files/upload") {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                    setBody(MultiPartFormDataContent(
                        formData {
                            append("messageId", messageId)
                            append("file", fileBytes, Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            })
                        }
                    ))
                }

                if (response.status == HttpStatusCode.Created) {
                    // Extract success response metadata
                    val bodyText = response.bodyAsText()
                    val json = Json.parseToJsonElement(bodyText)
                    val originalName = json.jsonObject["originalName"]?.toString()?.replace("\"", "") ?: fileName
                    val sizeBytes = json.jsonObject["sizeBytes"]?.toString()?.toLongOrNull() ?: fileBytes.size.toLong()
                    val sizeFormatted = "%.1f MB".format(sizeBytes / (1024.0 * 1024.0))

                    val attachment = Attachment(
                        name = originalName,
                        size = sizeFormatted,
                        type = originalName.substringAfterLast('.', "Unknown").uppercase()
                    )
                    onSuccess(attachment)
                } else {
                    _errorFlow.value = "Upload failed: ${response.status.value}"
                }
            } catch (e: Exception) {
                _errorFlow.value = e.localizedMessage
            } finally {
                _isUploading.value = false
            }
        }
    }

    private fun updateMessageContent(id: String, newContent: String, isStreaming: Boolean, tokens: Int = 0, cost: Double = 0.0) {
        val index = messages.indexOfFirst { it.id == id }
        if (index != -1) {
            val msg = messages[index]
            messages[index] = msg.copy(content = newContent, isStreaming = isStreaming, tokens = tokens, cost = cost)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db?.messageDao()?.deleteMessagesForConversation(id)
            db?.conversationDao()?.deleteConversation(id)
            loadConversations()
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            val messageDao = db?.messageDao() ?: return@launch
            val conversationDao = db?.conversationDao() ?: return@launch
            val allConvs = _conversations.value
            allConvs.forEach { conv ->
                messageDao.deleteMessagesForConversation(conv.id)
                conversationDao.deleteConversation(conv.id)
            }
            messageDao.deleteMessagesForConversation("default-conversation-id")
            conversationDao.deleteConversation("default-conversation-id")
            loadConversations()
            viewModelScope.launch(Dispatchers.Main) {
                messages.clear()
            }
        }
    }

    fun clearRoomCache(conversationId: String = "default-conversation-id") {
        viewModelScope.launch(Dispatchers.IO) {
            val messageDao = db?.messageDao() ?: return@launch
            val conversationDao = db?.conversationDao() ?: return@launch
            messageDao.deleteMessagesForConversation(conversationId)
            conversationDao.deleteConversation(conversationId)
            loadConversations()
            viewModelScope.launch(Dispatchers.Main) {
                messages.clear()
            }
        }
    }

    private val _agentSession = MutableStateFlow<AgentSession?>(null)
    val agentSession = _agentSession.asStateFlow()

    private val _isAgentRunning = MutableStateFlow(false)
    val isAgentRunning = _isAgentRunning.asStateFlow()

    private val _agentArtifacts = MutableStateFlow<List<AgentArtifact>>(emptyList())
    val agentArtifacts = _agentArtifacts.asStateFlow()

    fun loadAgentArtifacts(authToken: String) {
        val session = _agentSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = client.get("$backendUrl/api/v1/agent/session/${session.sessionId}/artifacts") {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                }
                if (response.status == HttpStatusCode.OK) {
                    val list = Json.decodeFromString<List<AgentArtifact>>(response.bodyAsText())
                    _agentArtifacts.value = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startAgentSession(
        authToken: String,
        workspaceId: String,
        hostPath: String,
        prompt: String,
        provider: String,
        modelName: String
    ) {
        _isAgentRunning.value = true
        _errorFlow.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = client.post("$backendUrl/api/v1/agent/session") {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        kotlinx.serialization.json.buildJsonObject {
                            put("workspaceId", workspaceId)
                            put("hostPath", hostPath)
                            put("prompt", prompt)
                            put("provider", provider)
                            put("modelName", modelName)
                        }
                    )
                }
                if (response.status == HttpStatusCode.OK) {
                    val session = Json.decodeFromString<AgentSession>(response.bodyAsText())
                    _agentSession.value = session
                    loadAgentArtifacts(authToken)
                } else {
                    _errorFlow.value = "Başlatma Hatası: ${response.status.value}"
                }
            } catch (e: Exception) {
                _errorFlow.value = "Hata: ${e.localizedMessage}"
            } finally {
                _isAgentRunning.value = false
            }
        }
    }

    fun executeNextAgentStep(authToken: String) {
        val session = _agentSession.value ?: return
        _isAgentRunning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = client.post("$backendUrl/api/v1/agent/session/${session.sessionId}/execute") {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                    contentType(ContentType.Application.Json)
                }
                if (response.status == HttpStatusCode.OK) {
                    val updatedSession = Json.decodeFromString<AgentSession>(response.bodyAsText())
                    _agentSession.value = updatedSession
                    loadAgentArtifacts(authToken)
                } else {
                    _errorFlow.value = "Çalıştırma Hatası: ${response.status.value}"
                }
            } catch (e: Exception) {
                _errorFlow.value = "Hata: ${e.localizedMessage}"
            } finally {
                _isAgentRunning.value = false
            }
        }
    }

    fun clearAgentSession() {
        _agentSession.value = null
        _agentArtifacts.value = emptyList()
    }
}

@Serializable
data class AgentTask(
    val id: String,
    val title: String,
    val command: String,
    var status: String = "pending",
    var stdout: String = "",
    var stderr: String = ""
)

@Serializable
data class AgentSession(
    val sessionId: String,
    val workspaceId: String,
    val originalPrompt: String,
    val tasks: List<AgentTask>,
    var currentTaskIndex: Int = 0,
    var status: String = "planned"
)

@Serializable
data class AgentArtifact(
    val id: String,
    val sessionId: String,
    val type: String,
    val title: String,
    val content: String,
    val createdAt: String
)
