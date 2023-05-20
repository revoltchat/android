package chat.revolt.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.revolt.ui.theme.Theme
import chat.revolt.ui.theme.getDefaultTheme

object GlobalState {
    var theme by mutableStateOf(getDefaultTheme())

    fun hydrateWithSettings(settings: SyncedSettings) {
        settings.android.theme?.let { this.theme = Theme.valueOf(it) }
    }

    fun reset() {
        theme = getDefaultTheme()
    }
}