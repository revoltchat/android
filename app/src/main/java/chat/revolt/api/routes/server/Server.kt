package chat.revolt.api.routes.server

import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import io.ktor.client.request.*

suspend fun ackServer(serverId: String) {
    RevoltHttp.put("/servers/$serverId/ack") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)
    }
}