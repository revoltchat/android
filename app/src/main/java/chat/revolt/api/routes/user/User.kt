package chat.revolt.api.routes.user

import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.schemas.CompleteUser
import io.ktor.client.request.*
import io.ktor.client.statement.*

suspend fun fetchSelf(): CompleteUser {
    val response = RevoltHttp.get("/users/@me") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)
    }
        .bodyAsText()

    val user = RevoltJson.decodeFromString(CompleteUser.serializer(), response)

    RevoltAPI.userCache[user.id!!] = user
    RevoltAPI.selfId = user.id

    return user
}

suspend fun fetchSelfWithNewToken(token: String): CompleteUser {
    RevoltAPI.setSessionHeader(token)
    return fetchSelf()
}