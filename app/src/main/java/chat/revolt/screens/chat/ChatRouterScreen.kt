package chat.revolt.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.realtime.DisconnectionState
import chat.revolt.api.realtime.RealtimeSocket
import chat.revolt.api.schemas.ChannelType
import chat.revolt.components.chat.DisconnectedNotice
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.screens.chat.DrawerChannel
import chat.revolt.screens.chat.views.ChannelScreen
import chat.revolt.screens.chat.views.HomeScreen
import kotlinx.coroutines.launch

class ChatRouterViewModel : ViewModel() {
    private var _currentServer = mutableStateOf("home")
    val currentServer: String
        get() = _currentServer.value

    fun setCurrentServer(serverId: String) {
        _currentServer.value = serverId
    }

    fun navigateToServer(serverId: String, navController: NavController) {
        setCurrentServer(serverId)

        if (serverId == "home") {
            navController.navigate("home") {
                popUpTo("home") {
                    inclusive = true
                }
            }
            return
        }

        val channelId = RevoltAPI.serverCache[serverId]?.channels?.firstOrNull()
        navController.navigate("channel/$channelId") {
            popUpTo("home") {
                inclusive = true
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRouterScreen(topNav: NavController, viewModel: ChatRouterViewModel = viewModel()) {
    val channelDrawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    Column() {
        AnimatedVisibility(visible = RealtimeSocket.disconnectionState != DisconnectionState.Connected) {
            DisconnectedNotice(
                state = RealtimeSocket.disconnectionState,
                onReconnect = {
                    RealtimeSocket.updateDisconnectionState(DisconnectionState.Reconnecting)
                    scope.launch { RevoltAPI.connectWS() }
                })
        }
        DismissibleNavigationDrawer(
            drawerState = channelDrawerState,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth()) {
                        Row {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.navigateToServer("home", navController)
                                    },
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = stringResource(id = R.string.home),
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }

                                RevoltAPI.serverCache.values.forEach { server ->
                                    if (server.name == null) return@forEach

                                    if (server.icon != null) {
                                        RemoteImage(
                                            url = "$REVOLT_FILES/icons/${server.icon.id!!}/server.png?max_side=256",
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    viewModel.navigateToServer(
                                                        server.id!!,
                                                        navController
                                                    )
                                                },
                                            description = "${server.name}"
                                        )
                                    } else {
                                        // return a placeholder icon, currently the first letter of the server name in a circle
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable {
                                                    viewModel.navigateToServer(
                                                        server.id!!,
                                                        navController
                                                    )
                                                }
                                        ) {
                                            Text(
                                                text = server.name.first().toString(),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            Crossfade(targetState = viewModel.currentServer) {
                                Column(
                                    Modifier
                                        .weight(1f)
                                ) {
                                    if (it == "home") {
                                        Column(
                                            Modifier
                                                .weight(1f)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            RevoltAPI.channelCache.values.filter { it.channelType == ChannelType.Group }
                                                .forEach { channel ->
                                                    DrawerChannel(
                                                        name = channel.name ?: "GDM #${channel.id}",
                                                        channelType = ChannelType.Group,
                                                        selected = channel.id == (navBackStackEntry?.arguments?.getString(
                                                            "channelId"
                                                        ) ?: false),
                                                        onClick = {
                                                            navController.navigate("channel/${channel.id}")
                                                            scope.launch {
                                                                channelDrawerState.close()
                                                            }
                                                        }
                                                    )
                                                }
                                        }
                                    } else {
                                        val server = RevoltAPI.serverCache[it]

                                        Text(
                                            text = server?.name ?: stringResource(R.string.unknown),
                                            fontWeight = FontWeight.Black,
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
                                                        onClick = {
                                                            scope.launch { channelDrawerState.close() }
                                                            navController.navigate("channel/${ch.id}") {
                                                                popUpTo("home") {
                                                                    inclusive = true
                                                                }
                                                            }
                                                        })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Column(Modifier.fillMaxSize()) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(navController = topNav)
                    }
                    composable("channel/{channelId}") { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId")
                        if (channelId != null) {
                            ChannelScreen(navController, channelId = channelId)
                        }
                    }
                }
            }
        }
    }
}