package com.zeka.routes

import com.zeka.data.db.tables.Personas
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
data class PersonaRequest(
    val name: String,
    val systemPrompt: String,
    val icon: String,
    val temperature: Double = 0.7,
    val topP: Double = 0.9,
    val maxTokens: Int = 2048
)

@Serializable
data class PersonaResponse(
    val id: String,
    val userId: String?,
    val name: String,
    val systemPrompt: String,
    val icon: String,
    val temperature: Double,
    val topP: Double,
    val maxTokens: Int
)

fun Route.personaRoutes() {
    authenticate("auth-jwt") {
        route("/api/v1/personas") {
            // Get all visible personas (global + user's custom)
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("userId")?.asString())

                val personasList = transaction {
                    Personas.select { 
                        (Personas.userId.isNull()) or (Personas.userId eq userId) 
                    }.map {
                        PersonaResponse(
                            id = it[Personas.id].value.toString(),
                            userId = it[Personas.userId]?.toString(),
                            name = it[Personas.name],
                            systemPrompt = it[Personas.systemPrompt],
                            icon = it[Personas.icon],
                            temperature = it[Personas.temperature],
                            topP = it[Personas.topP],
                            maxTokens = it[Personas.maxTokens]
                        )
                    }
                }
                call.respond(personasList)
            }

            // Create custom persona
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("userId")?.asString())
                val req = call.receive<PersonaRequest>()

                val newId = transaction {
                    Personas.insert {
                        it[Personas.userId] = userId
                        it[name] = req.name
                        it[systemPrompt] = req.systemPrompt
                        it[icon] = req.icon
                        it[temperature] = req.temperature
                        it[topP] = req.topP
                        it[maxTokens] = req.maxTokens
                    } get Personas.id
                }

                call.respond(HttpStatusCode.Created, mapOf("id" to newId.value.toString()))
            }

            // Update custom persona
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("userId")?.asString())
                val personaId = UUID.fromString(call.parameters["id"])
                val req = call.receive<PersonaRequest>()

                val updatedCount = transaction {
                    Personas.update({ (Personas.id eq personaId) and (Personas.userId eq userId) }) {
                        it[name] = req.name
                        it[systemPrompt] = req.systemPrompt
                        it[icon] = req.icon
                        it[temperature] = req.temperature
                        it[topP] = req.topP
                        it[maxTokens] = req.maxTokens
                    }
                }

                if (updatedCount == 0) {
                    call.respond(HttpStatusCode.NotFound, "Persona not found or access denied")
                } else {
                    call.respond(HttpStatusCode.OK, "Updated successfully")
                }
            }

            // Delete custom persona
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = UUID.fromString(principal?.payload?.getClaim("userId")?.asString())
                val personaId = UUID.fromString(call.parameters["id"])

                val deletedCount = transaction {
                    Personas.deleteWhere { (id eq personaId) and (Personas.userId eq userId) }
                }

                if (deletedCount == 0) {
                    call.respond(HttpStatusCode.NotFound, "Persona not found or access denied")
                } else {
                    call.respond(HttpStatusCode.OK, "Deleted successfully")
                }
            }
        }
    }
}
