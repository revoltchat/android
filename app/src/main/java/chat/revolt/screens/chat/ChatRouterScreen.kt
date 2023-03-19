package chat.revolt.screens.chat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.realtime.DisconnectionState
import chat.revolt.api.realtime.RealtimeSocket
import chat.revolt.components.chat.DisconnectedNotice
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.presenceFromStatus
import chat.revolt.components.screens.chat.DoubleDrawer
import chat.revolt.components.screens.chat.DoubleDrawerOpenState
import chat.revolt.components.screens.chat.drawer.channel.ChannelList
import chat.revolt.components.screens.chat.drawer.server.DrawerServer
import chat.revolt.components.screens.chat.drawer.server.DrawerServerlikeIcon
import chat.revolt.components.screens.chat.drawer.server.ServerDrawerSeparator
import chat.revolt.components.screens.chat.rememberDoubleDrawerState
import chat.revolt.screens.chat.dialogs.safety.ReportMessageDialog
import chat.revolt.screens.chat.sheets.ChannelContextSheet
import chat.revolt.screens.chat.sheets.ChannelInfoSheet
import chat.revolt.screens.chat.sheets.MessageContextSheet
import chat.revolt.screens.chat.sheets.StatusSheet
import chat.revolt.screens.chat.views.ChannelScreen
import chat.revolt.screens.chat.views.HomeScreen
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.bottomSheet
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import kotlinx.coroutines.flow.distinctUntilChanged
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
                navController.graph.startDestinationRoute?.let { route ->
                    popUpTo(route)
                }
            }
            return
        }

        val channelId = RevoltAPI.serverCache[serverId]?.channels?.firstOrNull()
        navController.navigate("channel/$channelId") {
            navController.graph.startDestinationRoute?.let { route ->
                popUpTo(route)
            }
        }
    }
}

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ChatRouterScreen(topNav: NavController, viewModel: ChatRouterViewModel = viewModel()) {
    val drawerState = rememberDoubleDrawerState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)

    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue }
            .distinctUntilChanged()
            .collect { state ->
                if (state != DoubleDrawerOpenState.Center) {
                    keyboardController?.hide()
                }
            }
    }

    ModalBottomSheetLayout(
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        bottomSheetNavigator = bottomSheetNavigator,
    ) {
        Column {
            AnimatedVisibility(visible = RealtimeSocket.disconnectionState != DisconnectionState.Connected) {
                DisconnectedNotice(
                    state = RealtimeSocket.disconnectionState,
                    onReconnect = {
                        RealtimeSocket.updateDisconnectionState(DisconnectionState.Reconnecting)
                        scope.launch { RevoltAPI.connectWS() }
                    })
            }

            DoubleDrawer(
                state = drawerState,
                startPanel = {
                    Column(Modifier.fillMaxWidth()) {
                        Row {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                UserAvatar(
                                    username = RevoltAPI.userCache[RevoltAPI.selfId]?.username
                                        ?: "",
                                    presence = presenceFromStatus(
                                        RevoltAPI.userCache[RevoltAPI.selfId]?.status?.presence
                                            ?: ""
                                    ),
                                    userId = RevoltAPI.selfId ?: "",
                                    avatar = RevoltAPI.userCache[RevoltAPI.selfId]?.avatar,
                                    size = 48.dp,
                                    presenceSize = 16.dp,
                                    onClick = {
                                        viewModel.navigateToServer("home", navController)
                                    },
                                    onLongClick = {
                                        navController.navigate("status")
                                    },
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(48.dp)
                                )

                                ServerDrawerSeparator()

                                RevoltAPI.serverCache.values
                                    .sortedBy { it.id }
                                    .forEach { server ->
                                        if (server.id == null || server.name == null) return@forEach

                                        DrawerServer(
                                            iconId = server.icon?.id,
                                            serverName = server.name,
                                            hasUnreads = RevoltAPI.unreads.serverHasUnread(server.id),
                                        ) {
                                            viewModel.navigateToServer(
                                                server.id,
                                                navController
                                            )
                                        }
                                    }

                                DrawerServerlikeIcon(
                                    onClick = {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.comingsoon_toast),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(id = R.string.server_plus_alt),
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }

                            Crossfade(targetState = viewModel.currentServer) {
                                ChannelList(
                                    serverId = it,
                                    navController = navController,
                                    drawerState = drawerState
                                )
                            }
                        }
                    }
                },
                endPanel = {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👋", fontSize = 64.sp)
                    }
                },
            ) {
                Column(Modifier.fillMaxSize()) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(navController = topNav)
                        }
                        composable("channel/{channelId}") { backStackEntry ->
                            val channelId = backStackEntry.arguments?.getString("channelId")
                            if (channelId != null) {
                                ChannelScreen(
                                    navController = navController,
                                    channelId = channelId
                                )
                            }
                        }

                        bottomSheet("channel/{channelId}/info") { backStackEntry ->
                            val channelId = backStackEntry.arguments?.getString("channelId")
                            if (channelId != null) {
                                ChannelInfoSheet(
                                    navController = navController,
                                    channelId = channelId
                                )
                            }
                        }
                        bottomSheet("channel/{channelId}/menu") { backStackEntry ->
                            val channelId = backStackEntry.arguments?.getString("channelId")
                            if (channelId != null) {
                                ChannelContextSheet(
                                    navController = navController,
                                    channelId = channelId
                                )
                            }
                        }
                        bottomSheet("message/{messageId}/menu") { backStackEntry ->
                            val messageId = backStackEntry.arguments?.getString("messageId")
                            if (messageId != null) {
                                MessageContextSheet(
                                    navController = navController,
                                    messageId = messageId
                                )
                            }
                        }
                        bottomSheet("status") {
                            StatusSheet(navController = navController, topNav = topNav)
                        }

                        dialog("report/message/{messageId}") { backStackEntry ->
                            val messageId = backStackEntry.arguments?.getString("messageId")
                            if (messageId != null) {
                                ReportMessageDialog(
                                    navController = navController,
                                    messageId = messageId
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}