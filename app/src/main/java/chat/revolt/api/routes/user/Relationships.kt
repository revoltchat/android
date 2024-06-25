package chat.revolt.api.routes.user

import chat.revolt.api.RevoltError
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerializationException

suspend fun blockUser(userId: String) {
    val response = RevoltHttp.put("/users/$userId/block")
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun unblockUser(userId: String) {
    val response = RevoltHttp.delete("/users/$userId/block")
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun friendUser(username: String) {
    val response = RevoltHttp.post("/users/friend") {
        contentType(ContentType.Application.Json)
        setBody(mapOf("username" to username))
    }
    val body = response.bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), body)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun acceptFriendRequest(userId: String) {
    val response = RevoltHttp.put("/users/$userId/friend")
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun unfriendUser(userId: String) {
    val response = RevoltHttp.delete("/users/$userId/friend")
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}