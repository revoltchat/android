package chat.revolt.api.routes.sync

import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray

@Serializable
data class SyncedSetting(val timestamp: Long, val value: String)

suspend fun getKeys(vararg keys: String): Map<String, SyncedSetting> {
    val response = RevoltHttp.post("/sync/settings/fetch") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)

        // format: {"keys": ["key1", "key2"]}
        setBody(
            RevoltJson.encodeToString(
                MapSerializer(
                    String.serializer(),
                    ListSerializer(String.serializer())
                ),
                mapOf("keys" to keys.toList())
            )
        )
    }.bodyAsText()

    return RevoltJson.decodeFromString(
        MapSerializer(
            String.serializer(),
            JsonArray.serializer()
        ),
        response
    ).mapValues { (_, value) ->
        SyncedSetting(
            timestamp = value[0].toString().toLong(),
            value = value[1]
                .toString()
                .removeSurrounding("\"")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\") // the revolt API is so scuffed i can't even make this up
        )
    }
}

suspend fun setKey(key: String, value: String) {
    RevoltHttp.post("/sync/settings/set") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)

        parameter("timestamp", System.currentTimeMillis())

        // format: {"key": "value"}
        setBody(
            RevoltJson.encodeToString(
                MapSerializer(
                    String.serializer(),
                    String.serializer()
                ),
                mapOf(key to value)
            )
        )
    }
}