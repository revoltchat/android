package chat.revolt.components.screens.chat.drawer.channel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.ChannelType
import chat.revolt.components.screens.chat.DoubleDrawerState
import chat.revolt.components.screens.chat.drawer.server.DrawerChannel
import kotlinx.coroutines.launch

@Composable
fun RowScope.ChannelList(
    serverId: String,
    navController: NavController,
    drawerState: DoubleDrawerState
) {
    val coroutineScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
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
                                selected = (channel.id == navBackStackEntry?.arguments?.getString(
                                    "channelId"
                                )),
                                hasUnread = channel.lastMessageID?.let { lastMessageID ->
                                    RevoltAPI.unreads.hasUnread(
                                        channel.id!!,
                                        lastMessageID
                                    )
                                } ?: false,
                                onClick = {
                                    navController.navigate("channel/${channel.id}") {
                                        navController.graph.startDestinationRoute?.let { route ->
                                            popUpTo(route)
                                        }
                                    }
                                    coroutineScope.launch { drawerState.focusCenter() }
                                },
                                onLongClick = {
                                    navController.navigate("channel/${channel.id}/info")
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
                                selected = navBackStackEntry?.arguments?.getString(
                                    "channelId"
                                ) == ch.id,
                                hasUnread = ch.lastMessageID?.let { lastMessageID ->
                                    RevoltAPI.unreads.hasUnread(
                                        ch.id!!,
                                        lastMessageID
                                    )
                                } ?: true,
                                onClick = {
                                    coroutineScope.launch { drawerState.focusCenter() }
                                    navController.navigate("channel/${ch.id}") {
                                        navController.graph.startDestinationRoute?.let { route ->
                                            popUpTo(route)
                                        }
                                    }
                                },
                                onLongClick = {
                                    navController.navigate("channel/${ch.id}/menu")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}