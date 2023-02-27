package chat.revolt.api.routes.sync

import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.schemas.ChannelUnreadResponse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.builtins.ListSerializer

suspend fun syncUnreads(): List<ChannelUnreadResponse> {
    val response = RevoltHttp.get("/sync/unreads") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)
    }
        .bodyAsText()

    return RevoltJson.decodeFromString(
        ListSerializer(ChannelUnreadResponse.serializer()),
        response
    )
}