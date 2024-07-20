package chat.revolt.api

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import chat.revolt.BuildConfig
import chat.revolt.api.internals.Members
import chat.revolt.api.realtime.DisconnectionState
import chat.revolt.api.realtime.RealtimeSocket
import chat.revolt.api.routes.user.fetchSelf
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.schemas.Emoji
import chat.revolt.api.schemas.Message
import chat.revolt.api.schemas.Server
import chat.revolt.api.schemas.User
import chat.revolt.api.unreads.Unreads
import chat.revolt.persistence.Database
import chat.revolt.persistence.SqlStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import java.lang.reflect.InvocationTargetException
import java.net.SocketException
import chat.revolt.api.schemas.Channel as ChannelSchema

const val REVOLT_BASE = "https://api.revolt.chat"
const val REVOLT_SUPPORT = "https://support.revolt.chat"
const val REVOLT_MARKETING = "https://revolt.chat"
const val REVOLT_FILES = "https://autumn.revolt.chat"
const val REVOLT_JANUARY = "https://jan.revolt.chat"
const val REVOLT_APP = "https://app.revolt.chat"
const val REVOLT_INVITES = "https://rvlt.gg"
const val REVOLT_WEBSOCKET = "wss://ws.revolt.chat"
const val REVOLT_KJBOOK = "https://revoltchat.github.io/android"

fun buildUserAgent(accessMethod: String = "Ktor"): String {
    return "$accessMethod RevoltAndroid/${BuildConfig.VERSION_NAME} ${BuildConfig.APPLICATION_ID} (Android ${android.os.Build.VERSION.SDK_INT}; ${android.os.Build.MANUFACTURER} ${android.os.Build.DEVICE}; (Kotlin ${KotlinVersion.CURRENT})"
}

private const val BACKEND_IS_STABLE = false

@OptIn(ExperimentalSerializationApi::class)
val RevoltJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

@OptIn(ExperimentalSerializationApi::class)
val RevoltCbor = Cbor {
    ignoreUnknownKeys = true
}

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

    engine {
        addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .apply {
                    if (chain.request().headers[RevoltAPI.TOKEN_HEADER_NAME] == null) {
                        header(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)
                    }
                }
                .build()
            chain.proceed(request)
        }
    }

    defaultRequest {
        url(REVOLT_BASE)
        header("User-Agent", buildUserAgent())
    }
}

val mainHandler = Handler(Looper.getMainLooper())

object RevoltAPI {
    const val TOKEN_HEADER_NAME = "x-session-token"

    val userCache = mutableStateMapOf<String, User>()
    val serverCache = mutableStateMapOf<String, Server>()
    val channelCache = mutableStateMapOf<String, ChannelSchema>()
    val emojiCache = mutableStateMapOf<String, Emoji>()
    val messageCache = mutableStateMapOf<String, Message>()

    val members = Members()

    val unreads = Unreads()

    var selfId: String? = null

    var sessionToken: String = ""
        private set
    var sessionId: String = ""
        private set

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    val realtimeContext = newSingleThreadContext("RealtimeContext")
    val wsFrameChannel = Channel<Any>(Channel.UNLIMITED)

    private var socketCoroutine: Job? = null

    private var openForLocalHydration = true

    fun setSessionHeader(token: String) {
        sessionToken = token
    }

    fun setSessionId(id: String) {
        sessionId = id
    }

    suspend fun loginAs(token: String) {
        setSessionHeader(token)
        fetchSelf()
        startSocketOps()
        unreads.sync()
    }

    suspend fun connectWS() {
        socketCoroutine = CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(realtimeContext) {
                    try {
                        RealtimeSocket.connect(sessionToken)
                    } catch (e: Exception) {
                        try {
                            if (e is SocketException || (e is InvocationTargetException && e.targetException is SocketException)) {
                                Log.d(
                                    "RevoltAPI",
                                    "Socket closed, probably no big deal /// " + e.message
                                )
                            } else {
                                Log.e("RevoltAPI", "WebSocket error", e)
                            }
                            RealtimeSocket.updateDisconnectionState(DisconnectionState.Disconnected)
                        } catch (e: Exception) {
                            Sentry.captureMessage("Error in socket error handling: $e")
                        }
                    }
                }
            } catch (e: Exception) {
                try {
                    if (e is InterruptedException) {
                        Log.d("RevoltAPI", "Socket interrupted")
                    } else {
                        Log.e("RevoltAPI", "WebSocket error", e)
                    }
                    RealtimeSocket.updateDisconnectionState(DisconnectionState.Disconnected)
                } catch (e: Exception) {
                    Sentry.captureMessage("Error in socket error handling: $e")
                }
            }
        }
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
        sessionId = ""

        userCache.clear()
        serverCache.clear()
        channelCache.clear()
        emojiCache.clear()
        messageCache.clear()

        members.clear()
        unreads.clear()

        socketCoroutine?.cancel()
        mainHandler.removeCallbacksAndMessages(null)

        clearPersistentCache()
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

    /**
     * Hydrate caches from a local database.
     */
    fun hydrateFromPersistentCache() {
        if (!openForLocalHydration) {
            Log.w("RevoltAPI", "Hydration is closed, but was called")
            // Stale data is worst case, let's track it even in prod
            Sentry.captureMessage("Local hydration called twice or after real data was fetched")
            return
        }

        val db = Database(SqlStorage.driver)

        val channels = db.channelQueries.selectAll().executeAsList().map {
            ChannelSchema(
                id = it.id,
                channelType = try {
                    ChannelType.valueOf(it.channelType)
                } catch (e: Exception) {
                    null
                },
                user = it.userId,
                name = it.name,
                owner = it.owner,
                description = it.description,
                recipients = selfId?.let { selfId ->
                    it.userId?.let { u -> listOf(u, selfId) }
                } ?: it.userId?.let { u -> listOf(u) },
                icon = AutumnResource(
                    id = it.iconId,
                ),
                server = it.server,
                lastMessageID = it.lastMessageId,
                active = it.active == 1L,
                nsfw = it.nsfw == 1L
            )
        }
        channelCache.clear()
        channelCache.putAll(channels.associateBy { it.id!! })

        val servers = db.serverQueries.selectAll().executeAsList().map {
            Server(
                id = it.id,
                owner = it.owner,
                name = it.name,
                description = it.description,
                icon = AutumnResource(
                    id = it.iconId,
                ),
                banner = AutumnResource(
                    id = it.bannerId,
                ),
                flags = it.flags,
                channels = channels
                    .filter { c -> c.server == it.id }
                    .filterNot { c -> c.id == null }
                    .map { c -> c.id!! },
            )
        }
        serverCache.clear()
        serverCache.putAll(servers.associateBy { it.id!! })

        openForLocalHydration = false
    }

    /**
     * Clear the local caching database.
     */
    private fun clearPersistentCache() {
        val db = Database(SqlStorage.driver)
        db.serverQueries.clear()
        db.channelQueries.clear()
    }

    /**
     * Marks database as hydrated (after real data was fetched, for example).
     */
    fun closeHydration() {
        openForLocalHydration = false
    }
}

@Serializable
data class RevoltError(val type: String)

@Serializable
data class RateLimitResponse(@SerialName("retry_after") val retryAfter: Int) {
    fun toException(): HitRateLimitException {
        return HitRateLimitException(retryAfter)
    }
}

class HitRateLimitException(retryAfter: Int) :
    Exception("Hit rate limit, retry after ${retryAfter}ms")