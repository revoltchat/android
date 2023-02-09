package chat.revolt.callbacks

/**
 * Callbacks for UI events, such as when a user selects "reply" on a message, so that the
 * channel screen can add a reply to the message, for example.
 *
 * We do this by having a singleton object that contains all the receivers, and then
 * the UI can set the callbacks to whatever it wants.
 */
object UiCallbacks {
    interface CallbackReceiver {
        fun onQueueMessageForReply(messageId: String)
    }

    var receivers = mutableListOf<CallbackReceiver>()

    fun registerReceiver(receiver: CallbackReceiver) {
        receivers.add(receiver)
    }

    fun unregisterReceiver(receiver: CallbackReceiver) {
        receivers.remove(receiver)
    }

    fun emitQueueMessageForReply(messageId: String) {
        receivers.forEach { it.onQueueMessageForReply(messageId) }
    }
}