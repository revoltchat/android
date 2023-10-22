package chat.revolt.api.routes.auth

import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.schemas.Session
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.builtins.ListSerializer

suspend fun fetchAllSessions(): List<Session> {
    val response = RevoltHttp.get("/auth/session/all")
        .bodyAsText()

    return RevoltJson.decodeFromString(
        ListSerializer(Session.serializer()),
        response
    )
}

suspend fun logoutSessionById(id: String) {
    RevoltHttp.delete("/auth/session/$id")
}

suspend fun logoutAllSessions(includingSelf: Boolean = false) {
    RevoltHttp.delete("/auth/session/all") {
        parameter("revoke_self", includingSelf)
    }
}