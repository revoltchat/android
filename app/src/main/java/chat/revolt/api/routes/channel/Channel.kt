package chat.revolt.api.routes.channel

import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.internals.ULID
import chat.revolt.api.schemas.Message
import chat.revolt.api.schemas.MessagesInChannel
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.builtins.ListSerializer

suspend fun fetchMessagesFromChannel(
    channelId: String,
    limit: Int = 50,
    include_users: Boolean = false,
    before: String? = null,
    after: String? = null,
    nearby: String? = null,
    sort: String? = null
): MessagesInChannel {
    val response = RevoltHttp.get("/channels/$channelId/messages") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)

        parameter("limit", limit)
        parameter("include_users", include_users)

        if (before != null) parameter("before", before)
        if (after != null) parameter("after", after)
        if (nearby != null) parameter("nearby", nearby)
        if (sort != null) parameter("sort", sort)
    }
        .bodyAsText()

    if (include_users) {
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
    val attachments: List<String>?,
)

suspend fun sendMessage(
    channelId: String,
    content: String,
    nonce: String? = ULID.makeNext(),
    replies: List<SendMessageReply>? = null,
    attachments: List<String>? = null,
): String {
    val response = RevoltHttp.post("/channels/$channelId/messages") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)

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