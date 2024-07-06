package chat.revolt.components.chat

import android.content.Intent
import android.icu.text.DateFormat
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.activities.media.ImageViewActivity
import chat.revolt.activities.media.VideoViewActivity
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.BrushCompat
import chat.revolt.api.internals.Roles
import chat.revolt.api.internals.SpecialUsers
import chat.revolt.api.internals.ULID
import chat.revolt.api.internals.solidColor
import chat.revolt.api.routes.channel.react
import chat.revolt.api.routes.channel.unreact
import chat.revolt.api.routes.microservices.january.asJanuaryProxyUrl
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.api.schemas.User
import chat.revolt.api.settings.GlobalState
import chat.revolt.api.settings.MessageReplyStyle
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.UserAvatarWidthPlaceholder
import chat.revolt.components.markdown.LocalMarkdownTreeConfig
import chat.revolt.components.markdown.RichMarkdown
import kotlinx.coroutines.launch
import chat.revolt.api.schemas.Message as MessageSchema

@Composable
fun authorColour(message: MessageSchema): Brush {
    return if (message.masquerade?.colour != null) {
        BrushCompat.parseColour(message.masquerade.colour)
    } else {
        val defaultColour = Brush.solidColor(LocalContentColor.current)

        val serverId = RevoltAPI.channelCache[message.channel]?.server ?: return defaultColour

        val highestRole = message.author?.let {
            Roles.resolveHighestRole(serverId, it, withColour = true)
        } ?: return defaultColour

        highestRole.colour?.let { BrushCompat.parseColour(it) }
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
    onMessageContextMenu: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onNameClick: (() -> Unit)? = null,
    canReply: Boolean = false,
    onReply: () -> Unit = {},
    onAddReaction: () -> Unit = {},
) {
    val author = RevoltAPI.userCache[message.author] ?: return CircularProgressIndicator()
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val attachmentView = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            // do nothing
        }
    )

    val authorIsBlocked = remember(author) { author.relationship == "Blocked" }

    Column(Modifier.animateContentSize()) {
        if (message.tail == false) {
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (authorIsBlocked) {
            Row(
                modifier = Modifier
                    .combinedClickable(
                        onClick = {},
                        onDoubleClick = {},
                        onLongClick = {
                            onMessageContextMenu()
                        }
                    )
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            ) {
                UserAvatarWidthPlaceholder()

                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close_octagon_24dp),
                            contentDescription = null
                        )

                        Text(
                            text = stringResource(R.string.message_blocked),
                            fontSize = 12.sp,
                            color = LocalContentColor.current.copy(alpha = 0.5f),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        } else {
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

                    message.channel?.let { chId ->
                        InReplyTo(
                            channelId = chId,
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
                }

                Row(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {},
                            onDoubleClick = {
                                if (canReply && GlobalState.messageReplyStyle == MessageReplyStyle.DoubleTap) {
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

                                CompositionLocalProvider(
                                    LocalMarkdownTreeConfig provides LocalMarkdownTreeConfig.current.copy(
                                        currentServer = RevoltAPI.channelCache[message.channel]?.server
                                    )
                                ) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    RichMarkdown(input = message.content)
                                }
                            }
                        }

                        message.attachments?.let {
                            message.attachments.forEach { attachment ->
                                Spacer(modifier = Modifier.height(2.dp))
                                MessageAttachment(attachment) {
                                    when (attachment.metadata?.type) {
                                        "Image" -> {
                                            attachmentView.launch(
                                                Intent(
                                                    context,
                                                    ImageViewActivity::class.java
                                                ).apply {
                                                    putExtra("autumnResource", attachment)
                                                }
                                            )
                                        }

                                        "Video" -> {
                                            attachmentView.launch(
                                                Intent(
                                                    context,
                                                    VideoViewActivity::class.java
                                                ).apply {
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
                                when (embed.type) {
                                    "Website", "Text" -> {
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

                                    "Image" -> {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        BoxWithConstraints(
                                            modifier = Modifier
                                                .clip(MaterialTheme.shapes.medium)
                                                .clickable {
                                                    embed.url?.let {
                                                        viewUrlInBrowser(context, it)
                                                    }
                                                }
                                        ) {
                                            embed.url?.let { url ->
                                                RemoteImage(
                                                    url = asJanuaryProxyUrl(url),
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .width(
                                                            embed.width?.toInt()?.dp
                                                                ?: maxWidth
                                                        )
                                                        .aspectRatio(
                                                            embed.width!!.toFloat() / embed.height!!.toFloat()
                                                        ),
                                                    description = null
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }

                                    else -> {
                                        // no-op
                                    }
                                }
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
                                        scope.launch {
                                            ActionChannel.send(
                                                Action.MessageReactionInfo(
                                                    message.id!!,
                                                    reaction.key
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceColorAtElevation(
                                                2.dp
                                            )
                                        )
                                        .clickable(onClick = onAddReaction)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_hamburger_plus_24dp),
                                        contentDescription = stringResource(R.string.message_context_sheet_actions_react),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
