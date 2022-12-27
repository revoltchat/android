package chat.revolt.components.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.revolt.api.REVOLT_BASE
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.components.generic.RemoteImage
import chat.revolt.api.schemas.Message as MessageSchema

@Composable
fun Message(
    message: MessageSchema
) {
    val author = RevoltAPI.userCache[message.author] ?: return CircularProgressIndicator()

    Row() {
        if (author.avatar != null) {
            RemoteImage(
                url = "$REVOLT_FILES/avatars/${author.avatar.id!!}/user.png",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                description = "Avatar for ${author.username}"
            )
        } else {
            RemoteImage(
                url = "$REVOLT_BASE/users/${author.id}/default_avatar",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                description = "Avatar for ${author.username}"
            )
        }

        Column(modifier = Modifier.padding(start = 10.dp)) {
            author.username?.let {
                Text(
                    text = it,
                    fontWeight = FontWeight.Bold
                )
            }
            message.content?.let {
                Text(
                    text = it
                )
            }
        }
    }
}