package chat.revolt.api.realtime

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
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

enum class DisconnectionState {
    Disconnected,
    Reconnecting,
    Connected
}

object RealtimeSocket {
    var socket: WebSocketSession? = null

    private var _disconnectionState = mutableStateOf(DisconnectionState.Reconnecting)
    val disconnectionState: DisconnectionState
        get() = _disconnectionState.value

    fun updateDisconnectionState(state: DisconnectionState) {
        _disconnectionState.value = state
    }

    suspend fun connect(token: String) {
        if (disconnectionState == DisconnectionState.Connected) {
            Log.d("RealtimeSocket", "Already connected to websocket. Refusing to connect again.")
            return
        }

        socket?.close(CloseReason(CloseReason.Codes.NORMAL, "Reconnecting to websocket."))

        RevoltHttp.ws(REVOLT_WEBSOCKET) {
            socket = this

            Log.d("RealtimeSocket", "Connected to websocket.")
            updateDisconnectionState(DisconnectionState.Connected)
            invalidateAllChannelStates()

            // Send authorization frame
            val authFrame = AuthorizationFrame("Authenticate", token)
            val authFrameString =
                RevoltJson.encodeToString(AuthorizationFrame.serializer(), authFrame)

            Log.d(
                "RealtimeSocket",
                "Sending authorization frame: ${
                    authFrameString.replace(
                        token,
                        "X".repeat(token.length)
                    )
                }"
            )
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
        if (disconnectionState != DisconnectionState.Connected) return

        val pingPacket = PingFrame("Ping", System.currentTimeMillis())
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
            "Message" -> {
                val messageFrame = RevoltJson.decodeFromString(MessageFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message frame for ${messageFrame.id} in channel ${messageFrame.channel}."
                )

                RevoltAPI.messageCache[messageFrame.id!!] = messageFrame

                // Update last message ID for channel - important for unreads
                messageFrame.channel?.let {
                    RevoltAPI.channelCache[it] =
                        RevoltAPI.channelCache[it]!!.copy(lastMessageID = messageFrame.id)
                }

                channelCallbacks[messageFrame.channel]?.onMessage(messageFrame)
            }
            "ChannelStartTyping" -> {
                val typingFrame =
                    RevoltJson.decodeFromString(ChannelStartTypingFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel start typing frame for ${typingFrame.id} from ${typingFrame.user}."
                )

                channelCallbacks[typingFrame.id]?.onStartTyping(typingFrame)
            }
            "ChannelStopTyping" -> {
                val typingFrame =
                    RevoltJson.decodeFromString(ChannelStopTypingFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel stop typing frame for ${typingFrame.id} from ${typingFrame.user}."
                )

                channelCallbacks[typingFrame.id]?.onStopTyping(typingFrame)
            }
            "UserUpdate" -> {
                val userUpdateFrame =
                    RevoltJson.decodeFromString(UserUpdateFrame.serializer(), rawFrame)

                val existing = RevoltAPI.userCache[userUpdateFrame.id]
                    ?: return // if we don't have the user no point in updating it

                RevoltAPI.userCache[userUpdateFrame.id] =
                    existing.mergeWithPartial(userUpdateFrame.data)
            }
            "ChannelUpdate" -> {
                val channelUpdateFrame =
                    RevoltJson.decodeFromString(ChannelUpdateFrame.serializer(), rawFrame)

                val existing = RevoltAPI.channelCache[channelUpdateFrame.id]
                    ?: return // if we don't have the channel no point in updating it

                RevoltAPI.channelCache[channelUpdateFrame.id] =
                    existing.mergeWithPartial(channelUpdateFrame.data)
            }
            "ChannelAck" -> {
                val channelAckFrame =
                    RevoltJson.decodeFromString(ChannelAckFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel ack frame for ${channelAckFrame.id} with new newest ${channelAckFrame.messageId}."
                )

                RevoltAPI.unreads.processExternalAck(channelAckFrame.id, channelAckFrame.messageId)
            }
            "Authenticated" -> {
                // No effect
            }
            else -> {
                Log.i("RealtimeSocket", "Unknown frame: $rawFrame")
            }
        }
    }

    private fun invalidateAllChannelStates() {
        channelCallbacks.forEach { (_, callback) ->
            callback.onStateInvalidate()
        }
    }

    interface ChannelCallback {
        fun onStartTyping(typing: ChannelStartTypingFrame)
        fun onStopTyping(typing: ChannelStopTypingFrame)
        fun onMessage(message: MessageFrame)
        fun onStateInvalidate()
    }

    private val channelCallbacks: SnapshotStateMap<String, ChannelCallback> = mutableStateMapOf()

    fun registerChannelCallback(channelId: String, callback: ChannelCallback) {
        channelCallbacks[channelId] = callback

        Log.d("RealtimeSocket", "Registered channel callback for $channelId.")
    }

    fun unregisterChannelCallback(channelId: String) {
        channelCallbacks.remove(channelId)

        Log.d("RealtimeSocket", "Unregistered channel callback for $channelId")
    }
}