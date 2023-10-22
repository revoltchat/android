package chat.revolt.screens.chat.views.channel

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.activities.RevoltTweenDp
import chat.revolt.activities.RevoltTweenFloat
import chat.revolt.activities.RevoltTweenInt
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ChannelUtils
import chat.revolt.api.routes.microservices.autumn.FileArgs
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.ChannelType
import chat.revolt.components.chat.Message
import chat.revolt.components.chat.NativeMessageField
import chat.revolt.components.chat.SystemMessage
import chat.revolt.components.emoji.EmojiPicker
import chat.revolt.components.media.InbuiltMediaPicker
import chat.revolt.components.screens.chat.AttachmentManager
import chat.revolt.components.screens.chat.ChannelHeader
import chat.revolt.components.screens.chat.ReplyManager
import chat.revolt.components.screens.chat.TypingIndicator
import chat.revolt.internals.markdown.MarkdownContext
import chat.revolt.internals.markdown.MarkdownParser
import chat.revolt.internals.markdown.MarkdownState
import chat.revolt.internals.markdown.addRevoltRules
import chat.revolt.internals.markdown.createCodeRule
import chat.revolt.internals.markdown.createInlineCodeRule
import chat.revolt.sheets.ChannelInfoSheet
import chat.revolt.sheets.MessageContextSheet
import com.discord.simpleast.core.simple.SimpleMarkdownRules
import com.discord.simpleast.core.simple.SimpleRenderer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    navController: NavController,
    channelId: String,
    onToggleDrawer: () -> Unit,
    onUserSheetOpenFor: (String, String?) -> Unit,
    useDrawer: Boolean,
    viewModel: ChannelScreenViewModel = viewModel()
) {
    val channel = viewModel.activeChannel

    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val codeBlockColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)

    var channelInfoSheetShown by remember { mutableStateOf(false) }

    var messageContextSheetShown by remember { mutableStateOf(false) }
    var messageContextSheetTarget by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    fun processFileUri(uri: Uri, pickerIdentifier: String? = null) {
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
                viewModel.pendingAttachments.any { it.pickerIdentifier == pickerIdentifier }
            ) {
                viewModel.pendingAttachments.removeIf { it.pickerIdentifier == pickerIdentifier }
                return
            }

            viewModel.pendingAttachments.add(
                FileArgs(
                    file = mFile,
                    contentType = file.type ?: "application/octet-stream",
                    filename = file.name ?: "attachment",
                    pickerIdentifier = pickerIdentifier
                )
            )
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uriList ->
        uriList.let { list ->
            list.forEach { uri ->
                processFileUri(uri)
            }
        }
    }

    val capturedPhotoUri = rememberSaveable { mutableStateOf<Uri?>(null) }
    val pickCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { uriUpdated ->
        if (uriUpdated) {
            capturedPhotoUri.value?.let { uri ->
                processFileUri(uri)
            }
        }
    }

    val scrollDownFABPadding by animateDpAsState(
        if (viewModel.typingUsers.isNotEmpty()) 25.dp else 0.dp,
        animationSpec = RevoltTweenDp,
        label = "ScrollDownFABPadding"
    )

    val fieldContent = remember(viewModel.pendingMessageContent, viewModel.textSelection) {
        TextFieldValue(
            viewModel.pendingMessageContent,
            viewModel.textSelection
        )
    }

    LaunchedEffect(channelId) {
        viewModel.activeChannelId = channelId
        viewModel.fetchChannel(channelId)

        coroutineScope.launch {
            viewModel.listenForWsFrames()
        }

        coroutineScope.launch {
            viewModel.listenForUiCallbacks()
        }
    }

    if (channelInfoSheetShown) {
        val channelInfoSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = channelInfoSheetState,
            onDismissRequest = {
                channelInfoSheetShown = false
            }
        ) {
            ChannelInfoSheet(
                channelId = channelId
            )
        }
    }

    if (messageContextSheetShown) {
        val messageContextSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = messageContextSheetState,
            onDismissRequest = {
                messageContextSheetShown = false
            }
        ) {
            MessageContextSheet(
                messageId = messageContextSheetTarget,
                onHideSheet = {
                    messageContextSheetState.hide()
                    messageContextSheetShown = false
                },
                onReportMessage = {
                    navController.navigate("report/message/$messageContextSheetTarget")
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .imePadding()
            .safeDrawingPadding()
    ) {
        ChannelHeader(
            channel = channel ?: Channel(
                id = channelId,
                name = stringResource(R.string.loading),
                channelType = ChannelType.TextChannel
            ),
            onChannelClick = {
                channelInfoSheetShown = true
            },
            onToggleDrawer = onToggleDrawer,
            useDrawer = useDrawer
        )

        val isScrolledToBottom = remember(lazyListState) {
            derivedStateOf {
                lazyListState.firstVisibleItemIndex <= 6
            }
        }

        val isScrolledToTop = remember {
            derivedStateOf {
                val layoutInfo = lazyListState.layoutInfo
                val totalItemsNumber = layoutInfo.totalItemsCount
                val lastVisibleItemIndex =
                    (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
                val buffer = 6

                lastVisibleItemIndex > (totalItemsNumber - buffer)
            }
        }

        LaunchedEffect(isScrolledToTop) {
            snapshotFlow { isScrolledToTop.value }
                .distinctUntilChanged()
                .collect {
                    if (it) {
                        coroutineScope.launch {
                            if (viewModel.hasNoMoreMessages) return@launch
                            viewModel.fetchOlderMessages()
                        }
                    }
                }
        }

        LaunchedEffect(viewModel.activeChannel, RevoltAPI.channelCache, RevoltAPI.serverCache) {
            viewModel.checkShouldDenyMessageField()
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.BottomEnd
        ) {
            LazyColumn(state = lazyListState, reverseLayout = true) {
                item {
                    Spacer(modifier = Modifier.height(25.dp))
                }

                items(
                    items = viewModel.renderableMessages,
                    key = { it.id!! }
                ) { message ->
                    when {
                        message.system != null -> SystemMessage(message)
                        else -> Message(
                            message,
                            parse = {
                                val parser = MarkdownParser()
                                    .addRules(
                                        SimpleMarkdownRules.createEscapeRule()
                                    )
                                    .addRevoltRules(context)
                                    .addRules(
                                        createCodeRule(context, codeBlockColor.toArgb()),
                                        createInlineCodeRule(context, codeBlockColor.toArgb())
                                    )
                                    .addRules(
                                        SimpleMarkdownRules.createSimpleMarkdownRules(
                                            includeEscapeRule = false
                                        )
                                    )

                                SimpleRenderer.render(
                                    source = it.content ?: "",
                                    parser = parser,
                                    initialState = MarkdownState(0),
                                    renderContext = MarkdownContext(
                                        memberMap = viewModel.activeChannel?.server?.let { serverId ->
                                            RevoltAPI.members.markdownMemberMapFor(
                                                serverId
                                            )
                                        } ?: mapOf(),
                                        userMap = RevoltAPI.userCache.toMap(),
                                        channelMap = RevoltAPI.channelCache.mapValues { ch ->
                                            ch.value.name ?: ch.value.id ?: "#DeletedChannel"
                                        },
                                        emojiMap = RevoltAPI.emojiCache,
                                        serverId = channel?.server ?: "",
                                        // check if message consists solely of one *or more* custom emotes
                                        useLargeEmojis = it.content?.matches(
                                            Regex("(:([0-9A-Z]{26}):)+")
                                        ) == true
                                    )
                                )
                            },
                            onMessageContextMenu = {
                                messageContextSheetShown = true
                                messageContextSheetTarget = message.id ?: ""
                            },
                            onAvatarClick = {
                                message.author?.let { author ->
                                    onUserSheetOpenFor(author, channel?.server)
                                }
                            },
                            canReply = true,
                            onReply = {
                                if (viewModel.pendingReplies.size >= 5) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.too_many_replies, 5),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Message
                                }
                                viewModel.replyToMessage(message)
                            }
                        )
                    }
                }

                item {
                    if (viewModel.hasNoMoreMessages) {
                        Text(
                            text = stringResource(R.string.start_of_conversation),
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp, top = 64.dp, bottom = 32.dp)
                                .fillMaxWidth(),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    } else {
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

            androidx.compose.animation.AnimatedVisibility(
                !isScrolledToBottom.value,
                enter = slideInHorizontally(
                    animationSpec = RevoltTweenInt,
                    initialOffsetX = { it }
                ) + fadeIn(animationSpec = RevoltTweenFloat),
                exit = slideOutHorizontally(
                    animationSpec = RevoltTweenInt,
                    targetOffsetX = { it }
                ) + fadeOut(animationSpec = RevoltTweenFloat),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
            ) {
                ExtendedFloatingActionButton(
                    modifier = Modifier
                        .padding(bottom = scrollDownFABPadding)
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    text = {
                        Text(stringResource(R.string.scroll_to_bottom))
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.scroll_to_bottom)
                        )
                    },
                    onClick = {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    },
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }

            TypingIndicator(
                users = viewModel.typingUsers
            )
        }

        Column(
            modifier = Modifier
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                    top = 8.dp
                )
                .clip(MaterialTheme.shapes.medium)
        ) {
            AnimatedVisibility(visible = viewModel.pendingReplies.isNotEmpty()) {
                ReplyManager(
                    replies = viewModel.pendingReplies,
                    onRemove = { viewModel.pendingReplies.remove(it) },
                    onToggleMention = viewModel::toggleReplyMentionFor
                )
            }

            AnimatedVisibility(visible = viewModel.pendingAttachments.isNotEmpty()) {
                AttachmentManager(
                    attachments = viewModel.pendingAttachments,
                    uploading = viewModel.isSendingMessage,
                    uploadProgress = viewModel.pendingUploadProgress,
                    onRemove = { viewModel.pendingAttachments.remove(it) }
                )
            }

            Crossfade(
                viewModel.denyMessageField,
                label = "denyMessageField switch"
            ) { denyMessageField ->
                if (denyMessageField) {
                    Text(
                        text = stringResource(id = viewModel.denyMessageFieldReasonResource),
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                } else {
                    NativeMessageField(
                        value = fieldContent.text,
                        onValueChange = {
                            viewModel.pendingMessageContent = it
                            // viewModel.textSelection = it.selection
                        },
                        onSendMessage = viewModel::sendPendingMessage,
                        onAddAttachment = {
                            val isTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

                            when {
                                isTiramisu -> {
                                    focusManager.clearFocus()
                                    if (viewModel.currentBottomPane == BottomPane.InbuiltMediaPicker) {
                                        viewModel.currentBottomPane = BottomPane.None
                                    } else {
                                        viewModel.currentBottomPane = BottomPane.InbuiltMediaPicker
                                    }
                                }

                                !isTiramisu -> {
                                    pickFileLauncher.launch(arrayOf("*/*"))
                                }
                            }
                        },
                        onCommitAttachment = { uri ->
                            processFileUri(uri)
                        },
                        onPickEmoji = {
                            focusManager.clearFocus()
                            if (viewModel.currentBottomPane == BottomPane.EmojiPicker) {
                                viewModel.currentBottomPane = BottomPane.None
                            } else {
                                viewModel.currentBottomPane = BottomPane.EmojiPicker
                            }
                        },
                        channelType = channel?.channelType ?: ChannelType.TextChannel,
                        channelName = channel?.name
                            ?: channel?.let { ChannelUtils.resolveDMName(it) }
                            ?: stringResource(
                                R.string.unknown
                            ),
                        forceSendButton = viewModel.pendingAttachments.isNotEmpty(),
                        disabled = viewModel.pendingAttachments.isNotEmpty() && viewModel.isSendingMessage,
                        editMode = viewModel.editingMessage != null,
                        cancelEdit = viewModel::cancelEditingMessage,
                        onFocusChange = { nowFocused ->
                            if (nowFocused && viewModel.currentBottomPane != BottomPane.None) {
                                viewModel.currentBottomPane = BottomPane.None
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = viewModel.currentBottomPane == BottomPane.InbuiltMediaPicker
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    InbuiltMediaPicker(
                        onOpenDocumentsUi = {
                            pickFileLauncher.launch(arrayOf("*/*"))
                            viewModel.currentBottomPane = BottomPane.None
                        },
                        onOpenCamera = {
                            // Create a new content URI to store the captured image.
                            val contentResolver = context.contentResolver
                            val contentValues = ContentValues().apply {
                                put(
                                    MediaStore.MediaColumns.DISPLAY_NAME,
                                    "RVL_${System.currentTimeMillis()}.jpg"
                                )
                                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                put(
                                    MediaStore.MediaColumns.RELATIVE_PATH,
                                    Environment.DIRECTORY_PICTURES
                                )
                            }

                            capturedPhotoUri.value = contentResolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                            )

                            pickCameraLauncher.launch(capturedPhotoUri.value)
                            viewModel.currentBottomPane = BottomPane.None
                        },
                        onClose = {
                            viewModel.currentBottomPane = BottomPane.None
                        },
                        onMediaSelected = { media ->
                            try {
                                processFileUri(
                                    media.uri,
                                    pickerIdentifier = media.uri.lastPathSegment
                                )
                            } catch (e: Exception) {
                                if (e is FileNotFoundException) {
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            R.string.file_picker_cannot_attach_file_invalid
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        pendingMedia = viewModel.pendingAttachments
                            .filterNot { it.pickerIdentifier == null }
                            .map { it.pickerIdentifier!! },
                        disabled = viewModel.isSendingMessage
                    )
                }
            }

            AnimatedVisibility(visible = viewModel.currentBottomPane == BottomPane.EmojiPicker) {
                BackHandler(enabled = viewModel.currentBottomPane == BottomPane.EmojiPicker) {
                    viewModel.currentBottomPane = BottomPane.None
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .padding(4.dp)
                ) {
                    EmojiPicker(onEmojiSelected = viewModel::putAtCursorPosition)
                }
            }
        }
    }
}
