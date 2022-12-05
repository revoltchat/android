package chat.revolt.screens.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.components.generic.RemoteImage

@Composable
fun HomeScreen(navController: NavController) {
    val user = RevoltAPI.userCache[RevoltAPI.selfId]

    Column() {
        Text(text = "Logged in as " + user?.username + "!")
        RemoteImage(
            url = "$REVOLT_FILES/avatars/${user?.avatar?.id}/user.png",
            description = "User Avatar",
            modifier = Modifier
                .clip(CircleShape)
                .width(70.dp)
                .height(70.dp)
        )
    }
}