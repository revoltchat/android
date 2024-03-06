package chat.revolt.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.revolt.ui.theme.Theme
import chat.revolt.ui.theme.getDefaultTheme

enum class MessageReplyStyle {
    None,
    SwipeFromEnd,
    DoubleTap
}

object GlobalState {
    var theme by mutableStateOf(getDefaultTheme())
    var messageReplyStyle by mutableStateOf(MessageReplyStyle.SwipeFromEnd)

    fun hydrateWithSettings(settings: SyncedSettings) {
        this.theme = settings.android.theme?.let { Theme.valueOf(it) } ?: getDefaultTheme()
        this.messageReplyStyle =
            settings.android.messageReplyStyle?.let { MessageReplyStyle.valueOf(it) }
                ?: MessageReplyStyle.SwipeFromEnd
    }

    fun reset() {
        theme = getDefaultTheme()
        messageReplyStyle = MessageReplyStyle.SwipeFromEnd
    }
}
