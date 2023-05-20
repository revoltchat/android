package chat.revolt.api.settings

import androidx.compose.runtime.mutableStateOf
import chat.revolt.ui.theme.Theme
import chat.revolt.ui.theme.getDefaultTheme

object GlobalState {
    private var _theme = mutableStateOf(getDefaultTheme())
    val theme
        get() = _theme.value

    fun setTheme(theme: Theme) {
        _theme.value = theme
    }
}