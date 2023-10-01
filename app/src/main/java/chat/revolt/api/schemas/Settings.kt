package chat.revolt.api.schemas

import kotlinx.serialization.Serializable

@Serializable
data class OrderingSettings(
    val servers: List<String> = emptyList(),
)

@Serializable
data class AndroidSpecificSettings(
    /**
     * The theme to use for the app.
     * Can be one of `{ None, Revolt, Light, M3Dynamic, Amoled }`
     */
    var theme: String? = null,
)