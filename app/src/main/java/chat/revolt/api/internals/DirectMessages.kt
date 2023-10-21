package chat.revolt.api.internals

import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.SpecialUsers.PLATFORM_MODERATION_USER
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.ChannelType

object DirectMessages {
    fun unreadDMs(): List<Channel> {
        return RevoltAPI.channelCache.values
            .filter {
                it.channelType in listOf(
                    ChannelType.DirectMessage, ChannelType.Group
                ) && it.active == true && it.lastMessageID != null
            }
            .filter {
                it.id?.let { id -> RevoltAPI.unreads.hasUnread(id, it.lastMessageID!!) } ?: false
            }
    }

    fun hasPlatformModerationDM(): Boolean {
        return unreadDMs().any {
            it.channelType == ChannelType.DirectMessage &&
                it.recipients?.contains(PLATFORM_MODERATION_USER) ?: false
        }
    }

    fun getPlatformModerationDM(): Channel? {
        return unreadDMs().firstOrNull {
            it.channelType == ChannelType.DirectMessage &&
                it.recipients?.contains(PLATFORM_MODERATION_USER) ?: false
        }
    }
}
