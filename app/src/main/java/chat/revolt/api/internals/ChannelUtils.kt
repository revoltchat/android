package chat.revolt.api.internals

import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.User

object ChannelUtils {
    fun resolveDMName(channel: Channel): String? {
        return channel.name
            ?: RevoltAPI.userCache[channel.recipients?.first { u -> u != RevoltAPI.selfId }]?.let {
                User.resolveDefaultName(
                    it
                )
            }
    }

    fun resolveDMPartner(channel: Channel): String? {
        return channel.recipients?.first { u -> u != RevoltAPI.selfId }
    }
}