package chat.revolt.api.routes.custom

import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.api
import chat.revolt.api.schemas.Emoji
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

suspend fun fetchEmoji(id: String): Emoji {
    val response = RevoltHttp.get("/custom/emoji/$id".api()).bodyAsText()
    return RevoltJson.decodeFromString(
        Emoji.serializer(),
        response
    )
}
