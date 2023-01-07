package chat.revolt.api.routes.microservices.autumn

import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.schemas.AutumnError
import chat.revolt.api.schemas.AutumnId
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File

const val MAX_ATTACHMENTS_PER_MESSAGE = 5

data class FileArgs(
    val file: File,
    val filename: String,
    val contentType: String,
)

suspend fun uploadToAutumn(
    file: File,
    name: String,
    tag: String,
    contentType: ContentType,
    onProgress: (Long, Long) -> Unit = { _, _ -> }
): String {
    val uploadUrl = "$REVOLT_FILES/$tag"

    val response = RevoltHttp.post(uploadUrl) {
        setBody(MultiPartFormDataContent(
            formData {
                append("file", file.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, contentType.toString())
                    append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                })
            }
        ))
        onUpload { bytesSentTotal, contentLength ->
            onProgress(bytesSentTotal, contentLength)
        }
    }

    try {
        val autumnId = RevoltJson.decodeFromString(AutumnId.serializer(), response.bodyAsText())
        return autumnId.id
    } catch (e: Exception) {
        try {
            val error = RevoltJson.decodeFromString(AutumnError.serializer(), response.bodyAsText())
            throw Exception(error.type)
        } catch (e: Exception) {
            throw Exception("Unknown error")
        }
    }

}