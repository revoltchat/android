package chat.revolt.components.screens.chat.drawer.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ChannelUtils
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.schemas.User
import chat.revolt.components.generic.presenceFromStatus
import chat.revolt.components.screens.chat.drawer.server.DrawerChannel
import chat.revolt.sheets.ChannelContextSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.ChannelList(
    serverId: String,
    currentDestination: String?,
    currentChannel: String?,
    onChannelClick: (String) -> Unit,
    onSpecialClick: (String) -> Unit,
) {
    var channelContextSheetShown by remember { mutableStateOf(false) }
    var channelContextSheetTarget by remember { mutableStateOf("") }

    if (channelContextSheetShown) {
        val channelContextSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = channelContextSheetState,
            onDismissRequest = {
                channelContextSheetShown = false
            },
        ) {
            ChannelContextSheet(
                channelId = channelContextSheetTarget,
                onHideSheet = {
                    channelContextSheetState.hide()
                    channelContextSheetShown = false
                }
            )
        }
    }

    val dmAbleChannels =
        RevoltAPI.channelCache.values
            .filter { it.channelType == ChannelType.DirectMessage || it.channelType == ChannelType.Group }
            .filter { if (it.channelType == ChannelType.DirectMessage) it.active == true else true }
            .sortedBy { it.lastMessageID ?: it.id }
            .reversed()

    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth(),
    ) {
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxSize(),
        ) {
            if (serverId == "home") {
                item(
                    key = "header"
                ) {
                    Text(
                        text = stringResource(R.string.direct_messages),
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                item(
                    key = "home"
                ) {
                    DrawerChannel(
                        name = stringResource(R.string.home),
                        channelType = ChannelType.TextChannel,
                        selected = currentDestination == "home",
                        hasUnread = false,
                        onClick = {
                            onSpecialClick("home")
                        },
                        large = true,
                    )
                }

                item(
                    key = "notes"
                ) {
                    val notesChannelId =
                        RevoltAPI.channelCache.values.firstOrNull { it.channelType == ChannelType.SavedMessages }?.id

                    DrawerChannel(
                        name = stringResource(R.string.channel_notes),
                        channelType = ChannelType.SavedMessages,
                        selected = currentDestination == "channel/{channelId}" && currentChannel == notesChannelId,
                        hasUnread = false,
                        onClick = {
                            onChannelClick(notesChannelId ?: return@DrawerChannel)
                        },
                        large = true,
                    )
                }

                item(
                    key = "divider"
                ) {
                    Surface(
                        Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                            .height(1.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ) {}
                }

                items(
                    dmAbleChannels.size,
                    key = { index ->
                        val channel = dmAbleChannels.getOrNull(index)
                        channel?.id ?: index
                    }
                ) {
                    val channel = dmAbleChannels.getOrNull(it) ?: return@items

                    val partner =
                        if (channel.channelType == ChannelType.DirectMessage) RevoltAPI.userCache[ChannelUtils.resolveDMPartner(
                            channel
                        )] else null

                    DrawerChannel(
                        name = partner?.let { p -> User.resolveDefaultName(p) } ?: channel.name
                        ?: stringResource(R.string.unknown),
                        channelType = channel.channelType ?: ChannelType.TextChannel,
                        selected = currentDestination == "channel/{channelId}" && currentChannel == channel.id,
                        hasUnread = channel.lastMessageID?.let { lastMessageID ->
                            RevoltAPI.unreads.hasUnread(
                                channel.id!!,
                                lastMessageID
                            )
                        } ?: false,
                        dmPartnerIcon = partner?.avatar ?: channel.icon,
                        dmPartnerId = partner?.id,
                        dmPartnerName = partner?.let { p -> User.resolveDefaultName(p) },
                        dmPartnerStatus = presenceFromStatus(
                            status = partner?.status?.presence ?: "Offline",
                            online = partner?.online ?: false
                        ),
                        onClick = {
                            onChannelClick(channel.id ?: return@DrawerChannel)
                        },
                        onLongClick = {
                            channelContextSheetTarget = channel.id ?: return@DrawerChannel
                            channelContextSheetShown = true
                        }
                    )
                }
            } else {
                val server = RevoltAPI.serverCache[serverId]

                item {
                    Text(
                        text = server?.name
                            ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (server?.channels?.isEmpty() == true) {
                    item {
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_channels_heading),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = stringResource(R.string.no_channels_body),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    items(
                        server?.channels?.size ?: 0,
                        key = { server?.channels?.get(it) ?: "" }
                    ) {
                        server?.channels?.get(it)?.let { channelId ->
                            RevoltAPI.channelCache[channelId]?.let { ch ->
                                DrawerChannel(
                                    name = ch.name!!,
                                    channelType = ch.channelType!!,
                                    selected = currentDestination == "channel/{channelId}" && currentChannel == ch.id,
                                    hasUnread = ch.lastMessageID?.let { lastMessageID ->
                                        RevoltAPI.unreads.hasUnread(
                                            ch.id!!,
                                            lastMessageID
                                        )
                                    } ?: true,
                                    onClick = {
                                        onChannelClick(ch.id ?: return@DrawerChannel)
                                    },
                                    onLongClick = {
                                        channelContextSheetTarget = ch.id ?: return@DrawerChannel
                                        channelContextSheetShown = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}