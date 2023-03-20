package chat.revolt.components.screens.chat.drawer.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.ChannelType
import chat.revolt.components.screens.chat.DoubleDrawerState
import chat.revolt.components.screens.chat.drawer.server.DrawerChannel
import kotlinx.coroutines.launch

@Composable
fun RowScope.ChannelList(
    serverId: String,
    drawerState: DoubleDrawerState,
    currentChannel: String?,
    onChannelClick: (String) -> Unit,
    onChannelLongClick: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth(),
    ) {
        Column(
            Modifier
                .weight(1f)
        ) {
            if (serverId == "home") {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    RevoltAPI.channelCache.values.filter { it.channelType == ChannelType.Group }
                        .forEach { channel ->
                            DrawerChannel(
                                name = channel.name
                                    ?: "GDM #${channel.id}",
                                channelType = ChannelType.Group,
                                selected = currentChannel == channel.id,
                                hasUnread = channel.lastMessageID?.let { lastMessageID ->
                                    RevoltAPI.unreads.hasUnread(
                                        channel.id!!,
                                        lastMessageID
                                    )
                                } ?: false,
                                onClick = {
                                    onChannelClick(channel.id ?: return@DrawerChannel)
                                    coroutineScope.launch { drawerState.focusCenter() }
                                },
                                onLongClick = {
                                    onChannelLongClick(channel.id ?: return@DrawerChannel)
                                }
                            )
                        }
                }
            } else {
                val server = RevoltAPI.serverCache[serverId]

                Text(
                    text = server?.name
                        ?: stringResource(R.string.unknown),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(16.dp)
                )

                if (server?.channels?.isEmpty() == true) {
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
                } else {
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        server?.channels?.forEach { channelId ->
                            RevoltAPI.channelCache[channelId]?.let { ch ->
                                DrawerChannel(
                                    name = ch.name!!,
                                    channelType = ch.channelType!!,
                                    selected = currentChannel == ch.id,
                                    hasUnread = ch.lastMessageID?.let { lastMessageID ->
                                        RevoltAPI.unreads.hasUnread(
                                            ch.id!!,
                                            lastMessageID
                                        )
                                    } ?: true,
                                    onClick = {
                                        onChannelClick(ch.id ?: return@DrawerChannel)
                                        coroutineScope.launch { drawerState.focusCenter() }
                                    },
                                    onLongClick = {
                                        onChannelLongClick(ch.id ?: return@DrawerChannel)
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