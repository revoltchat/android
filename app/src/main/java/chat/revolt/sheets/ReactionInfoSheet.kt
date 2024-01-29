package chat.revolt.sheets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.MessageProcessor
import chat.revolt.api.internals.isUlid
import chat.revolt.api.routes.custom.fetchEmoji
import chat.revolt.api.routes.user.fetchUser
import chat.revolt.api.schemas.Emoji
import chat.revolt.api.schemas.User
import chat.revolt.components.chat.MemberListItem
import chat.revolt.components.generic.RemoteImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReactionInfoSheet(messageId: String, emoji: String, onDismiss: () -> Unit) {
    val message = RevoltAPI.messageCache[messageId] ?: return
    val channel = RevoltAPI.channelCache[message.channel] ?: return
    val reactions = message.reactions
    val reactionEmoji = reactions?.keys?.toList()

    val extendedEmojiInfo = remember(emoji) { mutableStateListOf<Emoji>() }

    LaunchedEffect(reactionEmoji) {
        reactionEmoji?.forEach {
            if (it.isUlid()) {
                extendedEmojiInfo.add(RevoltAPI.emojiCache[it] ?: fetchEmoji(it))
            }
        }
    }

    var selectedReactionIndex by remember(
        messageId,
        emoji
    ) { mutableIntStateOf(reactionEmoji?.indexOfFirst { it == emoji } ?: 0) }

    if (selectedReactionIndex >= (reactionEmoji?.size ?: 0)) {
        selectedReactionIndex = 0
    }

    if (reactionEmoji?.isEmpty() == true) {
        onDismiss()
    }

    LazyColumn {
        stickyHeader(key = "tabs") {
            ScrollableTabRow(
                selectedTabIndex = selectedReactionIndex,
                modifier = Modifier.fillMaxWidth(),
                divider = {}
            ) {
                reactionEmoji?.forEachIndexed { index, emoji ->
                    Tab(
                        text = {
                            if (emoji.isUlid()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RemoteImage(
                                        url = "$REVOLT_FILES/emojis/${emoji}/emoji.gif",
                                        description = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.size(4.dp))
                                    Text("${reactions[emoji]?.size ?: 0}")
                                }
                            } else {
                                Text("$emoji ${reactions[emoji]?.size ?: 0}")
                            }
                        },
                        selected = selectedReactionIndex == index,
                        onClick = { selectedReactionIndex = index }
                    )
                }
            }
            Divider()
        }

        if (reactionEmoji?.isNotEmpty() == true) {
            item("info") {
                val current = reactionEmoji[selectedReactionIndex]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(0.dp))
                ) {
                    if (current.isUlid()) {
                        val cached = extendedEmojiInfo.find { it.id == current }
                        RemoteImage(
                            url = "$REVOLT_FILES/emojis/$current/emoji.gif",
                            description = cached?.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(32.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = current,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                ),
                                modifier = Modifier
                                    .size(64.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(
                            top = 16.dp,
                            start = 0.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        )
                    ) {
                        if (current.isUlid()) {
                            val cached = extendedEmojiInfo.find { it.id == current }
                            Text(
                                text = ":${cached?.name ?: current}:",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.15.sp
                            )
                        } else {
                            Text(
                                text = MessageProcessor.emoji.unicodeAsShortcode(current)
                                    ?: current,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.15.sp
                            )
                        }

                        Text(
                            text = if (current.isUlid()) {
                                val cached = extendedEmojiInfo.find { it.id == current }
                                if (cached?.parent != null) {
                                    when (cached.parent.type) {
                                        "Server" -> RevoltAPI.serverCache[cached.parent.id]?.name?.let {
                                            stringResource(
                                                id = R.string.emote_info_from_server,
                                                it
                                            )
                                        }
                                            ?: stringResource(id = R.string.emote_info_from_server_unknown)

                                        else -> stringResource(id = R.string.emote_info_from_server_unknown)
                                    }
                                } else {
                                    stringResource(id = R.string.emote_info_from_server_unknown)
                                }
                            } else {
                                stringResource(id = R.string.emote_info_from_unicode)
                            }
                        )
                    }
                }
            }

            val reactionsForEmoji = reactions[reactionEmoji[selectedReactionIndex]]
            items(reactionsForEmoji?.size ?: 0) { index ->
                val reaction = reactionsForEmoji?.get(index) ?: return@items
                val userOrNull = RevoltAPI.userCache[reaction]
                val user = userOrNull ?: User.getPlaceholder(reaction)
                val member = if (channel.server != null && user.id != null) {
                    RevoltAPI.members.getMember(channel.server, user.id)
                } else {
                    null
                }

                LaunchedEffect(reaction) {
                    if (reaction !in RevoltAPI.userCache) {
                        RevoltAPI.userCache[reaction] = fetchUser(reaction)
                    }
                }

                MemberListItem(
                    member = member,
                    user = user,
                    serverId = channel.server,
                    userId = reaction,
                )
            }
        }

        item("bottom") {
            Spacer(Modifier.size(16.dp))
        }
    }
}