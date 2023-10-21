package chat.revolt.callbacks

import kotlinx.coroutines.flow.MutableSharedFlow

sealed class UiCallback {
    data class ReplyToMessage(val messageId: String) : UiCallback()
    data class EditMessage(val messageId: String) : UiCallback()
}

object UiCallbacks {
    val uiCallbackFlow: MutableSharedFlow<UiCallback> = MutableSharedFlow()

    suspend fun replyToMessage(messageId: String) {
        uiCallbackFlow.emit(UiCallback.ReplyToMessage(messageId))
    }

    suspend fun editMessage(messageId: String) {
        uiCallbackFlow.emit(UiCallback.EditMessage(messageId))
    }
}
