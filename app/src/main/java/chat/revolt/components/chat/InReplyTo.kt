package chat.revolt.components.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.components.generic.UserAvatar

@Composable
fun InReplyTo(
    messageId: String,
    modifier: Modifier = Modifier,
    withMention: Boolean = false,
    onMessageClick: (String) -> Unit = { _ -> },
) {
    val message = RevoltAPI.messageCache[messageId]
    val author = RevoltAPI.userCache[message?.author ?: ""]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onMessageClick(messageId) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(48.dp))

        if (message != null) {
            UserAvatar(
                username = author?.username ?: "",
                userId = author?.id ?: "",
                avatar = author?.avatar,
                size = 16.dp
            )

            Text(
                text = if (author != null) {
                    if (withMention) {
                        "@${author.username}"
                    } else {
                        author.username
                    }
                } else {
                    stringResource(id = R.string.unknown)
                } ?: stringResource(id = R.string.unknown),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Text(
                text = message.content ?: "",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = stringResource(id = R.string.reply_message_not_cached),
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}