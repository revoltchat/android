package chat.revolt.api.routes.microservices.health

import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.schemas.HealthNotice
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

suspend fun healthCheck(): HealthNotice {
    val response = RevoltHttp.get("https://health.revolt.chat/api/health").bodyAsText()
    return RevoltJson.decodeFromString(HealthNotice.serializer(), response)
}