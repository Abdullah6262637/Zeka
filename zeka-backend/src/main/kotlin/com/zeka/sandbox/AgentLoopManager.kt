package com.zeka.sandbox

import com.zeka.llm.LlmProvider
import com.zeka.llm.ProviderRouter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class AgentTask(
    val id: String,
    val title: String,
    val command: String,
    var status: String = "pending", // pending, running, completed, failed
    var stdout: String = "",
    var stderr: String = ""
)

@Serializable
data class AgentSession(
    val sessionId: String,
    val workspaceId: String,
    val originalPrompt: String,
    val tasks: MutableList<AgentTask>,
    var currentTaskIndex: Int = 0,
    var status: String = "planned" // planned, running, completed, failed
)

object AgentLoopManager {

    private val sessions = ConcurrentHashMap<String, AgentSession>()

    fun getSession(sessionId: String): AgentSession? = sessions[sessionId]

    suspend fun createSession(
        workspaceId: String,
        prompt: String,
        userId: UUID,
        apiKey: String,
        provider: LlmProvider,
        modelName: String
    ): AgentSession? {
        val sessionId = UUID.randomUUID().toString()

        val systemPrompt = """
            Sen Zeka Antigravity otonom kodlama asistanısın. Kullanıcının verdiği göreve göre, Docker sandbox terminalinde sırasıyla çalıştırılacak komutları içeren adım adım bir plan oluşturmalısın.
            Çıktın SADECE şu JSON formatında olmalıdır (başka hiçbir metin veya açıklama ekleme):
            {
              "tasks": [
                {
                  "title": "Görevin kısa adı",
                  "command": "terminalde çalışacak shell komutu"
                }
              ]
            }
        """.trimIndent()

        try {
            val responseText = ProviderRouter.executeChat(
                provider = provider,
                apiKey = apiKey,
                baseUrl = null,
                modelName = modelName,
                systemPrompt = systemPrompt,
                prompt = prompt
            )

            // Extract JSON block if surrounded by markdown code block
            val cleanJson = responseText.substringAfter("```json").substringBefore("```").trim()
            val finalJson = if (cleanJson.startsWith("{")) cleanJson else responseText.trim()

            val jsonElement = Json.parseToJsonElement(finalJson).jsonObject
            val tasksArray = jsonElement["tasks"]?.jsonArray ?: return null

            val tasksList = tasksArray.mapIndexed { index, element ->
                val obj = element.jsonObject
                AgentTask(
                    id = "task-${System.currentTimeMillis()}-$index",
                    title = obj["title"]?.jsonPrimitive?.content ?: "Adım $index",
                    command = obj["command"]?.jsonPrimitive?.content ?: "echo"
                )
            }.toMutableList()

            val session = AgentSession(
                sessionId = sessionId,
                workspaceId = workspaceId,
                originalPrompt = prompt,
                tasks = tasksList,
                currentTaskIndex = 0,
                status = "planned"
            )

            sessions[sessionId] = session
            return session
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun executeNextStep(sessionId: String): AgentSession? {
        val session = sessions[sessionId] ?: return null
        if (session.currentTaskIndex >= session.tasks.size) {
            session.status = "completed"
            return session
        }

        session.status = "running"
        val currentTask = session.tasks[session.currentTaskIndex]
        currentTask.status = "running"

        // Execute command inside sandbox
        val result = DockerSandboxManager.executeCommand(session.workspaceId, currentTask.command)
        currentTask.stdout = result.stdout
        currentTask.stderr = result.stderr

        if (result.exitCode == 0) {
            currentTask.status = "completed"
            session.currentTaskIndex++
            if (session.currentTaskIndex >= session.tasks.size) {
                session.status = "completed"
            } else {
                session.status = "planned" // Paused, awaiting next step approval
            }
        } else {
            currentTask.status = "failed"
            session.status = "failed"
        }

        return session
    }
}
