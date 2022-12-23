package chat.revolt.api.realtime

import android.util.Log
import chat.revolt.api.REVOLT_WEBSOCKET
import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.realtime.frames.receivable.*
import chat.revolt.api.realtime.frames.sendable.AuthorizationFrame
import chat.revolt.api.realtime.frames.sendable.PingFrame
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import java.util.Calendar

object RealtimeSocket {
    var socket: WebSocketSession? = null
    var open: Boolean = false

    suspend fun connect(token: String) {
        RevoltHttp.ws(REVOLT_WEBSOCKET) {
            socket = this

            Log.d("RealtimeSocket", "Connected to websocket.")
            open = true

            // Send authorization frame
            val authFrame = AuthorizationFrame("Authenticate", token)
            val authFrameString =
                RevoltJson.encodeToString(AuthorizationFrame.serializer(), authFrame)

            Log.d("RealtimeSocket", "Sending authorization frame: $authFrameString")
            send(RevoltJson.encodeToString(AuthorizationFrame.serializer(), authFrame))

            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val frameString = frame.readText()
                    val frameType =
                        RevoltJson.decodeFromString(AnyFrame.serializer(), frameString).type

                    handleFrame(frameType, frameString)
                }
            }
        }
    }

    suspend fun sendPing() {
        if (!open) return

        val pingPacket = PingFrame("Ping", Calendar.getInstance().timeInMillis.toInt())
        socket?.send(RevoltJson.encodeToString(PingFrame.serializer(), pingPacket))
        Log.d("RealtimeSocket", "Sent ping frame with ${pingPacket.data}")
    }

    private fun handleFrame(type: String, rawFrame: String) {
        when (type) {
            "Pong" -> {
                val pongFrame = RevoltJson.decodeFromString(PongFrame.serializer(), rawFrame)
                Log.d("RealtimeSocket", "Received pong frame for ${pongFrame.data}")
            }
            "Bulk" -> {
                val bulkFrame = RevoltJson.decodeFromString(BulkFrame.serializer(), rawFrame)
                Log.d("RealtimeSocket", "Received bulk frame with ${bulkFrame.v.size} sub-frames.")
                bulkFrame.v.forEach { subFrame ->
                    val subFrameType =
                        RevoltJson.decodeFromString(AnyFrame.serializer(), subFrame.toString()).type
                    handleFrame(subFrameType, subFrame.toString())
                }
            }
            "Ready" -> {
                val readyFrame = RevoltJson.decodeFromString(ReadyFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received ready frame with ${readyFrame.users.size} users, ${readyFrame.servers.size} servers, ${readyFrame.channels.size} channels, and ${readyFrame.emojis.size} emojis."
                )

                Log.d("RealtimeSocket", "Adding users to cache.")
                readyFrame.users.forEach { user ->
                    RevoltAPI.userCache[user.id!!] = user
                }

                Log.d("RealtimeSocket", "Adding servers to cache.")
                readyFrame.servers.forEach { server ->
                    RevoltAPI.serverCache[server.id!!] = server
                }

                Log.d("RealtimeSocket", "Adding channels to cache.")
                readyFrame.channels.forEach { channel ->
                    RevoltAPI.channelCache[channel.id!!] = channel
                }

                Log.d("RealtimeSocket", "Adding emojis to cache.")
                readyFrame.emojis.forEach { emoji ->
                    RevoltAPI.emojiCache[emoji.id!!] = emoji
                }
            }
            "UserUpdate" -> {
                val userUpdateFrame =
                    RevoltJson.decodeFromString(UserUpdateFrame.serializer(), rawFrame)
                // We will genuinely just ignore this frame for now, but it gets really spammy in the logs
                // FIXME handle this frame
            }
            else -> {
                Log.i("RealtimeSocket", "Unknown frame: $rawFrame")
            }
        }
    }
}