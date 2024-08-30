package chat.revolt.api.routes.safety

import chat.revolt.api.RevoltError
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.schemas.ContentReportReason
import chat.revolt.api.schemas.FullMessageReport
import chat.revolt.api.schemas.FullServerReport
import chat.revolt.api.schemas.FullUserReport
import chat.revolt.api.schemas.MessageReport
import chat.revolt.api.schemas.ServerReport
import chat.revolt.api.schemas.UserReport
import chat.revolt.api.schemas.UserReportReason
import chat.revolt.api.api
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException

suspend fun putMessageReport(
    messageId: String,
    reason: ContentReportReason,
    additionalContext: String? = null
) {
    val fullMessageReport = FullMessageReport(
        content = MessageReport(
            type = "Message",
            report_reason = reason,
            id = messageId
        ),
        additional_context = additionalContext
    )

    val response = RevoltHttp.post("/safety/report".api()) {
        setBody(
            RevoltJson.encodeToString(
                FullMessageReport.serializer(),
                fullMessageReport
            )
        )
    }
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun putServerReport(
    serverId: String,
    reason: ContentReportReason,
    additionalContext: String? = null
) {
    val fullServerReport = FullServerReport(
        content = ServerReport(
            type = "Server",
            report_reason = reason,
            id = serverId
        ),
        additional_context = additionalContext
    )

    val response = RevoltHttp.post("/safety/report".api()) {
        setBody(
            RevoltJson.encodeToString(
                FullServerReport.serializer(),
                fullServerReport
            )
        )
    }
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun putUserReport(
    userId: String,
    reason: UserReportReason,
    additionalContext: String? = null
) {
    val fullUserReport = FullUserReport(
        content = UserReport(
            type = "User",
            report_reason = reason,
            id = userId
        ),
        additional_context = additionalContext
    )

    val response = RevoltHttp.post("/safety/report".api()) {
        setBody(
            RevoltJson.encodeToString(
                FullUserReport.serializer(),
                fullUserReport
            )
        )
    }
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}
