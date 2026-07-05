package com.zeka.llm

import com.zeka.config.SecurityConfig
import com.zeka.data.db.tables.ProviderKeys
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

enum class LlmProvider {
    OPENAI, ANTHROPIC, GOOGLE, DEEPSEEK, OPENROUTER, OLLAMA
}

object ProviderRouter {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    fun getDecryptedKey(userId: UUID, providerName: String): String? {
        return transaction {
            ProviderKeys.select {
                (ProviderKeys.userId eq userId) and 
                (ProviderKeys.provider eq providerName) and 
                (ProviderKeys.isActive eq true)
            }.firstOrNull()?.let {
                val encrypted = it[ProviderKeys.encryptedApiKey]
                SecurityConfig.decrypt(encrypted)
            }
        }
    }

    /**
     * Executes a streaming chat request to the corresponding model provider.
     * Returns a Flow of token chunks.
     */
    fun streamChat(
        provider: LlmProvider,
        apiKey: String,
        baseUrl: String?,
        modelName: String,
        systemPrompt: String?,
        prompt: String,
        temperature: Double = 0.7
    ): Flow<String> = flow {
        when (provider) {
            LlmProvider.OPENAI -> {
                val url = baseUrl ?: "https://api.openai.com/v1/chat/completions"
                val body = buildJsonObject {
                    put("model", modelName)
                    put("temperature", temperature)
                    put("stream", true)
                    put("messages", buildJsonArray {
                        if (systemPrompt != null) {
                            addJsonObject {
                                put("role", "system")
                                put("content", systemPrompt)
                            }
                        }
                        addJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        }
                    })
                }

                client.preparePost(url) {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.execute { response ->
                    if (response.status != HttpStatusCode.OK) {
                        emit("Error: ${response.status.value} - ${response.bodyAsText()}")
                        return@execute
                    }
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") break
                            try {
                                val json = Json.parseToJsonElement(data).jsonObject
                                val content = json["choices"]?.jsonArray?.firstOrNull()
                                    ?.jsonObject?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.content
                                if (content != null) {
                                    emit(content)
                                }
                            } catch (e: Exception) {
                                // Skip parsing errors on incomplete chunks
                            }
                        }
                    }
                }
            }

            LlmProvider.ANTHROPIC -> {
                val url = baseUrl ?: "https://api.anthropic.com/v1/messages"
                val body = buildJsonObject {
                    put("model", modelName)
                    put("max_tokens", 4096)
                    put("temperature", temperature)
                    if (systemPrompt != null) {
                        put("system", systemPrompt)
                    }
                    put("messages", buildJsonArray {
                        addJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        }
                    })
                    put("stream", true)
                }

                client.preparePost(url) {
                    header("x-api-key", apiKey)
                    header("anthropic-version", "2023-06-01")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.execute { response ->
                    if (response.status != HttpStatusCode.OK) {
                        emit("Error: ${response.status.value} - ${response.bodyAsText()}")
                        return@execute
                    }
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            try {
                                val json = Json.parseToJsonElement(data).jsonObject
                                val type = json["type"]?.jsonPrimitive?.content
                                if (type == "content_block_delta") {
                                    val text = json["delta"]?.jsonObject?.get("text")?.jsonPrimitive?.content
                                    if (text != null) {
                                        emit(text)
                                    }
                                }
                            } catch (e: Exception) {
                                // Skip parsing errors on intermediate/incomplete lines
                            }
                        }
                    }
                }
            }

            LlmProvider.GOOGLE -> {
                val url = baseUrl ?: "https://generativelanguage.googleapis.com/v1beta/models/$modelName:streamGenerateContent?key=$apiKey"
                val body = buildJsonObject {
                    put("contents", buildJsonArray {
                        addJsonObject {
                            put("parts", buildJsonArray {
                                addJsonObject {
                                    put("text", prompt)
                                }
                            })
                        }
                    })
                    if (systemPrompt != null) {
                        put("systemInstruction", buildJsonObject {
                            put("parts", buildJsonArray {
                                addJsonObject {
                                    put("text", systemPrompt)
                                }
                            })
                        })
                    }
                    put("generationConfig", buildJsonObject {
                        put("temperature", temperature)
                    })
                }

                client.preparePost(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.execute { response ->
                    if (response.status != HttpStatusCode.OK) {
                        emit("Error: ${response.status.value} - ${response.bodyAsText()}")
                        return@execute
                    }
                    val channel = response.bodyAsChannel()
                    val responseText = StringBuilder()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        // Google Gemini stream yields SSE or JSON chunks. Standard SSE starts with data:
                        if (line.startsWith("data: ") || line.trim().startsWith("{")) {
                            val rawData = if (line.startsWith("data: ")) line.removePrefix("data: ").trim() else line.trim()
                            try {
                                val json = Json.parseToJsonElement(rawData).jsonObject
                                val candidates = json["candidates"]?.jsonArray
                                val content = candidates?.firstOrNull()?.jsonObject?.get("content")?.jsonObject
                                val parts = content?.get("parts")?.jsonArray
                                val text = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                                if (text != null) {
                                    emit(text)
                                }
                            } catch (e: Exception) {
                                // Accumulate and try parsing full response in case it's a batch JSON array
                            }
                        }
                    }
                }
            }

            LlmProvider.DEEPSEEK -> {
                val url = baseUrl ?: "https://api.deepseek.com/v1/chat/completions"
                val body = buildJsonObject {
                    put("model", modelName)
                    put("temperature", temperature)
                    put("stream", true)
                    put("messages", buildJsonArray {
                        if (systemPrompt != null) {
                            addJsonObject {
                                put("role", "system")
                                put("content", systemPrompt)
                            }
                        }
                        addJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        }
                    })
                }

                client.preparePost(url) {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.execute { response ->
                    if (response.status != HttpStatusCode.OK) {
                        emit("Error: ${response.status.value} - ${response.bodyAsText()}")
                        return@execute
                    }
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") break
                            try {
                                val json = Json.parseToJsonElement(data).jsonObject
                                val content = json["choices"]?.jsonArray?.firstOrNull()
                                    ?.jsonObject?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.content
                                if (content != null) {
                                    emit(content)
                                }
                            } catch (e: Exception) { }
                        }
                    }
                }
            }

            LlmProvider.OPENROUTER -> {
                val url = baseUrl ?: "https://openrouter.ai/api/v1/chat/completions"
                val body = buildJsonObject {
                    put("model", modelName)
                    put("temperature", temperature)
                    put("stream", true)
                    put("messages", buildJsonArray {
                        if (systemPrompt != null) {
                            addJsonObject {
                                put("role", "system")
                                put("content", systemPrompt)
                            }
                        }
                        addJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        }
                    })
                }

                client.preparePost(url) {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    header("HTTP-Referer", "https://zeka.ai")
                    header("X-Title", "Zeka App")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.execute { response ->
                    if (response.status != HttpStatusCode.OK) {
                        emit("Error: ${response.status.value} - ${response.bodyAsText()}")
                        return@execute
                    }
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") break
                            try {
                                val json = Json.parseToJsonElement(data).jsonObject
                                val content = json["choices"]?.jsonArray?.firstOrNull()
                                    ?.jsonObject?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.content
                                if (content != null) {
                                    emit(content)
                                }
                            } catch (e: Exception) { }
                        }
                    }
                }
            }

            LlmProvider.OLLAMA -> {
                val url = baseUrl ?: "http://localhost:11434/api/chat"
                val body = buildJsonObject {
                    put("model", modelName)
                    put("stream", true)
                    put("options", buildJsonObject {
                        put("temperature", temperature)
                    })
                    put("messages", buildJsonArray {
                        if (systemPrompt != null) {
                            addJsonObject {
                                put("role", "system")
                                put("content", systemPrompt)
                            }
                        }
                        addJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        }
                    })
                }

                client.preparePost(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.execute { response ->
                    if (response.status != HttpStatusCode.OK) {
                        emit("Error: ${response.status.value} - ${response.bodyAsText()}")
                        return@execute
                    }
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        try {
                            val json = Json.parseToJsonElement(line).jsonObject
                            val content = json["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
                            if (content != null) {
                                emit(content)
                            }
                        } catch (e: Exception) { }
                    }
                }
            }
        }
    }

    suspend fun executeChat(
        provider: LlmProvider,
        apiKey: String,
        baseUrl: String?,
        modelName: String,
        systemPrompt: String?,
        prompt: String,
        temperature: Double = 0.7
    ): String {
        val sb = StringBuilder()
        streamChat(provider, apiKey, baseUrl, modelName, systemPrompt, prompt, temperature).collect {
            sb.append(it)
        }
        return sb.toString()
    }
}
