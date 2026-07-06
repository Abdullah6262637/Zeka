package com.zeka.routes

import com.zeka.llm.LlmProvider
import com.zeka.llm.ProviderRouter
import com.zeka.sandbox.AgentLoopManager
import com.zeka.sandbox.DockerSandboxManager
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AgentSessionRequest(
    val workspaceId: String,
    val hostPath: String,
    val prompt: String,
    val provider: String,
    val modelName: String
)

fun Route.agentRoutes() {
    authenticate("auth-jwt") {
        route("/api/v1/agent") {
            post("/session") {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("userId")?.asString())
                val req = call.receive<AgentSessionRequest>()

                val apiKey = ProviderRouter.getDecryptedKey(userId, req.provider.lowercase())
                if (apiKey == null && req.provider.lowercase() != "ollama") {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "API key not found for provider ${req.provider}"))
                    return@post
                }

                val sandboxStarted = DockerSandboxManager.createSandbox(req.workspaceId, req.hostPath)
                if (!sandboxStarted) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to initialize Docker sandbox container"))
                    return@post
                }

                val llmProvider = try {
                    LlmProvider.valueOf(req.provider.uppercase())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid provider ${req.provider}"))
                    return@post
                }

                val session = AgentLoopManager.createSession(
                    workspaceId = req.workspaceId,
                    hostPath = req.hostPath,
                    prompt = req.prompt,
                    userId = userId,
                    apiKey = apiKey ?: "",
                    provider = llmProvider,
                    modelName = req.modelName
                )

                if (session == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create agent session (planner error)"))
                    return@post
                }

                call.respond(session)
            }

            get("/session/{sessionId}") {
                val sessionId = call.parameters["sessionId"]
                if (sessionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Session ID is required"))
                    return@get
                }

                val session = AgentLoopManager.getSession(sessionId)
                if (session == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                    return@get
                }

                call.respond(session)
            }

            get("/session/{sessionId}/artifacts") {
                val sessionId = call.parameters["sessionId"]
                if (sessionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Session ID is required"))
                    return@get
                }

                val artifacts = com.zeka.sandbox.ArtifactManager.getArtifactsForSession(sessionId)
                call.respond(artifacts)
            }

            post("/session/{sessionId}/execute") {
                val sessionId = call.parameters["sessionId"]
                if (sessionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Session ID is required"))
                    return@post
                }

                val session = AgentLoopManager.getSession(sessionId)
                if (session == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                    return@post
                }

                com.zeka.sandbox.AgentQueueProcessor.enqueueSessionStep(sessionId)
                call.respond(mapOf("status" to "enqueued"))
            }

            delete("/session/{sessionId}") {
                val sessionId = call.parameters["sessionId"]
                if (sessionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Session ID is required"))
                    return@delete
                }

                val session = AgentLoopManager.getSession(sessionId)
                if (session == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                    return@delete
                }

                DockerSandboxManager.stopSandbox(session.workspaceId)
                call.respond(mapOf("status" to "stopped"))
            }

            post("/plugins/install") {
                val multipart = call.receiveMultipart()
                var workspacePath = ""
                var fileBytes: ByteArray? = null
                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "workspacePath") {
                                workspacePath = part.value
                            }
                        }
                        is PartData.FileItem -> {
                            fileBytes = part.streamProvider().readBytes()
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                if (workspacePath.isEmpty() || fileBytes == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "workspacePath and file are required"))
                    return@post
                }

                val plugin = com.zeka.sandbox.PluginManager.installPlugin(fileBytes!!.inputStream(), workspacePath)
                if (plugin == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to install plugin"))
                } else {
                    call.respond(plugin)
                }
            }

            get("/plugins") {
                call.respond(com.zeka.sandbox.PluginManager.listPlugins())
            }

            post("/plugins/{pluginId}/toggle") {
                val pluginId = call.parameters["pluginId"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Plugin ID is required"))
                val active = call.receive<Map<String, Boolean>>()["active"] ?: true
                val success = com.zeka.sandbox.PluginManager.togglePlugin(pluginId, active)
                if (success) {
                    call.respond(mapOf("status" to "success"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Plugin not found"))
                }
            }
        }
    }
}
