package chat.revolt.callbacks

import kotlinx.coroutines.channels.Channel

sealed class Action {
    data class OpenUserSheet(val userId: String, val serverId: String?) : Action()
}

val ActionChannel = Channel<Action>(
    Channel.BUFFERED
)