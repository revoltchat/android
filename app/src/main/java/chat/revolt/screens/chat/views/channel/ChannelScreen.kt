package chat.revolt.screens.chat.views.channel

import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import chat.revolt.R
import chat.revolt.RevoltApplication
import chat.revolt.activities.RevoltTweenDp
import chat.revolt.activities.RevoltTweenFloat
import chat.revolt.activities.RevoltTweenInt
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ChannelUtils
import chat.revolt.api.internals.PermissionBit
import chat.revolt.api.internals.has
import chat.revolt.api.routes.channel.react
import chat.revolt.api.routes.microservices.autumn.FileArgs
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.schemas.Message
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.components.chat.DateDivider
import chat.revolt.components.chat.Message
import chat.revolt.components.chat.NativeMessageField
import chat.revolt.components.chat.SystemMessage
import chat.revolt.components.emoji.EmojiPicker
import chat.revolt.components.generic.GroupIcon
import chat.revolt.components.generic.PresenceBadge
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.UserAvatarWidthPlaceholder
import chat.revolt.components.generic.presenceFromStatus
import chat.revolt.components.media.MediaPickerGateway
import chat.revolt.components.screens.chat.AttachmentManager
import chat.revolt.components.screens.chat.ChannelIcon
import chat.revolt.components.screens.chat.ReplyManager
import chat.revolt.components.screens.chat.TypingIndicator
import chat.revolt.internals.extensions.BottomSheetInsets
import chat.revolt.internals.extensions.rememberChannelPermissions
import chat.revolt.internals.extensions.zero
import chat.revolt.sheets.ChannelInfoSheet
import chat.revolt.sheets.MessageContextSheet
import chat.revolt.sheets.ReactSheet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.io.File
import kotlin.math.max

sealed class ChannelScreenItem {
    data class RegularMessage(val message: Message) : ChannelScreenItem()
    data class ProspectiveMessage(val message: Message) : ChannelScreenItem()
    data class FailedMessage(val message: Message) : ChannelScreenItem()
    data class SystemMessage(val message: Message) : ChannelScreenItem()
    data class DateDivider(val instant: Instant) : ChannelScreenItem()
    data class LoadTrigger(val after: String?, val before: String?) :
        ChannelScreenItem()

    data object Loading : ChannelScreenItem()
}

sealed class ChannelScreenActivePane {
    data object None : ChannelScreenActivePane()
    data object EmojiPicker : ChannelScreenActivePane()
    data object AttachmentPicker : ChannelScreenActivePane()
}

private fun pxAsDp(px: Int): Dp {
    return (
            px / (
                    RevoltApplication.instance.resources
                        .displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
                    )
            ).dp
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun ChannelScreen(
    channelId: String,
    onToggleDrawer: () -> Unit,
    useDrawer: Boolean,
    viewModel: ChannelScreenViewModel = hiltViewModel()
) {
    // Setup

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.startListening(createUiCallbackListener = true)
    }

    // Load/switch channel

    val channelPermissions by rememberChannelPermissions(channelId, viewModel.ensuredSelfMember)

    LaunchedEffect(channelId) {
        viewModel.switchChannel(channelId)
    }

    // Keyboard height

    val imeTarget = WindowInsets.imeAnimationTarget.getBottom(LocalDensity.current)
    val navigationBarsInset = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    val imeCurrentInset = WindowInsets.ime.getBottom(LocalDensity.current)
    var imeInTransition by remember { mutableStateOf(false) }

    var emojiSearchFocused by remember { mutableStateOf(false) }

    val fallbackKeyboardHeight by animateIntAsState(
        targetValue = if (viewModel.activePane == ChannelScreenActivePane.None && !imeInTransition) navigationBarsInset else viewModel.keyboardHeight,
        label = "keyboardHeight"
    )

    LaunchedEffect(imeTarget) {
        if (imeTarget > 0) {
            viewModel.updateSaveKeyboardHeight(imeTarget)
        } else {
            imeInTransition = false
        }
    }

    // Attachment handling

    val processFileUri: (Uri, String?) -> Unit = remember {
        { uri, pickerIdentifier ->
            DocumentFile.fromSingleUri(context, uri)?.let { file ->
                val mFile = File(context.cacheDir, file.name ?: "attachment")

                mFile.outputStream().use { output ->
                    @Suppress("Recycle")
                    context.contentResolver.openInputStream(uri)?.copyTo(output)
                }

                // If the file is already pending and was picked from the inbuilt picker, remove it.
                // This is so you can "toggle" the file in the picker.
                // If the file was picked via DocumentsUI we don't want toggling functionality as
                // if you specifically opened it from DocumentsUI you probably want to send it anyway.
                if (
                    pickerIdentifier != null &&
                    viewModel.draftAttachments.any { it.pickerIdentifier == pickerIdentifier }
                ) {
                    viewModel.draftAttachments.removeIf { it.pickerIdentifier == pickerIdentifier }
                    return@let
                }

                viewModel.draftAttachments.add(
                    FileArgs(
                        file = mFile,
                        contentType = file.type ?: "application/octet-stream",
                        filename = file.name ?: "attachment",
                        pickerIdentifier = pickerIdentifier
                    )
                )
            }
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uriList ->
        uriList.let { list ->
            list.forEach { uri ->
                processFileUri(uri, null)
            }
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) {
        it.let { list ->
            list.forEach { uri ->
                processFileUri(uri, null)
            }
        }
    }

    val capturedPhotoUri = rememberSaveable { mutableStateOf<Uri?>(null) }
    val pickCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { uriUpdated ->
        if (uriUpdated) {
            capturedPhotoUri.value?.let { uri ->
                processFileUri(uri, null)
            }
        }
    }

    // UI elements

    val lazyListState = rememberLazyListState()

    val isScrolledToBottom = remember(lazyListState) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex <= 6
        }
    }

    val isNearTop = remember(lazyListState) {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex =
                (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            val buffer = 6

            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    val scrollDownFABPadding by animateDpAsState(
        if (viewModel.typingUsers.isNotEmpty()) 25.dp else 0.dp,
        animationSpec = RevoltTweenDp,
        label = "ScrollDownFABPadding"
    )

    // Load more messages when we reach the top of the list
    // TODO: Temp - use LoadTrigger instead

    LaunchedEffect(isNearTop) {
        snapshotFlow { isNearTop.value }
            .distinctUntilChanged()
            .collect { isNearTop ->
                if (isNearTop) {
                    Log.d("ChannelScreen", "Loading more messages")
                    viewModel.loadMessages(before = viewModel.items.lastOrNull {
                        it is ChannelScreenItem.RegularMessage || it is ChannelScreenItem.SystemMessage
                    }?.let {
                        when (it) {
                            is ChannelScreenItem.RegularMessage -> it.message.id
                            is ChannelScreenItem.SystemMessage -> it.message.id
                            else -> null
                        }
                    }, amount = 50)
                }
            }
    }

    // Sheets

    var channelInfoSheetShown by remember { mutableStateOf(false) }
    if (channelInfoSheetShown) {
        val channelInfoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = channelInfoSheetState,
            onDismissRequest = {
                channelInfoSheetShown = false
            },
            windowInsets = BottomSheetInsets
        ) {
            ChannelInfoSheet(
                channelId = channelId,
                onHideSheet = {
                    channelInfoSheetState.hide()
                    channelInfoSheetShown = false
                }
            )
        }
    }

    var messageContextSheetShown by remember { mutableStateOf(false) }
    var messageContextSheetTarget by remember { mutableStateOf("") }
    if (messageContextSheetShown) {
        val messageContextSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = messageContextSheetState,
            onDismissRequest = {
                messageContextSheetShown = false
            },
            windowInsets = BottomSheetInsets
        ) {
            MessageContextSheet(
                messageId = messageContextSheetTarget,
                onHideSheet = {
                    messageContextSheetState.hide()
                    messageContextSheetShown = false
                },
                onReportMessage = {
                    scope.launch {
                        ActionChannel.send(Action.ReportMessage(messageContextSheetTarget))
                    }
                }
            )
        }
    }

    var reactSheetShown by remember { mutableStateOf(false) }
    var reactSheetTarget by remember { mutableStateOf("") }
    if (reactSheetShown) {
        val reactSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = reactSheetState,
            onDismissRequest = {
                reactSheetShown = false
            },
            windowInsets = BottomSheetInsets
        ) {
            ReactSheet(reactSheetTarget) {
                if (it == null) return@ReactSheet

                scope.launch {
                    react(channelId, reactSheetTarget, it)
                    reactSheetState.hide()
                    reactSheetShown = false
                }
            }
        }
    }

    // Begin UI composition

    Scaffold(
        contentWindowInsets = WindowInsets(
            left = 0,
            right = 0,
            top = 0,
            bottom = 0
        ),
        topBar = {
            TopAppBar(
                modifier = Modifier.clickable {
                    channelInfoSheetShown = true
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.channel?.let {
                            when (it.channelType) {
                                ChannelType.DirectMessage -> {
                                    val partner =
                                        RevoltAPI.userCache[ChannelUtils.resolveDMPartner(it)]
                                    UserAvatar(
                                        username = it.name ?: stringResource(R.string.unknown),
                                        userId = ChannelUtils.resolveDMPartner(it) ?: "",
                                        size = 24.dp,
                                        presenceSize = 12.dp,
                                        avatar = partner?.avatar
                                    )
                                }

                                ChannelType.Group -> {
                                    GroupIcon(
                                        name = it.name ?: stringResource(R.string.unknown),
                                        size = 24.dp,
                                        icon = it.icon
                                    )
                                }

                                else -> {
                                    ChannelIcon(
                                        channelType = it.channelType ?: ChannelType.TextChannel,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .alpha(0.8f)
                                    )
                                }
                            }

                            CompositionLocalProvider(
                                LocalTextStyle provides LocalTextStyle.current.copy(
                                    fontSize = 20.sp,
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Bottom,
                                        trim = LineHeightStyle.Trim.LastLineBottom
                                    )
                                )
                            ) {
                                when (it.channelType) {
                                    ChannelType.TextChannel, ChannelType.VoiceChannel, ChannelType.Group -> Text(
                                        it.name ?: stringResource(R.string.unknown),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    ChannelType.SavedMessages -> Text(
                                        stringResource(R.string.channel_notes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    ChannelType.DirectMessage -> Text(
                                        ChannelUtils.resolveName(it)
                                            ?: stringResource(R.string.unknown),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    else -> Text(
                                        stringResource(R.string.unknown),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (it.channelType == ChannelType.DirectMessage) {
                                val partner =
                                    RevoltAPI.userCache[ChannelUtils.resolveDMPartner(it)]
                                PresenceBadge(
                                    presence = presenceFromStatus(
                                        partner?.status?.presence,
                                        online = partner?.online == true
                                    ),
                                    size = 12.dp
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .alpha(0.5f)
                            )
                        }
                    }
                },
                windowInsets = WindowInsets.zero,
                navigationIcon = {
                    if (useDrawer) {
                        IconButton(onClick = onToggleDrawer) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(id = R.string.menu)
                            )
                        }
                    }
                }
            )
        }
    ) { pv ->
        Crossfade(
            targetState = viewModel.ageGateUnlocked,
            label = "ageGateUnlocked"
        ) { ageGateUnlocked ->
            if (ageGateUnlocked == false) {
                ChannelScreenAgeGate(
                    onAccept = {
                        viewModel.ageGateUnlocked = true
                    },
                    onDeny = {
                        onToggleDrawer()
                    }
                )
            } else if (ageGateUnlocked == null) {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            } else if (ageGateUnlocked == true) {
                Column(
                    modifier = Modifier
                        .padding(pv)
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            reverseLayout = true,
                            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                        ) {

                            // If we don't have a guaranteed first item, the message list will not scroll
                            // to the bottom when new messages are added. Evil hack to make our other evil
                            // hack (clear/addAll) work. Too bad!
                            item(key = "guaranteed_first") {
                                Box {}
                            }

                            items(
                                viewModel.items.size,
                                key = { index ->
                                    when (val item = viewModel.items[index]) {
                                        is ChannelScreenItem.RegularMessage -> item.message.id!!
                                        is ChannelScreenItem.ProspectiveMessage -> item.message.id!!
                                        is ChannelScreenItem.FailedMessage -> item.message.id!!
                                        is ChannelScreenItem.SystemMessage -> item.message.id!!
                                        is ChannelScreenItem.DateDivider -> item.instant.toEpochMilliseconds()
                                        is ChannelScreenItem.LoadTrigger -> index
                                        is ChannelScreenItem.Loading -> index
                                    }
                                },
                                contentType = { index ->
                                    when (viewModel.items.getOrNull(index)) {
                                        null -> null
                                        is ChannelScreenItem.RegularMessage -> "RegularMessage"
                                        is ChannelScreenItem.ProspectiveMessage -> "ProspectiveMessage"
                                        is ChannelScreenItem.FailedMessage -> "FailedMessage"
                                        is ChannelScreenItem.SystemMessage -> "SystemMessage"
                                        is ChannelScreenItem.DateDivider -> "DateDivider"
                                        is ChannelScreenItem.LoadTrigger -> "LoadTrigger"
                                        is ChannelScreenItem.Loading -> "Loading"
                                    }
                                }
                            ) { index ->
                                when (val item = viewModel.items[index]) {
                                    is ChannelScreenItem.RegularMessage -> {
                                        Message(
                                            message = item.message,
                                            onMessageContextMenu = {
                                                item.message.id?.let { messageId ->
                                                    messageContextSheetTarget = messageId
                                                    messageContextSheetShown = true
                                                }
                                            },
                                            onAvatarClick = {
                                                item.message.author?.let { author ->
                                                    scope.launch {
                                                        ActionChannel.send(
                                                            Action.OpenUserSheet(
                                                                author,
                                                                viewModel.channel?.server
                                                            )
                                                        )
                                                    }
                                                }
                                            },
                                            onNameClick = {
                                                val author =
                                                    item.message.author?.let { RevoltAPI.userCache[it] }
                                                        ?: return@Message
                                                viewModel.putAtCursorPosition("@${author.username}#${author.discriminator}")
                                            },
                                            canReply = true,
                                            onReply = {
                                                item.message.id?.let { messageId ->
                                                    scope.launch { viewModel.addReplyTo(messageId) }
                                                }
                                            },
                                            onAddReaction = {
                                                item.message.id?.let { messageId ->
                                                    reactSheetTarget = messageId
                                                    reactSheetShown = true
                                                }
                                            }
                                        )
                                    }

                                    is ChannelScreenItem.ProspectiveMessage -> {
                                        Box(Modifier.alpha(0.5f)) {
                                            Message(
                                                message = item.message,
                                                onMessageContextMenu = {
                                                    // TODO Context menu that allows you to cancel send
                                                },
                                                onAvatarClick = {},
                                                onNameClick = {},
                                                canReply = false,
                                                onReply = {},
                                                onAddReaction = {}
                                            )
                                        }
                                    }

                                    is ChannelScreenItem.FailedMessage -> {
                                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                                            Column {
                                                Message(
                                                    message = item.message,
                                                    onMessageContextMenu = {},
                                                    onAvatarClick = {},
                                                    onNameClick = {},
                                                    canReply = false,
                                                    onReply = {},
                                                    onAddReaction = {}
                                                )
                                                Row {
                                                    UserAvatarWidthPlaceholder()
                                                    Text(
                                                        stringResource(R.string.message_failed_to_send),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.error.copy(
                                                            alpha = 0.8f
                                                        ),
                                                        modifier = Modifier.padding(
                                                            top = 4.dp,
                                                            bottom = 4.dp,
                                                            start = 20.dp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    is ChannelScreenItem.SystemMessage -> {
                                        SystemMessage(message = item.message)
                                    }

                                    is ChannelScreenItem.DateDivider -> {
                                        DateDivider(instant = item.instant)
                                    }

                                    is ChannelScreenItem.LoadTrigger -> {
                                        LaunchedEffect(Unit) {
                                            Log.d(
                                                "ChannelScreen",
                                                "LoadTrigger: After ${item.after} Before ${item.before}"
                                            )
                                        }
                                    }

                                    is ChannelScreenItem.Loading -> {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier
                                                    .padding(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        TypingIndicator(
                            users = viewModel.typingUsers,
                            serverId = viewModel.channel?.server
                        )

                        androidx.compose.animation.AnimatedVisibility(
                            !isScrolledToBottom.value,
                            enter = slideInVertically(
                                animationSpec = RevoltTweenInt,
                                initialOffsetY = { it }
                            ) + fadeIn(animationSpec = RevoltTweenFloat),
                            exit = slideOutVertically(
                                animationSpec = RevoltTweenInt,
                                targetOffsetY = { it }
                            ) + fadeOut(animationSpec = RevoltTweenFloat)
                        ) {
                            SmallFloatingActionButton(
                                modifier = Modifier
                                    .padding(bottom = scrollDownFABPadding)
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                onClick = {
                                    scope.launch {
                                        lazyListState.animateScrollToItem(0)
                                    }
                                },
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_down_24dp),
                                    contentDescription = stringResource(R.string.scroll_to_bottom)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = viewModel.denyMessageField,
                            label = "denyMessageField"
                        ) { deny ->
                            if (!deny) {
                                Column {
                                    AnimatedVisibility(
                                        visible = viewModel.draftReplyTo.isNotEmpty() && !viewModel.denyMessageField
                                    ) {
                                        ReplyManager(
                                            replies = viewModel.draftReplyTo,
                                            onToggleMention = {
                                                scope.launch { viewModel.toggleMentionOnReply(it.id) }
                                            },
                                            onRemove = {
                                                viewModel.draftReplyTo.remove(it)
                                            }
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = viewModel.draftAttachments.isNotEmpty() && !viewModel.denyMessageField
                                    ) {
                                        AttachmentManager(
                                            attachments = viewModel.draftAttachments,
                                            uploading = viewModel.attachmentUploadProgress > 0,
                                            uploadProgress = viewModel.attachmentUploadProgress,
                                            canRemove = true,
                                            canPreview = true,
                                            onRemove = {
                                                viewModel.draftAttachments.remove(it)
                                            }
                                        )
                                    }

                                    AnimatedVisibility(visible = viewModel.editingMessage != null) {
                                        Row(Modifier.padding(start = 24.dp, top = 8.dp)) {
                                            AssistChip(
                                                onClick = {
                                                    viewModel.editingMessage = null
                                                    viewModel.putDraftContent("")
                                                },
                                                label = {
                                                    Text(stringResource(R.string.message_field_editing_message))
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = null
                                                    )
                                                },
                                                trailingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = stringResource(R.string.message_field_editing_message_cancel_alt),
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.alpha(0.8f)
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    NativeMessageField(
                                        value = viewModel.draftContent,
                                        onValueChange = viewModel::putDraftContent,
                                        onAddAttachment = {
                                            if (viewModel.activePane == ChannelScreenActivePane.AttachmentPicker) {
                                                viewModel.activePane = ChannelScreenActivePane.None
                                            } else {
                                                viewModel.activePane =
                                                    ChannelScreenActivePane.AttachmentPicker
                                            }
                                        },
                                        onCommitAttachment = {
                                            processFileUri(it, null)
                                        },
                                        onPickEmoji = {
                                            if (viewModel.activePane == ChannelScreenActivePane.EmojiPicker) {
                                                viewModel.activePane = ChannelScreenActivePane.None
                                            } else {
                                                viewModel.activePane =
                                                    ChannelScreenActivePane.EmojiPicker
                                            }
                                        },
                                        onSendMessage = viewModel::sendPendingMessage,
                                        channelType = viewModel.channel?.channelType
                                            ?: ChannelType.TextChannel,
                                        channelName = viewModel.channel?.let { channel ->
                                            ChannelUtils.resolveName(channel)
                                        }
                                            ?: stringResource(R.string.unknown),
                                        onFocusChange = { isFocused ->
                                            if (isFocused && viewModel.activePane != ChannelScreenActivePane.None) {
                                                viewModel.activePane = ChannelScreenActivePane.None
                                                imeInTransition = true
                                            }
                                        },
                                        forceSendButton = viewModel.draftAttachments.isNotEmpty(),
                                        canAttach = (channelPermissions has PermissionBit.UploadFiles) && viewModel.editingMessage == null,
                                        serverId = viewModel.channel?.server,
                                        channelId = channelId,
                                        failedValidation = viewModel.draftContent.length > 2000,
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 32.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        stringResource(viewModel.denyMessageFieldReasonResource),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (viewModel.activePane == ChannelScreenActivePane.None && !imeInTransition) {
                            Spacer(
                                Modifier
                                    .imePadding()
                                    .navigationBarsPadding()
                                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                            )
                        } else {
                            Box(
                                Modifier
                                    .heightIn(min = pxAsDp(fallbackKeyboardHeight))
                            ) {
                                Box(
                                    Modifier.then(
                                        if (emojiSearchFocused) {
                                            Modifier.requiredHeight(
                                                pxAsDp(
                                                    max(
                                                        imeCurrentInset * 2,
                                                        fallbackKeyboardHeight
                                                    )
                                                )
                                            )
                                        } else {
                                            Modifier.requiredHeight(pxAsDp(fallbackKeyboardHeight))
                                        }
                                    )
                                ) {
                                    when (viewModel.activePane) {
                                        ChannelScreenActivePane.EmojiPicker -> {
                                            BackHandler(enabled = viewModel.activePane == ChannelScreenActivePane.EmojiPicker) {
                                                viewModel.activePane = ChannelScreenActivePane.None
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceColorAtElevation(
                                                            1.dp
                                                        )
                                                    )
                                                    .padding(4.dp)
                                                    .navigationBarsPadding()
                                            ) {
                                                EmojiPicker(
                                                    onEmojiSelected = viewModel::putAtCursorPosition,
                                                    bottomInset = pxAsDp(
                                                        max(
                                                            imeCurrentInset - navigationBarsInset,
                                                            0
                                                        )
                                                    ),
                                                    onSearchFocus = {
                                                        emojiSearchFocused = it
                                                    }
                                                )
                                            }
                                        }

                                        ChannelScreenActivePane.AttachmentPicker -> {
                                            BackHandler(enabled = viewModel.activePane == ChannelScreenActivePane.AttachmentPicker) {
                                                viewModel.activePane = ChannelScreenActivePane.None
                                            }

                                            MediaPickerGateway(
                                                onOpenPhotoPicker = {
                                                    pickMediaLauncher.launch(
                                                        PickVisualMediaRequest(
                                                            mediaType = ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                                        )
                                                    )
                                                    viewModel.activePane =
                                                        ChannelScreenActivePane.None
                                                },
                                                onOpenDocumentPicker = {
                                                    pickFileLauncher.launch(arrayOf("*/*"))
                                                    viewModel.activePane =
                                                        ChannelScreenActivePane.None
                                                },
                                                onOpenCamera = {
                                                    // Create a new content URI to store the captured image.
                                                    val contentResolver =
                                                        context.contentResolver
                                                    val contentValues = ContentValues().apply {
                                                        put(
                                                            MediaStore.MediaColumns.DISPLAY_NAME,
                                                            "RVL_${System.currentTimeMillis()}.jpg"
                                                        )
                                                        put(
                                                            MediaStore.MediaColumns.MIME_TYPE,
                                                            "image/jpeg"
                                                        )
                                                        put(
                                                            MediaStore.MediaColumns.RELATIVE_PATH,
                                                            Environment.DIRECTORY_PICTURES
                                                        )
                                                    }

                                                    try {
                                                        capturedPhotoUri.value =
                                                            contentResolver.insert(
                                                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                                contentValues
                                                            )
                                                    } catch (e: Exception) {
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(
                                                                R.string.file_picker_chip_camera_failed
                                                            ),
                                                            Toast.LENGTH_SHORT
                                                        ).show()

                                                        return@MediaPickerGateway
                                                    }

                                                    try {
                                                        capturedPhotoUri.value?.let { uri ->
                                                            pickCameraLauncher.launch(uri)
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(
                                                                R.string.file_picker_chip_camera_none_installed
                                                            ),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }

                                                    viewModel.activePane =
                                                        ChannelScreenActivePane.None
                                                },
                                            )
                                        }

                                        else -> {
                                            // Do nothing
                                        }
                                    }
                                }
                                Box(Modifier.imePadding())
                            }
                        }
                    }
                }
            }
        }
    }
}