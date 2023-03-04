package chat.revolt.components.chat

import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.asJanuaryProxyUrl
import chat.revolt.api.internals.ULID
import chat.revolt.api.internals.WebCompat
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.UserAvatarWidthPlaceholder
import chat.revolt.markdown.Markdown
import chat.revolt.api.schemas.Message as MessageSchema

fun viewAttachmentInBrowser(ctx: android.content.Context, attachment: AutumnResource) {
    val customTab = CustomTabsIntent
        .Builder()
        .build()
    customTab.launchUrl(
        ctx,
        Uri.parse("$REVOLT_FILES/attachments/${attachment.id}/${attachment.filename}")
    )
}


fun formatLongAsTime(time: Long): String {
    val date = java.util.Date(time)
    val format =
        java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault())

    return format.format(date)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Message(
    message: MessageSchema,
    truncate: Boolean = false,
    onMessageContextMenu: () -> Unit = {},
) {
    val author = RevoltAPI.userCache[message.author] ?: return CircularProgressIndicator()
    val context = LocalContext.current

    Column {
        if (message.tail == false) {
            Spacer(modifier = Modifier.height(10.dp))
        }

        message.replies?.forEach { reply ->
            val replyMessage = RevoltAPI.messageCache[reply]

            InReplyTo(
                messageId = reply,
                withMention = replyMessage?.author?.let { message.mentions?.contains(replyMessage.author) }
                    ?: false,
            ) {
                // TODO Add jump to message
                if (replyMessage == null) {
                    Toast.makeText(context, "lmao prankd", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        onMessageContextMenu()
                    }
                )
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
        ) {
            if (message.tail == false) {
                UserAvatar(
                    username = author.username ?: "",
                    userId = author.id!!,
                    avatar = author.avatar,
                    rawUrl = message.masquerade?.avatar?.let { asJanuaryProxyUrl(it) }
                )
            } else {
                UserAvatarWidthPlaceholder()
            }

            Column(modifier = Modifier.padding(start = 10.dp)) {
                if (message.tail == false) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.masquerade?.name ?: author.username ?: "",
                            fontWeight = FontWeight.Bold,
                            color = if (message.masquerade?.colour != null) {
                                WebCompat.parseColour(message.masquerade.colour)
                            } else LocalContentColor.current,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        InlineBadges(
                            bot = author.bot != null && message.masquerade == null,
                            masquerade = message.masquerade != null && author.bot != null,
                            colour = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp),
                            precedingIfAny = {
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        Text(
                            text = formatLongAsTime(ULID.asTimestamp(message.id!!)),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                message.content?.let {
                    if (message.content.isBlank()) return@let // if only an attachment is sent

                    Text(
                        text = Markdown.annotate(it),
                        maxLines = if (truncate) 1 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                message.attachments?.let {
                    message.attachments.forEach { attachment ->
                        Spacer(modifier = Modifier.height(5.dp))
                        MessageAttachment(attachment) {
                            viewAttachmentInBrowser(context, attachment)
                        }
                    }
                }
            }
        }
    }
}