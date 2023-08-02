package chat.revolt.api.realtime

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import chat.revolt.api.REVOLT_WEBSOCKET
import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.realtime.frames.receivable.AnyFrame
import chat.revolt.api.realtime.frames.receivable.BulkFrame
import chat.revolt.api.realtime.frames.receivable.ChannelAckFrame
import chat.revolt.api.realtime.frames.receivable.ChannelStartTypingFrame
import chat.revolt.api.realtime.frames.receivable.ChannelStopTypingFrame
import chat.revolt.api.realtime.frames.receivable.ChannelUpdateFrame
import chat.revolt.api.realtime.frames.receivable.MessageAppendFrame
import chat.revolt.api.realtime.frames.receivable.MessageFrame
import chat.revolt.api.realtime.frames.receivable.MessageUpdateFrame
import chat.revolt.api.realtime.frames.receivable.PongFrame
import chat.revolt.api.realtime.frames.receivable.ReadyFrame
import chat.revolt.api.realtime.frames.receivable.ServerCreateFrame
import chat.revolt.api.realtime.frames.receivable.ServerDeleteFrame
import chat.revolt.api.realtime.frames.receivable.ServerMemberJoinFrame
import chat.revolt.api.realtime.frames.receivable.ServerMemberLeaveFrame
import chat.revolt.api.realtime.frames.receivable.ServerMemberUpdateFrame
import chat.revolt.api.realtime.frames.receivable.ServerUpdateFrame
import chat.revolt.api.realtime.frames.receivable.UserUpdateFrame
import chat.revolt.api.realtime.frames.sendable.AuthorizationFrame
import chat.revolt.api.realtime.frames.sendable.PingFrame
import chat.revolt.api.routes.server.fetchMember
import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.SerializationException

enum class DisconnectionState {
    Disconnected,
    Reconnecting,
    Connected
}

sealed class RealtimeSocketFrames {
    data class Reconnected(val unit: Unit = Unit) : RealtimeSocketFrames()
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
            pushReconnectEvent()

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

    private suspend fun handleFrame(type: String, rawFrame: String) {
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
                val userMap = readyFrame.users.associateBy { it.id!! }
                RevoltAPI.userCache.putAll(userMap)

                Log.d("RealtimeSocket", "Adding servers to cache.")
                val serverMap = readyFrame.servers.associateBy { it.id!! }
                RevoltAPI.serverCache.putAll(serverMap)

                Log.d("RealtimeSocket", "Adding channels to cache.")
                val channelMap = readyFrame.channels.associateBy { it.id!! }
                RevoltAPI.channelCache.putAll(channelMap)

                Log.d("RealtimeSocket", "Adding emojis to cache.")
                val emojiMap = readyFrame.emojis.associateBy { it.id!! }
                RevoltAPI.emojiCache.putAll(emojiMap)
            }

            "Message" -> {
                val messageFrame = RevoltJson.decodeFromString(MessageFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message frame for ${messageFrame.id} in channel ${messageFrame.channel}."
                )

                if (messageFrame.id == null) {
                    Log.d("RealtimeSocket", "Message frame has no ID or channel. Ignoring.")
                    return
                }

                RevoltAPI.messageCache[messageFrame.id] = messageFrame

                messageFrame.channel?.let {
                    if (RevoltAPI.channelCache[it] == null) {
                        Log.d("RealtimeSocket", "Channel $it not found in cache. Ignoring.")
                        return
                    }

                    RevoltAPI.channelCache[it] =
                        RevoltAPI.channelCache[it]!!.copy(lastMessageID = messageFrame.id)

                    RevoltAPI.wsFrameChannel.send(messageFrame)
                }
            }

            "MessageAppend" -> {
                val messageAppendFrame =
                    RevoltJson.decodeFromString(MessageAppendFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message append frame for ${messageAppendFrame.id} in channel ${messageAppendFrame.channel}."
                )

                var message = RevoltAPI.messageCache[messageAppendFrame.id]

                if (message == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Message ${messageAppendFrame.id} not found in cache. Will not append."
                    )
                    return
                }

                messageAppendFrame.append.embeds?.let {
                    message = message!!.copy(embeds = message!!.embeds?.plus(it) ?: it)
                }

                RevoltAPI.messageCache[messageAppendFrame.id] = message!!

                RevoltAPI.wsFrameChannel.send(messageAppendFrame)
            }

            "MessageUpdate" -> {
                val messageUpdateFrame =
                    RevoltJson.decodeFromString(MessageUpdateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message update frame for ${messageUpdateFrame.id} in channel ${messageUpdateFrame.channel}."
                )

                val oldMessage = RevoltAPI.messageCache[messageUpdateFrame.id]
                if (oldMessage == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Message ${messageUpdateFrame.id} not found in cache. Will not update."
                    )
                    return
                }

                val rawMessage: MessageFrame
                try {
                    rawMessage =
                        RevoltJson.decodeFromJsonElement(
                            MessageFrame.serializer(),
                            messageUpdateFrame.data
                        )
                } catch (e: SerializationException) {
                    Log.d("RealtimeSocket", "Message update frame has invalid data. Ignoring.")
                    return
                }

                Log.d(
                    "RealtimeSocket",
                    "Merging message ${messageUpdateFrame.id} with updated partial."
                )

                RevoltAPI.messageCache[messageUpdateFrame.id] =
                    oldMessage.mergeWithPartial(rawMessage)

                messageUpdateFrame.channel.let {
                    if (RevoltAPI.channelCache[it] == null) {
                        Log.d("RealtimeSocket", "Channel $it not found in cache. Ignoring.")
                        return
                    }
                }

                RevoltAPI.wsFrameChannel.send(messageUpdateFrame)
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

            "ServerCreate" -> {
                val serverCreateFrame =
                    RevoltJson.decodeFromString(ServerCreateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server create frame for ${serverCreateFrame.id}, with name ${serverCreateFrame.server.name}. Adding to cache."
                )

                RevoltAPI.serverCache[serverCreateFrame.id] = serverCreateFrame.server

                serverCreateFrame.channels.forEach { channel ->
                    if (channel.id == null) return@forEach
                    RevoltAPI.channelCache[channel.id] = channel
                }
            }

            "ChannelStartTyping" -> {
                val channelStartTypingFrame =
                    RevoltJson.decodeFromString(ChannelStartTypingFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel start typing frame for ${channelStartTypingFrame.id}."
                )

                RevoltAPI.wsFrameChannel.send(channelStartTypingFrame)
            }

            "ChannelStopTyping" -> {
                val channelStopTypingFrame =
                    RevoltJson.decodeFromString(ChannelStopTypingFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel stop typing frame for ${channelStopTypingFrame.id}."
                )

                RevoltAPI.wsFrameChannel.send(channelStopTypingFrame)
            }

            "ServerUpdate" -> {
                val serverUpdateFrame =
                    RevoltJson.decodeFromString(ServerUpdateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server update frame for ${serverUpdateFrame.id}."
                )

                val existing = RevoltAPI.serverCache[serverUpdateFrame.id]
                    ?: return // if we don't have the server no point in updating it

                var updated =
                    existing.mergeWithPartial(serverUpdateFrame.data)

                serverUpdateFrame.clear?.forEach {
                    when (it) {
                        "Icon" -> updated = updated.copy(icon = null)
                        "Banner" -> updated = updated.copy(banner = null)
                        "Description" -> updated = updated.copy(description = null)
                        else -> Log.e("RealtimeSocket", "Unknown server clear field: $it")
                    }
                }

                RevoltAPI.serverCache[serverUpdateFrame.id] = updated
            }

            "ServerDelete" -> {
                val serverDeleteFrame =
                    RevoltJson.decodeFromString(ServerDeleteFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server delete frame for ${serverDeleteFrame.id}."
                )

                RevoltAPI.serverCache.remove(serverDeleteFrame.id)
            }

            "ServerMemberUpdate" -> {
                val serverMemberUpdateFrame =
                    RevoltJson.decodeFromString(ServerMemberUpdateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server member update frame for ${serverMemberUpdateFrame.id.user} in ${serverMemberUpdateFrame.id.server}."
                )

                val existing = RevoltAPI.members.getMember(
                    serverMemberUpdateFrame.id.server,
                    serverMemberUpdateFrame.id.user
                )
                    ?: return // if we don't have the member no point in updating them

                var updated = existing.mergeWithPartial(serverMemberUpdateFrame.data)

                serverMemberUpdateFrame.clear?.forEach {
                    when (it) {
                        "Avatar" -> updated = updated.copy(avatar = null)
                        "Nickname" -> updated = updated.copy(nickname = null)
                        else -> Log.e("RealtimeSocket", "Unknown server member clear field: $it")
                    }
                }

                RevoltAPI.members.setMember(serverMemberUpdateFrame.id.server, updated)
            }

            "ServerMemberJoin" -> {
                val serverMemberJoinFrame =
                    RevoltJson.decodeFromString(ServerMemberJoinFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server member join frame for ${serverMemberJoinFrame.user} in ${serverMemberJoinFrame.id}."
                )

                val member = fetchMember(serverMemberJoinFrame.id, serverMemberJoinFrame.user)

                RevoltAPI.members.setMember(serverMemberJoinFrame.id, member)
            }

            "ServerMemberLeave" -> {
                val serverMemberLeaveFrame =
                    RevoltJson.decodeFromString(ServerMemberLeaveFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server member leave frame for ${serverMemberLeaveFrame.user} in ${serverMemberLeaveFrame.id}."
                )

                RevoltAPI.members.removeMember(
                    serverMemberLeaveFrame.id,
                    serverMemberLeaveFrame.user
                )
            }

            "Authenticated" -> {
                /* no-op */
            }

            else -> {
                Log.i("RealtimeSocket", "Unknown frame: $rawFrame")
            }
        }
    }

    private suspend fun pushReconnectEvent() {
        RevoltAPI.wsFrameChannel.send(RealtimeSocketFrames.Reconnected())
    }
}