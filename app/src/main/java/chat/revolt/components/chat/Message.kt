package chat.revolt.components.chat

import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.asJanuaryProxyUrl
import chat.revolt.api.internals.ULID
import chat.revolt.api.internals.WebCompat
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.UserAvatarWidthPlaceholder
import chat.revolt.api.schemas.Message as MessageSchema

fun viewUrlInBrowser(ctx: android.content.Context, url: String) {
    val customTab = CustomTabsIntent
        .Builder()
        .build()
    customTab.launchUrl(ctx, Uri.parse(url))
}

fun viewAttachmentInBrowser(ctx: android.content.Context, attachment: AutumnResource) {
    val url = REVOLT_FILES + attachment.id + "/" + attachment.filename
    viewUrlInBrowser(ctx, url)
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
    parse: (MessageSchema) -> SpannableStringBuilder = { SpannableStringBuilder(it.content) },
    onMessageContextMenu: () -> Unit = {},
) {
    val author = RevoltAPI.userCache[message.author] ?: return CircularProgressIndicator()
    val context = LocalContext.current
    val contentColor = LocalContentColor.current

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
                    userId = author.id ?: message.id ?: ULID.makeSpecial(0),
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
                            text = message.masquerade?.name
                                ?: author.username
                                ?: stringResource(id = R.string.unknown),
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

                    AndroidView(factory = { ctx ->
                        androidx.appcompat.widget.AppCompatTextView(ctx).apply {
                            text = parse(message)
                            maxLines = if (truncate) 1 else Int.MAX_VALUE
                            ellipsize = TextUtils.TruncateAt.END
                            textSize = 16f
                            typeface = ResourcesCompat.getFont(ctx, R.font.inter)

                            setTextColor(contentColor.toArgb())
                        }
                    })
                }

                message.attachments?.let {
                    message.attachments.forEach { attachment ->
                        Spacer(modifier = Modifier.height(2.dp))
                        MessageAttachment(attachment) {
                            viewAttachmentInBrowser(context, attachment)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                message.embeds?.let {
                    message.embeds.forEach { embed ->
                        val embedIsEmpty =
                            embed.title == null && embed.description == null && embed.iconURL == null && embed.image == null

                        if (embedIsEmpty) {
                            // if we do not emit anything, compose will cause an internal error.
                            // FIXME if you are doing fixme's anyways then check if this is still an issue
                            Box {}
                            return@forEach
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Embed(embed = embed, onLinkClick = {
                            viewUrlInBrowser(context, it)
                        })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}