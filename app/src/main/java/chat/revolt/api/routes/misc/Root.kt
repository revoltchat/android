package chat.revolt.api.routes.misc

import chat.revolt.api.RevoltHttp
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Root(
    val revolt: String,
    val features: Features,
    val ws: String,
    val app: String,
    val vapid: String
)

@Serializable
data class Features(
    val captcha: CAPTCHA,
    val email: Boolean,

    @SerialName("invite_only")
    val inviteOnly: Boolean,

    val autumn: Autumn,
    val january: Autumn,
    val voso: Voso
)

@Serializable
data class Autumn(
    val enabled: Boolean,
    val url: String
)

@Serializable
data class CAPTCHA(
    val enabled: Boolean,
    val key: String
)

@Serializable
data class Voso(
    val enabled: Boolean,
    val url: String,
    val ws: String
)

suspend fun getRootRoute(): Root {
    return RevoltHttp.get("/").body()
}