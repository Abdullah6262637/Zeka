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
    val userId: String,
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
        hostPath: String,
        prompt: String,
        userId: UUID,
        apiKey: String,
        provider: LlmProvider,
        modelName: String
    ): AgentSession? {
        val sessionId = UUID.randomUUID().toString()

        val skillsPrompt = PromptSkillLoader.loadSkillsFromWorkspace(hostPath)
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
            $skillsPrompt
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
                userId = userId.toString(),
                originalPrompt = prompt,
                tasks = tasksList,
                currentTaskIndex = 0,
                status = "planned"
            )

            // Save artifacts to DB
            ArtifactManager.saveArtifact(sessionId, "plan", "Görev Planı: " + (if (prompt.length > 25) prompt.take(22) + "..." else prompt), finalJson)
            
            val mockDiff = """
                diff --git a/src/main/kotlin/com/zeka/Application.kt b/src/main/kotlin/com/zeka/Application.kt
                index 4f3a2b..9d8c7e 100644
                --- a/src/main/kotlin/com/zeka/Application.kt
                +++ b/src/main/kotlin/com/zeka/Application.kt
                @@ -25,5 +25,6 @@
                 fun main() {
                -    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
                +    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
                +    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
                         .start(wait = true)
                 }
            """.trimIndent()
            ArtifactManager.saveArtifact(sessionId, "diff", "Kod Değişikliği (Konfigürasyon)", mockDiff)

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

        val userUuid = UUID.fromString(session.userId)
        if (!QuotaManager.checkQuota(userUuid)) {
            session.status = "failed"
            currentTask.status = "failed"
            currentTask.stderr = "HATA: Günlük kullanım kotası (CPU/Token) aşıldı."
            ArtifactManager.saveArtifact(sessionId, "log", "Kota Aşımı: ${currentTask.title}", "Günlük kullanım limitleriniz tükendi.")
            return session
        }

        // Execute command inside sandbox
        val result = DockerSandboxManager.executeCommand(session.workspaceId, currentTask.command)
        currentTask.stdout = result.stdout
        currentTask.stderr = result.stderr

        // Save log artifact to DB
        val logContent = "Komut: ${currentTask.command}\n\nSTDOUT:\n${result.stdout}\n\nSTDERR:\n${result.stderr}"
        ArtifactManager.saveArtifact(sessionId, "log", "Çıktı: ${currentTask.title}", logContent)

        if (result.exitCode == 0) {
            currentTask.status = "completed"
            session.currentTaskIndex++
            
            // Deduct quota & Save memory
            QuotaManager.consumeCpu(userUuid, 5)
            QuotaManager.consumeTokens(userUuid, 150)
            MemoryManager.saveMemory(
                workspaceId = session.workspaceId,
                taskTitle = currentTask.title,
                command = currentTask.command,
                content = "Başarılı komut yürütme. Çıktı boyutu: ${result.stdout.length} karakter."
            )

            // Simüle edilmiş tarayıcı ekran görüntüsü (screenshot) artifact'i ekle
            val mockScreenshotUrl = "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=800&q=80"
            ArtifactManager.saveArtifact(
                sessionId = sessionId,
                type = "screenshot",
                title = "Arayüz Test Ekran Görüntüsü",
                content = mockScreenshotUrl
            )

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
