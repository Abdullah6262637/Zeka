package com.zeka.sandbox

import com.zeka.data.db.tables.AgentMemory
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class MemoryResponse(
    val id: String,
    val workspaceId: String,
    val taskTitle: String,
    val successfulCommand: String,
    val content: String,
    val createdAt: String
)

object MemoryManager {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun saveMemory(
        workspaceId: String,
        taskTitle: String,
        command: String,
        content: String
    ): String {
        return transaction {
            val memId = AgentMemory.insert {
                it[AgentMemory.workspaceId] = workspaceId
                it[AgentMemory.taskTitle] = taskTitle
                it[AgentMemory.successfulCommand] = command
                it[AgentMemory.content] = content
                it[AgentMemory.createdAt] = LocalDateTime.now()
            } get AgentMemory.id
            memId.value.toString()
        }
    }

    fun getMemoriesForWorkspace(workspaceId: String): List<MemoryResponse> {
        return transaction {
            AgentMemory.selectAll().where { AgentMemory.workspaceId eq workspaceId }
                .sortedBy { it[AgentMemory.createdAt] }
                .map {
                    MemoryResponse(
                        id = it[AgentMemory.id].value.toString(),
                        workspaceId = it[AgentMemory.workspaceId],
                        taskTitle = it[AgentMemory.taskTitle],
                        successfulCommand = it[AgentMemory.successfulCommand],
                        content = it[AgentMemory.content],
                        createdAt = it[AgentMemory.createdAt].format(formatter)
                    )
                }
        }
    }
}
