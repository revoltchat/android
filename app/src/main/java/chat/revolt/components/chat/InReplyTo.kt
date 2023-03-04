package chat.revolt.components.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.asJanuaryProxyUrl
import chat.revolt.api.internals.WebCompat
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

    val username = message?.masquerade?.name ?: author?.username ?: ""

    val contentColor = LocalContentColor.current
    val usernameColor = message?.masquerade?.colour?.let {
        WebCompat.parseColour(it)
    } ?: contentColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onMessageClick(messageId) }
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(40.dp))

            if (message != null) {
                UserAvatar(
                    username = username,
                    userId = author?.id ?: "",
                    avatar = author?.avatar,
                    rawUrl = message.masquerade?.avatar?.let { asJanuaryProxyUrl(it) },
                    size = 16.dp
                )

                Text(
                    text = if (author != null) {
                        if (withMention) {
                            "@$username"
                        } else {
                            username
                        }
                    } else {
                        stringResource(id = R.string.unknown)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = usernameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                InlineBadges(
                    bot = message.masquerade == null && author?.bot != null,
                    masquerade = message.masquerade != null && author?.bot != null,
                    colour = contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(8.dp),
                    followingIfAny = {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                )

                Text(
                    text = message.content ?: "",
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = stringResource(id = R.string.reply_message_not_cached),
                    fontStyle = FontStyle.Italic, // inter doesn't have italics...
                    color = contentColor.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Default, // ...so we use the defaul t font
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}