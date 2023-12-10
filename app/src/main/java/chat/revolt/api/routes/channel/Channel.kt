package chat.revolt.api.routes.channel

import chat.revolt.api.RevoltError
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.internals.ULID
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.Message
import chat.revolt.api.schemas.MessagesInChannel
import chat.revolt.api.schemas.User
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer

suspend fun fetchMessagesFromChannel(
    channelId: String,
    limit: Int = 50,
    includeUsers: Boolean = false,
    before: String? = null,
    after: String? = null,
    nearby: String? = null,
    sort: String? = null
): MessagesInChannel {
    val response = RevoltHttp.get("/channels/$channelId/messages") {
        parameter("limit", limit)
        parameter("include_users", includeUsers)

        if (before != null) parameter("before", before)
        if (after != null) parameter("after", after)
        if (nearby != null) parameter("nearby", nearby)
        if (sort != null) parameter("sort", sort)
    }
        .bodyAsText()

    if (includeUsers) {
        return RevoltJson.decodeFromString(
            MessagesInChannel.serializer(),
            response
        )
    } else {
        val messages = RevoltJson.decodeFromString(
            ListSerializer(Message.serializer()),
            response
        )

        return MessagesInChannel(
            messages = messages,
            users = emptyList(),
            members = emptyList()
        )
    }
}

@kotlinx.serialization.Serializable
data class SendMessageReply(
    val id: String,
    val mention: Boolean
)

@kotlinx.serialization.Serializable
data class SendMessageBody(
    val content: String,
    val nonce: String = ULID.makeNext(),
    val replies: List<SendMessageReply> = emptyList(),
    val attachments: List<String>?
)

@kotlinx.serialization.Serializable
data class EditMessageBody(
    val content: String?
)

@kotlinx.serialization.Serializable
data class CreateInviteResponse(
    val type: String,
    @SerialName("_id")
    val id: String,
    val server: String,
    val creator: String,
    val channel: String,
)

suspend

fun sendMessage(
    channelId: String,
    content: String,
    nonce: String? = ULID.makeNext(),
    replies: List<SendMessageReply>? = null,
    attachments: List<String>? = null
): String {
    val response = RevoltHttp.post("/channels/$channelId/messages") {
        contentType(ContentType.Application.Json)
        setBody(
            SendMessageBody(
                content = content,
                nonce = nonce ?: ULID.makeNext(),
                replies = replies ?: emptyList(),
                attachments = attachments
            )
        )
    }
        .bodyAsText()

    return response
}

suspend fun editMessage(channelId: String, messageId: String, newContent: String? = null) {
    val response = RevoltHttp.patch("/channels/$channelId/messages/$messageId") {
        contentType(ContentType.Application.Json)
        setBody(
            EditMessageBody(
                content = newContent
            )
        )
    }
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun ackChannel(channelId: String, messageId: String = ULID.makeNext()) {
    RevoltHttp.put("/channels/$channelId/ack/$messageId")
}

suspend fun fetchSingleChannel(channelId: String): Channel {
    val response = RevoltHttp.get("/channels/$channelId")
        .bodyAsText()

    return RevoltJson.decodeFromString(
        Channel.serializer(),
        response
    )
}

suspend fun fetchGroupParticipants(channelId: String): List<User> {
    val response = RevoltHttp.get("/channels/$channelId/members")
        .bodyAsText()

    return RevoltJson.decodeFromString(
        ListSerializer(User.serializer()),
        response
    )
}

suspend fun createInvite(channelId: String): CreateInviteResponse {
    val response = RevoltHttp.post("/channels/$channelId/invites")
        .bodyAsText()

    val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
    if (error.type != "Server") throw Error(error.type)

    return RevoltJson.decodeFromString(CreateInviteResponse.serializer(), response)
}