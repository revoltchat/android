package chat.revolt.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import chat.revolt.components.screens.chat.drawer.channel.ChannelList
import chat.revolt.components.screens.chat.drawer.server.DrawerServer
import chat.revolt.components.screens.chat.drawer.server.DrawerServerlikeIcon
import chat.revolt.components.screens.chat.drawer.server.ServerDrawerSeparator
import chat.revolt.persistence.KVStorage
import chat.revolt.screens.chat.dialogs.safety.ReportMessageDialog
import chat.revolt.screens.chat.sheets.AddServerSheet
import chat.revolt.screens.chat.sheets.ChannelContextSheet
import chat.revolt.screens.chat.sheets.ChannelInfoSheet
import chat.revolt.screens.chat.sheets.MessageContextSheet
import chat.revolt.screens.chat.sheets.StatusSheet
import chat.revolt.screens.chat.views.HomeScreen
import chat.revolt.screens.chat.views.NoCurrentChannelScreen
import chat.revolt.screens.chat.views.channel.ChannelScreen
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.bottomSheet
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatRouterViewModel @Inject constructor(
    private val kvStorage: KVStorage
) : ViewModel() {
    private var _currentServer = mutableStateOf("home")
    val currentServer: String
        get() = _currentServer.value

    private var _currentChannel = mutableStateOf<String?>(null)
    val currentChannel: String?
        get() = _currentChannel.value

    private var _sidebarSparkDisplayed = mutableStateOf(true)
    val sidebarSparkDisplayed: Boolean
        get() = _sidebarSparkDisplayed.value

    init {
        viewModelScope.launch {
            _currentServer.value = kvStorage.get("currentServer") ?: "home"
            _currentChannel.value = kvStorage.get("currentChannel")
            _sidebarSparkDisplayed.value = if (kvStorage.getBoolean("sidebarSpark") == null) {
                false
            } else {
                kvStorage.getBoolean("sidebarSpark")!!
            }
        }
    }

    private fun setCurrentServer(serverId: String, save: Boolean = true) {
        _currentServer.value = serverId

        if (save) viewModelScope.launch {
            kvStorage.set("currentServer", serverId)
        }
    }

    private fun setCurrentChannel(channelId: String) {
        _currentChannel.value = channelId

        viewModelScope.launch {
            kvStorage.set("currentChannel", channelId)
        }
    }

    suspend fun setSettingsHintDisplayed() {
        kvStorage.set("sidebarSpark", true)
        _sidebarSparkDisplayed.value = true
    }

    fun navigateToServer(serverId: String, navController: NavController) {
        if (serverId == "home") {
            navController.navigate("home") {
                navController.graph.startDestinationRoute?.let { route ->
                    popUpTo(route)
                }
            }
            setCurrentServer("home")
            return
        }

        val channelId = RevoltAPI.serverCache[serverId]?.channels?.firstOrNull()

        setCurrentServer(serverId, channelId != null)

        if (channelId != null) {
            navigateToChannel(channelId, navController)
        } else {
            navController.navigate("no_current_channel") {
                navController.graph.startDestinationRoute?.let { route ->
                    popUpTo(route)
                }
            }
        }
    }

    fun navigateToChannel(channelId: String, navController: NavController, pure: Boolean = false) {
        if (!pure) setCurrentChannel(channelId)

        navController.navigate("channel/$channelId") {
            navController.graph.startDestinationRoute?.let { route ->
                popUpTo(route)
            }
        }
    }
}

@OptIn(
    ExperimentalMaterialNavigationApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun ChatRouterScreen(topNav: NavController, viewModel: ChatRouterViewModel = hiltViewModel()) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)

    val showSidebarSpark = remember { mutableStateOf(false) }
    val sidebarSparkComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.open_settings_tutorial))
    val sidebarSparkProgress by animateLottieCompositionAsState(
        composition = sidebarSparkComposition,
    )

    BackHandler(enabled = drawerState.isClosed) {
        scope.launch {
            drawerState.open()
        }
    }

    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue }
            .distinctUntilChanged()
            .collect { state ->
                if (state == DrawerValue.Closed) {
                    keyboardController?.hide()
                }
            }
    }

    LaunchedEffect(viewModel.currentChannel) {
        snapshotFlow { viewModel.currentChannel }
            .distinctUntilChanged()
            .collect { channelId ->
                if (channelId != null) {
                    viewModel.navigateToChannel(channelId, navController, pure = true)
                }
            }
    }

    LaunchedEffect(viewModel.sidebarSparkDisplayed) {
        snapshotFlow { viewModel.sidebarSparkDisplayed }
            .distinctUntilChanged()
            .collect { displayed ->
                showSidebarSpark.value = !displayed
            }
    }

    LaunchedEffect(RevoltAPI.selfId) {
        snapshotFlow { RevoltAPI.selfId }
            .distinctUntilChanged()
            .collect { selfId ->
                if (selfId == null) {
                    topNav.popBackStack(
                        topNav.graph.startDestinationRoute!!,
                        inclusive = true
                    )
                    topNav.navigate("splash")
                }
            }
    }

    ModalBottomSheetLayout(
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        bottomSheetNavigator = bottomSheetNavigator,
    ) {
        if (showSidebarSpark.value) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(stringResource(id = R.string.spark_sidebar_settings_tutorial))
                },
                text = {
                    Column {
                        LottieAnimation(
                            composition = sidebarSparkComposition,
                            progress = { sidebarSparkProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            renderMode = RenderMode.HARDWARE
                        )
                        Text(stringResource(id = R.string.spark_sidebar_settings_tutorial_description_1))
                        Text(stringResource(id = R.string.spark_sidebar_settings_tutorial_description_2))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            viewModel.setSettingsHintDisplayed()
                        }
                        showSidebarSpark.value = false
                    }) {
                        Text(stringResource(id = R.string.spark_sidebar_settings_tutorial_acknowledge))
                    }
                }
            )
        }

        Column {
            AnimatedVisibility(visible = RealtimeSocket.disconnectionState != DisconnectionState.Connected) {
                DisconnectedNotice(
                    state = RealtimeSocket.disconnectionState,
                    onReconnect = {
                        RealtimeSocket.updateDisconnectionState(DisconnectionState.Reconnecting)
                        scope.launch { RevoltAPI.connectWS() }
                    })
            }

            DismissibleNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    DismissibleDrawerSheet(
                        drawerContainerColor = Color.Transparent,
                    ) {
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
                                                hasUnreads = RevoltAPI.unreads.serverHasUnread(
                                                    server.id
                                                ),
                                            ) {
                                                viewModel.navigateToServer(
                                                    server.id,
                                                    navController
                                                )
                                            }
                                        }

                                    DrawerServerlikeIcon(
                                        onClick = {
                                            navController.navigate("add_server")
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = stringResource(id = R.string.server_plus_alt),
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }

                                Crossfade(
                                    targetState = viewModel.currentServer,
                                    label = "Channel List"
                                ) {
                                    ChannelList(
                                        serverId = it,
                                        drawerState = drawerState,
                                        currentChannel = viewModel.currentChannel,
                                        onChannelClick = { channelId ->
                                            viewModel.navigateToChannel(channelId, navController)
                                        },
                                        onChannelLongClick = { channelId ->
                                            navController.navigate("channel/$channelId/info")
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
                content = {
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
                                        channelId = channelId,
                                        onToggleDrawer = {
                                            scope.launch {
                                                if (drawerState.isOpen) drawerState.close()
                                                else drawerState.open()
                                            }
                                        }
                                    )
                                }
                            }
                            composable("no_current_channel") {
                                NoCurrentChannelScreen()
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
                            bottomSheet("add_server") {
                                AddServerSheet()
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
                })
        }
    }
}