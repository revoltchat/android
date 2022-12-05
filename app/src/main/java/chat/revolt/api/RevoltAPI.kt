package chat.revolt.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import chat.revolt.api.routes.user.fetchSelf
import chat.revolt.api.schemas.CompleteUser
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

const val REVOLT_BASE = "https://api.revolt.chat"
const val REVOLT_SUPPORT = "https://support.revolt.chat"
const val REVOLT_MARKETING = "https://revolt.chat"
const val REVOLT_FILES = "https://autumn.revolt.chat"

private const val BACKEND_IS_STABLE = false

val Context.revoltKVStorage: DataStore<Preferences> by preferencesDataStore(name = "revolt_kv")

val RevoltJson = Json { ignoreUnknownKeys = true }

val RevoltHttp = HttpClient(OkHttp) {
    install(DefaultRequest)
    install(ContentNegotiation) {
        json(RevoltJson)
    }

    if (BACKEND_IS_STABLE) {
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
            retryOnException(maxRetries = 5)

            modifyRequest { request ->
                request.headers.append("x-retry-count", retryCount.toString())
            }

            exponentialDelay()
        }
    }

    install(Logging) { level = LogLevel.INFO }

    defaultRequest {
        url(REVOLT_BASE)
    }
}

object RevoltAPI {
    const val TOKEN_HEADER_NAME = "x-session-token"

    val userCache = mutableMapOf<String, CompleteUser>()

    var selfId: String? = null

    var sessionToken: String = ""
        private set

    fun setSessionHeader(token: String) {
        sessionToken = token
    }

    suspend fun initialize() {
        if (sessionToken != "") {
            fetchSelf()
        }
    }

    fun isLoggedIn(): Boolean {
        return selfId != null
    }
}

@kotlinx.serialization.Serializable
data class RevoltError(val type: String)