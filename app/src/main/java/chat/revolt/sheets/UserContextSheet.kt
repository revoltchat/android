package chat.revolt.sheets

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import chat.revolt.api.RevoltAPI
import chat.revolt.components.screens.settings.UserOverview

@Composable
fun UserContextSheet(
    userId: String,
    onHideSheet: suspend () -> Unit,
) {
    val user = RevoltAPI.userCache[userId]

    if (user == null) {
        // TODO fetch user in this scenario
        Text(text = "not in user cache, but for now there's always this message")
        return
    }

    UserOverview(user)
}