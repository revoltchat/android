package chat.revolt.api.routes.push

import chat.revolt.api.RevoltHttp
import chat.revolt.api.routes.account.WebPushData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun subscribePush(
    endpoint: String = "fcm",
    auth: String,
    p256diffieHellman: String? = null,
) {
    val data = WebPushData(
        endpoint = endpoint,
        p256diffieHellman = p256diffieHellman ?: "",
        auth = auth
    )

    RevoltHttp.post("/push/subscribe") {
        setBody(data)
        contentType(ContentType.Application.Json)
    }
}