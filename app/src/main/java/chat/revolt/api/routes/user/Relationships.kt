package chat.revolt.api.routes.user

import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltError
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import io.ktor.client.request.delete
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException
import kotlin.collections.set

suspend fun blockUser(userId: String) {
    val response = RevoltHttp.put("/users/$userId/block")
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val user = RevoltAPI.userCache[userId] ?: return
    RevoltAPI.userCache[userId] = user.copy(relationship = "Blocked")
}

suspend fun unblockUser(userId: String) {
    val response = RevoltHttp.delete("/users/$userId/block")
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val user = RevoltAPI.userCache[userId] ?: return
    RevoltAPI.userCache[userId] = user.copy(relationship = "None")
}

suspend fun unfriendUser(userId: String) {
    val response = RevoltHttp.delete("/users/$userId/friend")
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val user = RevoltAPI.userCache[userId] ?: return
    RevoltAPI.userCache[userId] = user.copy(relationship = "None")
}