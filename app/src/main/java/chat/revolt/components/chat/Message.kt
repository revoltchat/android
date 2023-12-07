package chat.revolt.components.chat

import android.content.Intent
import android.icu.text.DateFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Selection
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.toSpannable
import chat.revolt.R
import chat.revolt.activities.media.ImageViewActivity
import chat.revolt.activities.media.VideoViewActivity
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.Roles
import chat.revolt.api.internals.SpecialUsers
import chat.revolt.api.internals.ULID
import chat.revolt.api.internals.WebCompat
import chat.revolt.api.internals.solidColor
import chat.revolt.api.routes.channel.react
import chat.revolt.api.routes.channel.unreact
import chat.revolt.api.routes.microservices.january.asJanuaryProxyUrl
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.api.schemas.User
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.UserAvatarWidthPlaceholder
import chat.revolt.internals.markdown.LongClickableSpan
import kotlinx.coroutines.launch
import chat.revolt.api.schemas.Message as MessageSchema

@Composable
fun authorColour(message: MessageSchema): Brush {
    return if (message.masquerade?.colour != null) {
        WebCompat.parseColour(message.masquerade.colour)
    } else {
        val defaultColour = Brush.solidColor(LocalContentColor.current)

        val serverId = RevoltAPI.channelCache[message.channel]?.server ?: return defaultColour

        val highestRole = message.author?.let {
            Roles.resolveHighestRole(serverId, it, withColour = true)
        } ?: return defaultColour

        highestRole.colour?.let { WebCompat.parseColour(it) }
            ?: defaultColour
    }
}

@Composable
fun authorName(message: MessageSchema): String {
    if (message.masquerade?.name != null) {
        return message.masquerade.name
    }

    val serverId =
        RevoltAPI.channelCache[message.channel]?.server
            ?: return RevoltAPI.userCache[message.author]?.let { User.resolveDefaultName(it) }
                ?: stringResource(R.string.unknown)

    val member = message.author?.let { RevoltAPI.members.getMember(serverId, it) }
        ?: return stringResource(R.string.unknown)
    return member.nickname
        ?: RevoltAPI.userCache[message.author]?.let { User.resolveDefaultName(it) }
        ?: stringResource(R.string.unknown)
}

@Composable
fun authorAvatarUrl(message: MessageSchema): String? {
    if (message.masquerade?.avatar != null) {
        return asJanuaryProxyUrl(message.masquerade.avatar)
    }

    val serverId =
        RevoltAPI.channelCache[message.channel]?.server ?: return null
    val member = message.author?.let { RevoltAPI.members.getMember(serverId, it) }
        ?: return null

    return member.avatar?.let { "$REVOLT_FILES/avatars/${it.id}?max_side=256" }
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

fun formatLongAsTime(time: Long): String {
    val date = java.util.Date(time)

    val withinLastWeek = System.currentTimeMillis() - time < 604800000

    return if (withinLastWeek) {
        val relativeDate = DateUtils.getRelativeTimeSpanString(
            time,
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_ALL
        )
        val relativeTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(date)

        "$relativeDate $relativeTime"
    } else {
        val absoluteDate = DateFormat.getDateInstance(DateFormat.SHORT).format(date)
        val absoluteTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(date)

        "$absoluteDate $absoluteTime"
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun Message(
    message: MessageSchema,
    truncate: Boolean = false,
    parse: (MessageSchema) -> SpannableStringBuilder = { SpannableStringBuilder(it.content) },
    onMessageContextMenu: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onNameClick: (() -> Unit)? = null,
    canReply: Boolean = false,
    onReply: () -> Unit = {}
) {
    val author = RevoltAPI.userCache[message.author] ?: return CircularProgressIndicator()
    val context = LocalContext.current
    val contentColor = LocalContentColor.current

    val scope = rememberCoroutineScope()

    val attachmentView = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            // do nothing
        }
    )
    Column {
        if (message.tail == false) {
            Spacer(modifier = Modifier.height(10.dp))
        }

        Column(
            modifier = Modifier.then(
                if (message.mentions?.contains(RevoltAPI.selfId) == true) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                } else {
                    Modifier
                }
            )
        ) {
            message.replies?.forEach { reply ->
                val replyMessage = RevoltAPI.messageCache[reply]

                InReplyTo(
                    messageId = reply,
                    withMention = replyMessage?.author?.let {
                        message.mentions?.contains(
                            replyMessage.author
                        )
                    }
                        ?: false
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
                        onDoubleClick = {
                            if (canReply) {
                                onReply()
                            }
                        },
                        onLongClick = {
                            onMessageContextMenu()
                        }
                    )
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            ) {
                if (message.tail == false) {
                    Column {
                        Spacer(modifier = Modifier.height(4.dp))
                        UserAvatar(
                            username = User.resolveDefaultName(author),
                            userId = author.id ?: message.id ?: ULID.makeSpecial(0),
                            avatar = author.avatar,
                            rawUrl = authorAvatarUrl(message),
                            onClick = onAvatarClick
                        )
                    }
                } else {
                    UserAvatarWidthPlaceholder()
                }

                Column(modifier = Modifier.padding(start = 10.dp)) {
                    if (message.tail == false) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = authorName(message),
                                style = LocalTextStyle.current.copy(
                                    fontWeight = FontWeight.Bold,
                                    brush = authorColour(message)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.then(
                                    if (onNameClick != null)
                                        Modifier.combinedClickable(
                                            onClick = onNameClick,
                                            onLongClick = {
                                                onMessageContextMenu()
                                            }
                                        )
                                    else Modifier
                                )
                            )

                            InlineBadges(
                                bot = author.bot != null && message.masquerade == null,
                                bridge = message.masquerade != null && author.bot != null,
                                platformModeration = author.id == SpecialUsers.PLATFORM_MODERATION_USER,
                                teamMember = author.id in SpecialUsers.TEAM_MEMBER_FLAIRS.keys,
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

                            Spacer(modifier = Modifier.width(2.dp))

                            if (message.edited != null) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(id = R.string.edited),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(
                                        alpha = 0.5f
                                    ),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    key(message.content) {
                        message.content?.let {
                            if (message.content.isBlank()) return@let // if only an attachment is sent

                            AndroidView(
                                factory = { ctx ->
                                    androidx.appcompat.widget.AppCompatTextView(ctx).apply {
                                        maxLines = if (truncate) 1 else Int.MAX_VALUE
                                        ellipsize = TextUtils.TruncateAt.END
                                        textSize = 16f
                                        typeface = ResourcesCompat.getFont(ctx, R.font.inter)

                                        setOnTouchListener(object : OnTouchListener {
                                            private var longClickHandler: Handler? = null
                                            private var isLongPressed = false

                                            override fun onTouch(
                                                widget: View?,
                                                event: MotionEvent?
                                            ): Boolean {
                                                if (longClickHandler == null) {
                                                    longClickHandler =
                                                        Handler(Looper.getMainLooper())
                                                }
                                                if (event == null) return false

                                                val action = event.action
                                                if (action == MotionEvent.ACTION_CANCEL) {
                                                    longClickHandler?.removeCallbacksAndMessages(
                                                        null
                                                    )
                                                }
                                                if (action == MotionEvent.ACTION_UP ||
                                                    action == MotionEvent.ACTION_DOWN
                                                ) {
                                                    var x = event.x.toInt()
                                                    var y = event.y.toInt()
                                                    x -= (widget as androidx.appcompat.widget.AppCompatTextView).totalPaddingLeft
                                                    y -= widget.totalPaddingTop
                                                    x += widget.scrollX
                                                    y += widget.scrollY

                                                    val spannedString = widget.text as SpannedString

                                                    val layout = widget.layout
                                                    val line = layout.getLineForVertical(y)
                                                    val off = layout.getOffsetForHorizontal(
                                                        line,
                                                        x.toFloat()
                                                    )
                                                    val link = spannedString.getSpans(
                                                        off,
                                                        off,
                                                        LongClickableSpan::class.java
                                                    )
                                                    if (link.isNotEmpty()) {
                                                        if (action == MotionEvent.ACTION_UP) {
                                                            longClickHandler?.removeCallbacksAndMessages(
                                                                null
                                                            )
                                                            if (!isLongPressed) {
                                                                link[0].onClick(widget)
                                                            }
                                                            isLongPressed = false
                                                        } else {
                                                            Selection.setSelection(
                                                                spannedString.toSpannable(),
                                                                spannedString.getSpanStart(link[0]),
                                                                spannedString.getSpanEnd(link[0])
                                                            )
                                                            longClickHandler?.postDelayed(
                                                                {
                                                                    link[0].onLongClick(widget)
                                                                    isLongPressed = true
                                                                },
                                                                250L
                                                            )
                                                        }
                                                        return true
                                                    }
                                                }
                                                return false
                                            }
                                        })

                                        setTextColor(contentColor.toArgb())
                                    }
                                },
                                update = {
                                    it.text = parse(message)
                                }
                            )
                        }
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

                    if ((message.reactions?.size ?: 0) > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            message.reactions?.forEach { reaction ->
                                Reaction(reaction.key, reaction.value,
                                    onClick = { hasOwn ->
                                        scope.launch {
                                            if (hasOwn) {
                                                unreact(
                                                    message.channel!!,
                                                    message.id!!,
                                                    reaction.key
                                                )
                                            } else {
                                                react(
                                                    message.channel!!,
                                                    message.id!!,
                                                    reaction.key
                                                )
                                            }
                                        }
                                    }
                                ) {

                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
