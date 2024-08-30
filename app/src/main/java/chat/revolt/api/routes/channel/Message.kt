package chat.revolt.api.routes.channel

import chat.revolt.api.RevoltHttp
import chat.revolt.api.api
import io.ktor.client.request.delete
import io.ktor.client.request.put

suspend fun react(channelId: String, messageId: String, emoji: String) {
    RevoltHttp.put("/channels/$channelId/messages/$messageId/reactions/$emoji".api())
}

suspend fun unreact(channelId: String, messageId: String, emoji: String) {
    RevoltHttp.delete("/channels/$channelId/messages/$messageId/reactions/$emoji".api())
}