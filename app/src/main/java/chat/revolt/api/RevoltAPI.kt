package chat.revolt.api

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import chat.revolt.api.realtime.RealtimeSocket
import chat.revolt.api.routes.user.fetchSelf
import chat.revolt.api.schemas.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

const val REVOLT_BASE = "https://api.revolt.chat"
const val REVOLT_SUPPORT = "https://support.revolt.chat"
const val REVOLT_MARKETING = "https://revolt.chat"
const val REVOLT_FILES = "https://autumn.revolt.chat"
const val REVOLT_WEBSOCKET = "wss://ws.revolt.chat"

private const val BACKEND_IS_STABLE = false

val RevoltJson = Json { ignoreUnknownKeys = true }

val RevoltHttp = HttpClient(OkHttp) {
    install(DefaultRequest)
    install(ContentNegotiation) {
        json(RevoltJson)
    }

    install(WebSockets)

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

val mainHandler = Handler(Looper.getMainLooper())

object RevoltAPI {
    const val TOKEN_HEADER_NAME = "x-session-token"

    // FIXME discount caching solutions! LRU would be better but this is fine for now
    val userCache = mutableStateMapOf<String, User>()
    val serverCache = mutableStateMapOf<String, Server>()
    val channelCache = mutableStateMapOf<String, Channel>()
    val emojiCache = mutableStateMapOf<String, Emoji>()
    val messageCache = mutableStateMapOf<String, Message>()

    var selfId: String? = null

    var sessionToken: String = ""
        private set

    private var socketThread: Thread? = null

    fun setSessionHeader(token: String) {
        sessionToken = token
    }

    suspend fun loginAs(token: String) {
        setSessionHeader(token)
        fetchSelf()

        startSocketOps()
    }

    suspend fun connectWS() {
        socketThread = Thread {
            try {
                runBlocking {
                    RealtimeSocket.connect(sessionToken)
                }
            } catch (e: Exception) {
                if (e is InterruptedException) {
                    Log.d("RevoltAPI", "Socket interrupted")
                } else {
                    Log.e("RevoltAPI", "WebSocket error", e)
                }
                RealtimeSocket.open = false
            }
        }
        socketThread!!.start()
    }

    private suspend fun startSocketOps() {
        connectWS()

        // Send a ping every roughly 30 seconds else the socket dies
        // Same interval as the web clients (/revolt.js)
        // Note: This will run even if the socket is closed (sendPing will just exit early)
        mainHandler.post(object : Runnable {
            override fun run() {
                runBlocking {
                    RealtimeSocket.sendPing()
                }
                mainHandler.postDelayed(this, 30 * 1000)
            }
        })
    }

    suspend fun initialize() {
        if (sessionToken != "") {
            fetchSelf()
        }
    }

    /**
     * Returns true if the user is logged in and the current user has been fetched at least once.
     * Call [initialize] to fetch the current user first, else this will return false.
     */
    fun isLoggedIn(): Boolean {
        return selfId != null
    }

    /**
     * Clears the API client's state completely.
     */
    fun logout() {
        selfId = null
        sessionToken = ""

        userCache.clear()
        serverCache.clear()
        channelCache.clear()
        emojiCache.clear()
        messageCache.clear()

        socketThread?.interrupt()
    }

    /**
     * Checks if a session token is valid.
     */
    suspend fun checkSessionToken(token: String): Boolean {
        return try {
            setSessionHeader(token)
            fetchSelf()
            true
        } catch (e: Exception) {
            false
        }
    }
}

@Serializable
data class RevoltError(val type: String)