package com.zeka.sandbox

import com.zeka.data.db.tables.Artifacts
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class ArtifactResponse(
    val id: String,
    val sessionId: String,
    val type: String,
    val title: String,
    val content: String,
    val createdAt: String
)

object ArtifactManager {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun saveArtifact(
        sessionId: String,
        type: String,
        title: String,
        content: String
    ): String {
        return transaction {
            val artId = Artifacts.insert {
                it[Artifacts.sessionId] = sessionId
                it[Artifacts.type] = type
                it[Artifacts.title] = title
                it[Artifacts.content] = content
                it[Artifacts.createdAt] = LocalDateTime.now()
            } get Artifacts.id
            artId.value.toString()
        }
    }

    fun getArtifactsForSession(sessionId: String): List<ArtifactResponse> {
        return transaction {
            Artifacts.selectAll().where { Artifacts.sessionId eq sessionId }
                .sortedBy { it[Artifacts.createdAt] }
                .map {
                    ArtifactResponse(
                        id = it[Artifacts.id].value.toString(),
                        sessionId = it[Artifacts.sessionId],
                        type = it[Artifacts.type],
                        title = it[Artifacts.title],
                        content = it[Artifacts.content],
                        createdAt = it[Artifacts.createdAt].format(formatter)
                    )
                }
        }
    }
}
