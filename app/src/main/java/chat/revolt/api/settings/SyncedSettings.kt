package chat.revolt.api.settings

import androidx.compose.runtime.mutableStateOf
import chat.revolt.api.RevoltJson
import chat.revolt.api.routes.sync.getKeys
import chat.revolt.api.routes.sync.setKey
import chat.revolt.api.schemas.AndroidSpecificSettings
import chat.revolt.api.schemas.OrderingSettings

object SyncedSettings {
    private val _ordering = mutableStateOf(OrderingSettings())
    private val _android = mutableStateOf(AndroidSpecificSettings("None"))

    val ordering: OrderingSettings
        get() = _ordering.value
    val android: AndroidSpecificSettings
        get() = _android.value

    suspend fun fetch() {
        try {
            val settings = getKeys("ordering", "android")

            settings["ordering"]?.let {
                _ordering.value = RevoltJson.decodeFromString(
                    OrderingSettings.serializer(),
                    it.value
                )
            }

            settings["android"]?.let {
                _android.value = RevoltJson.decodeFromString(
                    AndroidSpecificSettings.serializer(),
                    it.value
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateOrdering(value: OrderingSettings) {
        _ordering.value = value
        setKey("ordering", RevoltJson.encodeToString(OrderingSettings.serializer(), value))
    }

    suspend fun updateAndroid(value: AndroidSpecificSettings) {
        _android.value = value
        setKey("android", RevoltJson.encodeToString(AndroidSpecificSettings.serializer(), value))
    }
}