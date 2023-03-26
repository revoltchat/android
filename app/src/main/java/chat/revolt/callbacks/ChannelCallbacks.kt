package chat.revolt.callbacks

object ChannelCallbacks {
    interface CallbackReceiver {
        fun onReconnect()
        fun onStartTyping(channelId: String, userId: String)
        fun onStopTyping(channelId: String, userId: String)
        fun onMessage(messageId: String)
        fun onMessageUpdate(messageId: String)
        fun onMessageDelete(messageId: String)
        fun onMessageBulkDelete(messageIds: List<String>)
        fun onMessageReactionAdd(messageId: String, emoji: String, userId: String)
        fun onMessageReactionRemove(messageId: String, emoji: String, userId: String)
        fun onMessageReactionRemoveAll(messageId: String)
    }

    var receivers = mutableMapOf<String, CallbackReceiver>()

    fun registerReceiver(channelId: String, receiver: CallbackReceiver) {
        receivers[channelId] = receiver
    }

    fun unregisterReceiver(channelId: String) {
        receivers.remove(channelId)
    }

    fun emitReconnect() {
        receivers.forEach { it.value.onReconnect() }
    }

    fun emitStartTyping(channelId: String, userId: String) {
        receivers[channelId]?.onStartTyping(channelId, userId)
    }

    fun emitStopTyping(channelId: String, userId: String) {
        receivers[channelId]?.onStopTyping(channelId, userId)
    }

    fun emitMessage(channelId: String, messageId: String) {
        receivers[channelId]?.onMessage(messageId)
    }

    fun emitMessageUpdate(channelId: String, messageId: String) {
        receivers[channelId]?.onMessageUpdate(messageId)
    }

    fun emitMessageDelete(channelId: String, messageId: String) {
        receivers[channelId]?.onMessageDelete(messageId)
    }

    fun emitMessageBulkDelete(channelId: String, messageIds: List<String>) {
        receivers[channelId]?.onMessageBulkDelete(messageIds)
    }

    fun emitMessageReactionAdd(
        channelId: String,
        messageId: String,
        emoji: String,
        userId: String
    ) {
        receivers[channelId]?.onMessageReactionAdd(messageId, emoji, userId)
    }

    fun emitMessageReactionRemove(
        channelId: String,
        messageId: String,
        emoji: String,
        userId: String
    ) {
        receivers[channelId]?.onMessageReactionRemove(messageId, emoji, userId)
    }

    fun emitMessageReactionRemoveAll(channelId: String, messageId: String) {
        receivers[channelId]?.onMessageReactionRemoveAll(messageId)
    }
}