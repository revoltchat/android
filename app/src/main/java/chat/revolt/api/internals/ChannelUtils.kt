package chat.revolt.api.internals

import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.Channel

object ChannelUtils {
    fun resolveDMName(channel: Channel): String? {
        return channel.name
            ?: RevoltAPI.userCache[channel.recipients?.first { u -> u != RevoltAPI.selfId }]?.username
    }

    fun resolveDMPartner(channel: Channel): String? {
        return channel.recipients?.first { u -> u != RevoltAPI.selfId }
    }
}