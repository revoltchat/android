package chat.revolt.api.internals

import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.Server
import chat.revolt.api.schemas.User

sealed class CategorisedChannelList {
    data class Channel(val channel: chat.revolt.api.schemas.Channel) : CategorisedChannelList()
    data class Category(val category: chat.revolt.api.schemas.Category) : CategorisedChannelList()
}

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
        return channel.recipients?.firstOrNull { u -> u != RevoltAPI.selfId }
    }

    fun categoriseServerFlat(server: Server): List<CategorisedChannelList> {
        val output = mutableListOf<CategorisedChannelList>()

        val uncategorised =
            server.channels?.filter { c ->
                server.categories?.none { cat ->
                    cat.channels?.contains(
                        c
                    ) == true
                } ?: true
            }
                ?.mapNotNull {
                    RevoltAPI.channelCache[it]?.let { it1 ->
                        CategorisedChannelList.Channel(it1)
                    }
                } ?: emptyList()
        output.addAll(uncategorised)

        val categories =
            server.categories?.map { CategorisedChannelList.Category(it) } ?: emptyList()
        categories.forEach {
            output.add(it)
            val channels = it.category.channels?.mapNotNull { c ->
                RevoltAPI.channelCache[c]?.let { it1 ->
                    CategorisedChannelList.Channel(it1)
                }
            } ?: emptyList()
            output.addAll(channels)
        }

        return output
    }
}
