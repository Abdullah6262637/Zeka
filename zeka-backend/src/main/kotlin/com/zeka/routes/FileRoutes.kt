package com.zeka.routes

import com.zeka.data.db.tables.Attachments
import com.zeka.data.db.tables.Messages
import com.zeka.data.storage.MinioFileStorage
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.ByteArrayInputStream
import java.util.UUID

@Serializable
data class AttachmentResponse(
    val id: String,
    val fileType: String,
    val originalName: String,
    val sizeBytes: Long,
    val storageKey: String,
    val extractedText: String?
)

fun Route.fileRoutes() {
    authenticate("auth-jwt") {
        route("/api/v1/files") {
            post("/upload") {
                val multipart = call.receiveMultipart()
                var fileBytes: ByteArray? = null
                var fileName = ""
                var contentType = ""
                var messageIdStr: String? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            fileName = part.originalFileName ?: "file"
                            contentType = part.contentType?.toString() ?: "application/octet-stream"
                            fileBytes = part.streamProvider().readBytes()
                        }
                        is PartData.FormItem -> {
                            if (part.name == "messageId") {
                                messageIdStr = part.value
                            }
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                val bytes = fileBytes
                if (bytes == null) {
                    call.respond(HttpStatusCode.BadRequest, "No file uploaded")
                    return@post
                }

                val msgId = messageIdStr?.let { UUID.fromString(it) }
                if (msgId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing messageId associated with the file")
                    return@post
                }

                // Upload to MinIO
                val storageKey = "${UUID.randomUUID()}_$fileName"
                val inputStream = ByteArrayInputStream(bytes)
                
                try {
                    MinioFileStorage.uploadFile(
                        objectName = storageKey,
                        inputStream = inputStream,
                        size = bytes.size.toLong(),
                        contentType = contentType
                    )
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to upload to storage: ${e.localizedMessage}")
                    return@post
                }

                // Basic text extraction for text/pdf
                val fileExtension = fileName.substringAfterLast('.', "").lowercase()
                val extractedText = when (fileExtension) {
                    "txt", "csv", "json" -> String(bytes, Charsets.UTF_8).take(20000) // limit character count
                    "pdf" -> {
                        // Simple PDF text metadata reader mockup (since full PDF parser is added in Phase 3)
                        "[Metin Tabanlı PDF İçeriği: $fileName - Kuantum fiziği ile klasik fizik arasındaki temel farklar...] (Simüle Edilmiştir)"
                    }
                    else -> null // image/binary
                }

                val attachmentId = transaction {
                    Attachments.insert {
                        it[messageId] = msgId
                        it[fileType] = fileExtension
                        it[Attachments.storageKey] = storageKey
                        it[originalName] = fileName
                        it[sizeBytes] = bytes.size.toLong()
                        it[Attachments.extractedText] = extractedText
                    } get Attachments.id
                }

                call.respond(
                    HttpStatusCode.Created,
                    AttachmentResponse(
                        id = attachmentId.value.toString(),
                        fileType = fileExtension,
                        originalName = fileName,
                        sizeBytes = bytes.size.toLong(),
                        storageKey = storageKey,
                        extractedText = extractedText
                    )
                )
            }
        }
    }
}
