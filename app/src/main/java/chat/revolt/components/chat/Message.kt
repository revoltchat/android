package chat.revolt.components.chat

import android.content.Intent
import android.icu.text.DateFormat
import android.icu.text.RelativeDateTimeFormatter
import android.net.Uri
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import chat.revolt.activities.media.ImageViewActivity
import chat.revolt.activities.media.VideoViewActivity
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ULID
import chat.revolt.api.internals.WebCompat
import chat.revolt.api.routes.microservices.january.asJanuaryProxyUrl
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.UserAvatarWidthPlaceholder
import chat.revolt.api.schemas.Message as MessageSchema

@Composable
fun authorColour(message: MessageSchema): Color {
    return if (message.masquerade?.colour != null) {
        WebCompat.parseColour(message.masquerade.colour)
    } else {
        LocalContentColor.current
    }
}

@Composable
fun authorName(message: MessageSchema): String {
    return if (message.masquerade?.name != null) {
        message.masquerade.name
    } else {
        RevoltAPI.userCache[message.author]?.username ?: stringResource(id = R.string.unknown)
    }
}

fun viewUrlInBrowser(ctx: android.content.Context, url: String) {
    val customTab = CustomTabsIntent
        .Builder()
        .build()
    customTab.launchUrl(ctx, Uri.parse(url))
}

fun viewAttachmentInBrowser(ctx: android.content.Context, attachment: AutumnResource) {
    val url = "$REVOLT_FILES/attachments/${attachment.id}/${attachment.filename}"
    viewUrlInBrowser(ctx, url)
}


fun formatLongAsTime(
    time: Long,
    context: android.content.Context,
): String {
    val date = java.util.Date(time)

    val withinLastWeek = System.currentTimeMillis() - time < 604800000

    return if (withinLastWeek) {
        val howManyDays = (System.currentTimeMillis() - time) / 86400000

        val relativeDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            RelativeDateTimeFormatter.getInstance()
                .format(
                    -howManyDays.toDouble(),
                    RelativeDateTimeFormatter.RelativeDateTimeUnit.DAY
                )
        } else {
            when (howManyDays.toInt()) {
                0 -> context.getString(R.string.today)
                1 -> context.getString(R.string.yesterday)
                else -> context.getString(R.string.x_days_ago, howManyDays)
            }
        }
        val relativeTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(date)

        "$relativeDate $relativeTime"
    } else {
        val absoluteDate = DateFormat.getDateInstance(DateFormat.SHORT).format(date)
        val absoluteTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(date)

        "$absoluteDate $absoluteTime"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Message(
    message: MessageSchema,
    truncate: Boolean = false,
    parse: (MessageSchema) -> SpannableStringBuilder = { SpannableStringBuilder(it.content) },
    onMessageContextMenu: () -> Unit = {},
    canReply: Boolean = false,
    onReply: () -> Unit = {},
) {
    val author = RevoltAPI.userCache[message.author] ?: return CircularProgressIndicator()
    val context = LocalContext.current
    val contentColor = LocalContentColor.current

    val attachmentView = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            // do nothing
        })

    Column {
        if (message.tail == false) {
            Spacer(modifier = Modifier.height(10.dp))
        }

        message.replies?.forEach { reply ->
            val replyMessage = RevoltAPI.messageCache[reply]

            InReplyTo(
                messageId = reply,
                withMention = replyMessage?.author?.let {
                    message.mentions?.contains(
                        replyMessage.author
                    )
                }
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
                            text = authorName(message),
                            fontWeight = FontWeight.Bold,
                            color = authorColour(message),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        InlineBadges(
                            bot = author.bot != null && message.masquerade == null,
                            bridge = message.masquerade != null && author.bot != null,
                            colour = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp),
                            precedingIfAny = {
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        Text(
                            text = formatLongAsTime(ULID.asTimestamp(message.id!!), context),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        if (message.edited != null) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(id = R.string.edited),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
                            when (attachment.metadata?.type) {
                                "Image" -> {
                                    attachmentView.launch(
                                        Intent(context, ImageViewActivity::class.java).apply {
                                            putExtra("autumnResource", attachment)
                                        }
                                    )
                                }

                                "Video" -> {
                                    attachmentView.launch(
                                        Intent(context, VideoViewActivity::class.java).apply {
                                            putExtra("autumnResource", attachment)
                                        }
                                    )
                                }

                                "Audio" -> {
                                    /* no-op */
                                }

                                else -> {
                                    viewAttachmentInBrowser(context, attachment)
                                }
                            }
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