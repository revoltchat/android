package chat.revolt.screens.chat

import android.annotation.SuppressLint
import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.DirectMessages
import chat.revolt.api.realtime.DisconnectionState
import chat.revolt.api.realtime.RealtimeSocket
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.components.chat.DisconnectedNotice
import chat.revolt.components.screens.chat.drawer.ChannelSideDrawer
import chat.revolt.components.screens.voice.VoiceChannelOverlay
import chat.revolt.internals.Changelogs
import chat.revolt.internals.extensions.BottomSheetInsets
import chat.revolt.internals.extensions.zero
import chat.revolt.persistence.KVStorage
import chat.revolt.screens.chat.dialogs.safety.ReportMessageDialog
import chat.revolt.screens.chat.dialogs.safety.ReportServerDialog
import chat.revolt.screens.chat.dialogs.safety.ReportUserDialog
import chat.revolt.screens.chat.views.FriendsScreen
import chat.revolt.screens.chat.views.HomeScreen
import chat.revolt.screens.chat.views.NoCurrentChannelScreen
import chat.revolt.screens.chat.views.channel.ChannelScreen
import chat.revolt.sheets.AddServerSheet
import chat.revolt.sheets.ChangelogSheet
import chat.revolt.sheets.EmoteInfoSheet
import chat.revolt.sheets.LinkInfoSheet
import chat.revolt.sheets.ReactionInfoSheet
import chat.revolt.sheets.ServerContextSheet
import chat.revolt.sheets.StatusSheet
import chat.revolt.sheets.UserInfoSheet
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChatRouterDestination {
    data object Home : ChatRouterDestination()
    data object Friends : ChatRouterDestination()
    data class Channel(val channelId: String) : ChatRouterDestination()
    data class NoCurrentChannel(val serverId: String?) : ChatRouterDestination()

    fun asSerialisedString(): String {
        return when (this) {
            is Home -> "home"
            is Friends -> "friends"
            is Channel -> "channel/$channelId"
            is NoCurrentChannel -> "no_current_channel/$serverId"
        }
    }

    companion object {
        val default = Home
        val defaultForDMList = Home

        fun fromString(destination: String): ChatRouterDestination {
            return when {
                destination == "home" -> Home
                destination == "friends" -> Friends
                destination.startsWith("no_current_channel/") -> NoCurrentChannel(
                    destination.removePrefix(
                        "no_current_channel/"
                    )
                )

                destination.startsWith("channel/") -> Channel(destination.removePrefix("channel/"))
                else -> default
            }
        }
    }
}

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class ChatRouterViewModel @Inject constructor(
    private val kvStorage: KVStorage,
    @ApplicationContext val context: Context
) : ViewModel() {
    var currentDestination by mutableStateOf<ChatRouterDestination>(ChatRouterDestination.default)
    var sidebarSparkDisplayed by mutableStateOf(true)
    var latestChangelogRead by mutableStateOf(true)
    var latestChangelog by mutableStateOf("")
    var latestChangelogBody by mutableStateOf("")

    private val changelogs = Changelogs(context, kvStorage)

    init {
        viewModelScope.launch {
            val current = kvStorage.get("currentDestination")
            setSaveDestination(ChatRouterDestination.fromString(current ?: ""))

            sidebarSparkDisplayed = if (kvStorage.getBoolean("sidebarSpark") == null) {
                false
            } else {
                kvStorage.getBoolean("sidebarSpark")!!
            }

            latestChangelogRead = changelogs.hasSeenCurrent()
            latestChangelog = changelogs.getLatestChangelogCode()
            latestChangelogBody =
                changelogs.fetchChangelogByVersionCode(latestChangelog.toLong()).rendered
            if (!latestChangelogRead) {
                changelogs.markAsSeen()
            }
        }
    }

    fun setSaveDestination(destination: ChatRouterDestination) {
        currentDestination = destination

        viewModelScope.launch {
            kvStorage.set("currentDestination", destination.asSerialisedString())

            if (destination is ChatRouterDestination.Channel) {
                val server = RevoltAPI.channelCache[destination.channelId]?.server
                if (server != null) {
                    kvStorage.set("lastChannel/$server", destination.channelId)
                }
            }
        }
    }

    suspend fun setSettingsHintDisplayed() {
        kvStorage.set("sidebarSpark", true)
        sidebarSparkDisplayed = true
    }

    fun navigateToServer(serverId: String) {
        viewModelScope.launch {
            val savedLastChannel = kvStorage.get("lastChannel/$serverId")
            val channelId =
                savedLastChannel ?: RevoltAPI.serverCache[serverId]?.channels?.firstOrNull()

            if (channelId != null) {
                setSaveDestination(ChatRouterDestination.Channel(channelId))
            } else {
                setSaveDestination(ChatRouterDestination.NoCurrentChannel(serverId))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRouterScreen(
    topNav: NavController,
    windowSizeClass: WindowSizeClass,
    onNullifiedUser: () -> Unit,
    viewModel: ChatRouterViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    val showSidebarSpark = remember { mutableStateOf(false) }
    val sidebarSparkComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.open_settings_tutorial)
    )
    val sidebarSparkProgress by animateLottieCompositionAsState(
        composition = sidebarSparkComposition
    )

    var showPlatformModDMHint by remember { mutableStateOf(false) }

    var showStatusSheet by remember { mutableStateOf(false) }
    var showAddServerSheet by remember { mutableStateOf(false) }

    var showServerContextSheet by remember { mutableStateOf(false) }
    var serverContextSheetTarget by remember { mutableStateOf("") }

    var showUserContextSheet by remember { mutableStateOf(false) }
    var userContextSheetTarget by remember { mutableStateOf("") }
    var userContextSheetServer by remember { mutableStateOf<String?>(null) }

    var showChannelUnavailableAlert by remember { mutableStateOf(false) }

    var showLinkInfoSheet by remember { mutableStateOf(false) }
    var linkInfoSheetUrl by remember { mutableStateOf("") }

    var showEmoteInfoSheet by remember { mutableStateOf(false) }
    var emoteInfoSheetTarget by remember { mutableStateOf("") }

    var showReactionInfoSheet by remember { mutableStateOf(false) }
    var reactionInfoSheetMessageId by remember { mutableStateOf("") }
    var reactionInfoSheetEmoji by remember { mutableStateOf("") }

    var useTabletAwareUI by remember { mutableStateOf(false) }

    var voiceChannelOverlay by remember { mutableStateOf(false) }
    var voiceChannelOverlayChannelId by remember { mutableStateOf("") }

    var showReportUser by remember { mutableStateOf(false) }
    var reportUserTarget by remember { mutableStateOf("") }

    var showReportMessage by remember { mutableStateOf(false) }
    var reportMessageTarget by remember { mutableStateOf("") }

    var showReportServer by remember { mutableStateOf(false) }
    var reportServerTarget by remember { mutableStateOf("") }

    val toggleDrawerLambda = remember {
        {
            scope.launch {
                if (drawerState.isOpen) {
                    drawerState.close()
                } else {
                    drawerState.open()
                }
            }
        }
    }

    val currentServer = remember(viewModel.currentDestination) {
        when (viewModel.currentDestination) {
            is ChatRouterDestination.Channel -> {
                RevoltAPI.channelCache[(viewModel.currentDestination as ChatRouterDestination.Channel).channelId]?.server
            }

            is ChatRouterDestination.NoCurrentChannel -> {
                (viewModel.currentDestination as ChatRouterDestination.NoCurrentChannel).serverId
            }

            else -> null
        }
    }

    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue }
            .distinctUntilChanged()
            .collect { state ->
                if (state == DrawerValue.Open) {
                    val keyboard =
                        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    keyboard.hideSoftInputFromWindow(view.windowToken, 0)
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
                    onNullifiedUser()
                }
            }
    }

    LaunchedEffect(DirectMessages.unreadDMs()) {
        snapshotFlow { DirectMessages.unreadDMs() }
            .distinctUntilChanged()
            .collect { _ ->
                if (DirectMessages.hasPlatformModerationDM()) {
                    showPlatformModDMHint = true
                }
            }
    }

    LaunchedEffect(windowSizeClass) {
        snapshotFlow { windowSizeClass }
            .distinctUntilChanged()
            .collect { sizeClass ->
                useTabletAwareUI = sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded &&
                        sizeClass.heightSizeClass != WindowHeightSizeClass.Compact
            }
    }

    LaunchedEffect(Unit) {
        while (true) {
            ActionChannel.receive().let { action ->
                when (action) {
                    is Action.OpenUserSheet -> {
                        userContextSheetTarget = action.userId
                        userContextSheetServer = action.serverId
                        showUserContextSheet = true
                    }

                    is Action.SwitchChannel -> {
                        val resolvedChannel = RevoltAPI.channelCache[action.channelId]

                        if (resolvedChannel == null) {
                            showChannelUnavailableAlert = true
                            return@let
                        }

                        viewModel.setSaveDestination(ChatRouterDestination.Channel(action.channelId))
                    }

                    is Action.LinkInfo -> {
                        linkInfoSheetUrl = action.url
                        showLinkInfoSheet = true
                    }

                    is Action.EmoteInfo -> {
                        emoteInfoSheetTarget = action.emoteId
                        showEmoteInfoSheet = true
                    }

                    is Action.MessageReactionInfo -> {
                        reactionInfoSheetMessageId = action.messageId
                        reactionInfoSheetEmoji = action.emoji
                        showReactionInfoSheet = true
                    }

                    is Action.TopNavigate -> {
                        topNav.navigate(action.route)
                    }

                    is Action.ChatNavigate -> {
                        viewModel.setSaveDestination(action.destination)
                    }

                    is Action.ReportUser -> {
                        reportUserTarget = action.userId
                        showReportUser = true
                    }

                    is Action.ReportMessage -> {
                        reportMessageTarget = action.messageId
                        showReportMessage = true
                    }

                    is Action.OpenVoiceChannelOverlay -> {
                        voiceChannelOverlayChannelId = action.channelId
                        voiceChannelOverlay = true
                    }
                }
            }
        }
    }

    var isTouchExplorationEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        isTouchExplorationEnabled = accessibilityManager.isTouchExplorationEnabled
        accessibilityManager.addTouchExplorationStateChangeListener { enabled ->
            isTouchExplorationEnabled = enabled
        }
    }

    if (!viewModel.latestChangelogRead) {
        ChangelogSheet(
            versionName = viewModel.latestChangelog,
            versionIsHistorical = false,
            renderedContents = viewModel.latestChangelogBody,
            onDismiss = {
                viewModel.latestChangelogRead = true
            }
        )
    }

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
                    Text(
                        stringResource(id = R.string.spark_sidebar_settings_tutorial_description_1)
                    )
                    Text(
                        stringResource(id = R.string.spark_sidebar_settings_tutorial_description_2)
                    )
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

    if (showPlatformModDMHint) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(stringResource(id = R.string.notice_platform_mod_dm_title))
            },
            text = {
                Text(stringResource(id = R.string.notice_platform_mod_dm_description))
            },
            confirmButton = {
                TextButton(onClick = {
                    showPlatformModDMHint = false
                    DirectMessages.getPlatformModerationDM()?.id?.let {
                        viewModel.setSaveDestination(ChatRouterDestination.Channel(it))
                    }
                }) {
                    Text(stringResource(id = R.string.notice_platform_mod_dm_acknowledge))
                }
            }
        )
    }

    if (showStatusSheet) {
        val statusSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = statusSheetState,
            onDismissRequest = {
                showStatusSheet = false
            }
        ) {
            StatusSheet(
                onBeforeNavigation = {
                    scope.launch {
                        statusSheetState.hide()
                        showStatusSheet = false
                    }
                },
                onGoSettings = {
                    topNav.navigate("settings")
                }
            )
        }
    }

    if (showAddServerSheet) {
        val addServerSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = addServerSheetState,
            onDismissRequest = {
                showAddServerSheet = false
            }
        ) {
            AddServerSheet()
        }
    }

    if (showServerContextSheet) {
        val serverContextSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = serverContextSheetState,
            onDismissRequest = {
                showServerContextSheet = false
            },
            windowInsets = BottomSheetInsets
        ) {
            ServerContextSheet(
                serverId = serverContextSheetTarget,
                onHideSheet = {
                    serverContextSheetState.hide()
                    showServerContextSheet = false
                },
                onReportServer = {
                    reportServerTarget = currentServer ?: ""
                    showReportServer = true
                }
            )
        }
    }

    if (showUserContextSheet) {
        val userContextSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = userContextSheetState,
            onDismissRequest = {
                showUserContextSheet = false
            }
        ) {
            UserInfoSheet(
                userId = userContextSheetTarget,
                serverId = userContextSheetServer,
                dismissSheet = {
                    userContextSheetState.hide()
                    showUserContextSheet = false
                }
            )
        }
    }

    if (showReportUser) {
        ReportUserDialog(
            onDismiss = { showReportUser = false },
            userId = reportUserTarget
        )
    }

    if (showReportMessage) {
        ReportMessageDialog(
            onDismiss = { showReportMessage = false },
            messageId = reportMessageTarget
        )
    }

    if (showReportServer) {
        ReportServerDialog(
            onDismiss = { showReportServer = false },
            serverId = reportServerTarget
        )
    }

    if (showChannelUnavailableAlert) {
        AlertDialog(
            onDismissRequest = {
                showChannelUnavailableAlert = false
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_lock_alert_24dp),
                    contentDescription = null, // decorative
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(id = R.string.channel_link_invalid),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.channel_link_invalid_description),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showChannelUnavailableAlert = false
                }) {
                    Text(text = stringResource(id = R.string.ok))
                }
            }
        )
    }

    if (showLinkInfoSheet) {
        val linkInfoSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = linkInfoSheetState,
            onDismissRequest = {
                showLinkInfoSheet = false
            }
        ) {
            LinkInfoSheet(
                url = linkInfoSheetUrl,
                onDismiss = {
                    showLinkInfoSheet = false
                }
            )
        }
    }

    if (showEmoteInfoSheet) {
        val emoteInfoSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = emoteInfoSheetState,
            onDismissRequest = {
                showEmoteInfoSheet = false
            }
        ) {
            EmoteInfoSheet(
                id = emoteInfoSheetTarget,
                onDismiss = {
                    showEmoteInfoSheet = false
                }
            )
        }
    }

    if (showReactionInfoSheet) {
        val reactionInfoSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = reactionInfoSheetState,
            onDismissRequest = {
                showReactionInfoSheet = false
            }
        ) {
            ReactionInfoSheet(
                messageId = reactionInfoSheetMessageId,
                emoji = reactionInfoSheetEmoji,
                onDismiss = {
                    showReactionInfoSheet = false
                }
            )
        }
    }

    if (voiceChannelOverlay) {
        VoiceChannelOverlay(voiceChannelOverlayChannelId) {
            voiceChannelOverlay = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = RealtimeSocket.disconnectionState != DisconnectionState.Connected
        ) {
            DisconnectedNotice(
                state = RealtimeSocket.disconnectionState,
                onReconnect = {
                    RealtimeSocket.updateDisconnectionState(DisconnectionState.Reconnecting)
                    scope.launch { RevoltAPI.connectWS() }
                }
            )
        }

        AnimatedVisibility(
            visible = RealtimeSocket.disconnectionState == DisconnectionState.Connected
        ) {
            Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars))
        }

        if (useTabletAwareUI) {
            Row {
                DismissibleDrawerSheet(
                    drawerContainerColor = Color.Transparent,
                    windowInsets = WindowInsets.zero
                ) {
                    Sidebar(
                        viewModel = viewModel,
                        topNav = topNav,
                        currentServer = currentServer,
                        onShowStatusSheet = {
                            showStatusSheet = true
                        },
                        onShowServerContextSheet = {
                            serverContextSheetTarget = it
                            showServerContextSheet = true
                        },
                        onShowAddServerSheet = {
                            showAddServerSheet = true
                        },
                        showSettingsButton = isTouchExplorationEnabled,
                        onOpenSettings = {
                            topNav.navigate("settings")
                        },
                    )
                }
                ChannelNavigator(
                    dest = viewModel.currentDestination,
                    topNav = topNav,
                    useDrawer = false,
                    toggleDrawer = {
                        toggleDrawerLambda()
                    }
                )
            }
        } else {
            DismissibleNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    DismissibleDrawerSheet(
                        drawerContainerColor = Color.Transparent,
                        windowInsets = WindowInsets.zero
                    ) {
                        Sidebar(
                            viewModel = viewModel,
                            topNav = topNav,
                            currentServer = currentServer,
                            onShowStatusSheet = {
                                showStatusSheet = true
                            },
                            onShowServerContextSheet = {
                                serverContextSheetTarget = it
                                showServerContextSheet = true
                            },
                            onShowAddServerSheet = {
                                showAddServerSheet = true
                            },
                            showSettingsButton = isTouchExplorationEnabled,
                            onOpenSettings = {
                                topNav.navigate("settings")
                            },
                            drawerState = drawerState
                        )
                    }
                },
                content = {
                    Row(Modifier.fillMaxSize()) {
                        ChannelNavigator(
                            dest = viewModel.currentDestination,
                            topNav = topNav,
                            useDrawer = true,
                            toggleDrawer = {
                                toggleDrawerLambda()
                            },
                            drawerState = drawerState
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun Sidebar(
    viewModel: ChatRouterViewModel,
    currentServer: String?,
    topNav: NavController,
    drawerState: DrawerState? = null,
    onShowStatusSheet: () -> Unit,
    onShowServerContextSheet: (String) -> Unit,
    onShowAddServerSheet: () -> Unit,
    showSettingsButton: Boolean,
    onOpenSettings: () -> Unit,
) {
    ChannelSideDrawer(
        onDestinationChanged = viewModel::setSaveDestination,
        currentDestination = viewModel.currentDestination,
        currentServer = currentServer,
        drawerState = drawerState,
        navigateToServer = viewModel::navigateToServer,
        onLongPressAvatar = onShowStatusSheet,
        onShowServerContextSheet = onShowServerContextSheet,
        showSettingsIcon = showSettingsButton,
        onOpenSettings = onOpenSettings,
        topNav = topNav,
        onShowAddServerSheet = onShowAddServerSheet
    )
}

@Composable
fun ChannelNavigator(
    dest: ChatRouterDestination,
    topNav: NavController,
    useDrawer: Boolean,
    toggleDrawer: () -> Unit,
    drawerState: DrawerState? = null
) {
    val scope = rememberCoroutineScope()

    BackHandler(enabled = useDrawer) {
        toggleDrawer()
    }

    Column(Modifier.fillMaxSize()) {
        when (dest) {
            is ChatRouterDestination.Home -> {
                HomeScreen(
                    navController = topNav,
                    useDrawer = useDrawer,
                    onDrawerClicked = toggleDrawer,
                )
            }

            is ChatRouterDestination.Friends -> {
                FriendsScreen(
                    topNav = topNav,
                    useDrawer = useDrawer,
                    onDrawerClicked = toggleDrawer,
                )
            }

            is ChatRouterDestination.Channel -> {
                ChannelScreen(
                    channelId = dest.channelId,
                    onToggleDrawer = {
                        scope.launch {
                            if (drawerState?.isOpen == true) {
                                drawerState.close()
                            } else {
                                drawerState?.open()
                            }
                        }
                    },
                    useDrawer = useDrawer
                )
            }

            is ChatRouterDestination.NoCurrentChannel -> {
                NoCurrentChannelScreen(useDrawer = useDrawer, onDrawerClicked = toggleDrawer)
            }
        }
    }
}
