package chat.revolt.api.unreads

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ULID
import chat.revolt.api.routes.channel.ackChannel
import chat.revolt.api.routes.server.ackServer
import chat.revolt.api.routes.sync.syncUnreads
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.schemas.ChannelUnread

class Unreads {
    private val hasLoaded = mutableStateOf(false)
    private val channels = mutableStateMapOf<String, ChannelUnread>()

    suspend fun sync() {
        channels.clear()
        channels.putAll(
            try {
                syncUnreads().associate {
                    it.id.channel to ChannelUnread(
                        id = it.id.channel,
                        last_id = it.last_id,
                        mentions = it.mentions
                    )
                }
            } catch (e: Exception) {
                Log.e("Unreads", "Failed to sync unreads", e)
                emptyMap()
            }
        )
        hasLoaded.value = true
    }

    fun getForChannel(channelId: String): ChannelUnread? {
        if (!hasLoaded.value) return null
        return channels[channelId]
    }

    fun hasUnread(channelId: String, lastMessageId: String): Boolean {
        if (!hasLoaded.value) return false
        return (channels[channelId]?.last_id?.compareTo(lastMessageId) ?: 0) < 0
    }

    fun serverHasUnread(serverId: String): Boolean {
        if (!hasLoaded.value) return false

        return RevoltAPI.serverCache[serverId]?.channels?.any {
            val channel = RevoltAPI.channelCache[it] ?: return@any false
            if (channel.channelType == ChannelType.VoiceChannel) return@any false // TODO remove this when text in voice channels is implemented
            hasUnread(it, channel.lastMessageID ?: "")
        }
            ?: false
    }

    suspend fun markAsRead(channelId: String, messageId: String, sync: Boolean = true) {
        if (!hasLoaded.value) return
        channels[channelId]?.let {
            channels[channelId] = it.copy(last_id = messageId)
        }
        if (sync) {
            ackChannel(channelId, messageId)
        }
    }

    fun processExternalAck(channelId: String, messageId: String) {
        channels[channelId]?.let {
            channels[channelId] = it.copy(last_id = messageId)
        }
    }

    suspend fun markServerAsRead(serverId: String, sync: Boolean = true) {
        if (!hasLoaded.value) return

        val server = RevoltAPI.serverCache[serverId] ?: return
        server.channels?.forEach { channel ->
            channels[channel] = channels[channel]?.copy(last_id = ULID.makeNext()) ?: ChannelUnread(
                channel,
                ULID.makeNext()
            )
        }

        if (sync) {
            ackServer(serverId)
        }
    }

    fun clear() {
        channels.clear()
        hasLoaded.value = false
    }
}
