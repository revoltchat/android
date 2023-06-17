package chat.revolt.components.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.solidColor
import chat.revolt.api.routes.microservices.january.asJanuaryProxyUrl
import chat.revolt.api.schemas.User
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

    val username = message?.let { authorName(it) }
        ?: author?.let { User.resolveDefaultName(it) }
        ?: stringResource(id = R.string.unknown)

    val contentColor = LocalContentColor.current
    val usernameColor = message?.let { authorColour(it) } ?: Brush.solidColor(contentColor)

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
                    style = LocalTextStyle.current.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        brush = usernameColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                InlineBadges(
                    bot = message.masquerade == null && author?.bot != null,
                    bridge = message.masquerade != null && author?.bot != null,
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