package chat.revolt.screens.chat.views

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.RevoltTweenDp
import chat.revolt.RevoltTweenFloat
import chat.revolt.RevoltTweenInt
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ULID
import chat.revolt.api.realtime.RealtimeSocket
import chat.revolt.api.realtime.frames.receivable.ChannelStartTypingFrame
import chat.revolt.api.realtime.frames.receivable.ChannelStopTypingFrame
import chat.revolt.api.realtime.frames.receivable.MessageFrame
import chat.revolt.api.routes.channel.fetchMessagesFromChannel
import chat.revolt.api.routes.channel.sendMessage
import chat.revolt.api.routes.microservices.autumn.FileArgs
import chat.revolt.api.routes.microservices.autumn.MAX_ATTACHMENTS_PER_MESSAGE
import chat.revolt.api.routes.microservices.autumn.uploadToAutumn
import chat.revolt.api.routes.user.addUserIfUnknown
import chat.revolt.api.schemas.Channel
import chat.revolt.components.chat.Message
import chat.revolt.components.chat.MessageField
import chat.revolt.components.screens.chat.AttachmentManager
import chat.revolt.components.screens.chat.ChannelIcon
import chat.revolt.components.screens.chat.TypingIndicator
import io.ktor.http.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.io.File
import chat.revolt.api.schemas.Message as MessageSchema

class ChannelScreenViewModel : ViewModel() {
    private var _channel by mutableStateOf<Channel?>(null)
    val channel: Channel?
        get() = _channel

    private var _callback = mutableStateOf<RealtimeSocket.ChannelCallback?>(null)
    private val callback: RealtimeSocket.ChannelCallback?
        get() = _callback.value

    private var _renderableMessages = mutableStateListOf<MessageSchema>()
    val renderableMessages: List<MessageSchema>
        get() = _renderableMessages

    private fun setRenderableMessages(messages: List<MessageSchema>) {
        _renderableMessages.clear()
        _renderableMessages.addAll(messages)
    }

    private var _typingUsers = mutableStateListOf<String>()
    val typingUsers: List<String>
        get() = _typingUsers

    private var _messageContent by mutableStateOf("")
    val messageContent: String
        get() = _messageContent

    fun setMessageContent(content: String) {
        _messageContent = content
    }

    private var _attachments = mutableStateListOf<FileArgs>()
    val attachments: List<FileArgs>
        get() = _attachments

    private fun setAttachments(attachments: List<FileArgs>) {
        _attachments.clear()
        _attachments.addAll(attachments)
    }

    fun addAttachment(fileArgs: FileArgs) {
        _attachments.add(fileArgs)
    }

    fun removeAttachment(fileArgs: FileArgs) {
        _attachments.remove(fileArgs)
    }

    private fun popAttachmentBatch() {
        setAttachments(_attachments.drop(MAX_ATTACHMENTS_PER_MESSAGE))
    }

    private var _sendingMessage by mutableStateOf(false)
    val sendingMessage: Boolean
        get() = _sendingMessage

    private fun setSendingMessage(sending: Boolean) {
        _sendingMessage = sending
    }

    inner class ChannelScreenCallback : RealtimeSocket.ChannelCallback {
        override fun onMessage(message: MessageFrame) {
            viewModelScope.launch {
                addUserIfUnknown(message.author!!)
            }

            regroupMessages(listOf(message) + renderableMessages)
        }

        override fun onStartTyping(typing: ChannelStartTypingFrame) {
            viewModelScope.launch {
                addUserIfUnknown(typing.user)
            }

            if (!_typingUsers.contains(typing.user)) {
                _typingUsers.add(typing.user)
            }
        }

        override fun onStopTyping(typing: ChannelStopTypingFrame) {
            if (_typingUsers.contains(typing.user)) {
                _typingUsers.remove(typing.user)
            }
        }

        override fun onStateInvalidate() {
            fetchMessages()
            _typingUsers.clear()
        }
    }

    private fun registerCallback() {
        _callback.value = ChannelScreenCallback()
        RealtimeSocket.registerChannelCallback(channel!!.id!!, callback!!)
    }

    fun fetchMessages() {
        if (channel == null) {
            return
        }

        _renderableMessages.clear()

        viewModelScope.launch {
            val messages = arrayListOf<MessageSchema>()
            fetchMessagesFromChannel(channel!!.id!!, limit = 50, false).let {
                it.messages!!.forEach { message ->
                    addUserIfUnknown(message.author ?: return@forEach)
                    if (!RevoltAPI.messageCache.containsKey(message.id)) {
                        RevoltAPI.messageCache[message.id!!] = message
                    }
                    messages.add(message)
                }
            }
            regroupMessages(renderableMessages + messages)
        }
    }

    fun fetchOlderMessages() {
        if (channel == null) {
            return
        }

        viewModelScope.launch {
            val messages = arrayListOf<MessageSchema>()

            if (renderableMessages.isNotEmpty()) {
                fetchMessagesFromChannel(
                    channel!!.id!!,
                    limit = 50,
                    true,
                    before = renderableMessages.last().id
                ).let {
                    it.messages!!.forEach { message ->
                        addUserIfUnknown(message.author ?: return@forEach)
                        if (!RevoltAPI.messageCache.containsKey(message.id)) {
                            RevoltAPI.messageCache[message.id!!] = message
                        }
                        messages.add(message)
                    }
                }
            } else {
                fetchMessagesFromChannel(channel!!.id!!, limit = 50, true).let {
                    it.messages!!.forEach { message ->
                        addUserIfUnknown(message.author ?: return@forEach)
                        if (!RevoltAPI.messageCache.containsKey(message.id)) {
                            RevoltAPI.messageCache[message.id!!] = message
                        }
                        messages.add(message)
                    }
                }
            }

            regroupMessages(renderableMessages + messages)
        }
    }

    fun fetchChannel(id: String) {
        if (id in RevoltAPI.channelCache) {
            _channel = RevoltAPI.channelCache[id]
        } else {
            Log.e("ChannelScreen", "Channel $id not in cache, for now this is fatal!") // FIXME
        }

        registerCallback()
    }

    fun sendPendingMessage() {
        setSendingMessage(true)

        viewModelScope.launch {
            val attachmentIds = arrayListOf<String>()

            attachments.take(MAX_ATTACHMENTS_PER_MESSAGE).forEach {
                try {
                    val id = uploadToAutumn(
                        it.file,
                        it.filename,
                        "attachments",
                        ContentType.parse(it.contentType)
                    )
                    Log.d("ChannelScreen", "Uploaded attachment with id $id")
                    attachmentIds.add(id)
                } catch (e: Exception) {
                    Log.e("ChannelScreen", "Failed to upload attachment", e)
                    return@launch
                }
            }

            sendMessage(
                channel!!.id!!,
                messageContent,
                attachments = if (attachmentIds.isEmpty()) null else attachmentIds
            )

            _messageContent = ""
            popAttachmentBatch()
            setSendingMessage(false)
        }
    }

    private fun regroupMessages(newMessages: List<MessageSchema> = renderableMessages) {
        val groupedMessages = arrayListOf<MessageSchema>()

        // Verbatim implementation of https://wiki.rvlt.gg/index.php/Text_Channel_(UI)#Message_Grouping_Algorithm
        // The exception is the date variable being pushed into cache, we don't need that here.
        // Keep in mind: Recomposing UI is incredibly cheap in Jetpack Compose.
        newMessages.forEach { message ->
            var tail = true

            val next = newMessages.getOrNull(newMessages.indexOf(message) + 1)
            if (next != null) {
                val dateA = Instant.fromEpochMilliseconds(ULID.asTimestamp(message.id!!))
                val dateB = Instant.fromEpochMilliseconds(ULID.asTimestamp(next.id!!))

                val minuteDifference = (dateA - dateB).inWholeMinutes

                if (
                    message.author != next.author ||
                    minuteDifference >= 7 ||
                    message.masquerade != next.masquerade ||
                    message.system != null || next.system != null ||
                    message.replies != null
                ) {
                    tail = false
                }
            } else {
                tail = false
            }

            groupedMessages.add(message.copy(tail = tail))
        }

        setRenderableMessages(groupedMessages)
    }
}

@Composable
fun ChannelScreen(
    navController: NavController,
    channelId: String,
    viewModel: ChannelScreenViewModel = viewModel()
) {
    val channel = viewModel.channel

    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uriList ->
        uriList.let { uris ->
            uris.forEach {
                DocumentFile.fromSingleUri(context, it)?.let docfile@{ file ->
                    val mFile = File(context.cacheDir, file.name ?: "attachment")

                    mFile.outputStream().use { output ->
                        @Suppress("Recycle")
                        context.contentResolver.openInputStream(it)?.copyTo(output)
                    }

                    viewModel.addAttachment(
                        FileArgs(
                            file = mFile,
                            contentType = file.type ?: "application/octet-stream",
                            filename = file.name ?: "attachment"
                        )
                    )
                }
            }
        }
    }

    val scrollDownFABPadding by animateDpAsState(
        if (viewModel.typingUsers.isNotEmpty()) 32.dp else 0.dp,
        animationSpec = RevoltTweenDp
    )

    LaunchedEffect(channelId) {
        viewModel.fetchChannel(channelId)
    }

    DisposableEffect(channelId) {
        onDispose {
            RealtimeSocket.unregisterChannelCallback(channelId)
        }
    }

    if (channel == null) {
        CircularProgressIndicator()
        return
    }

    Column {
        Row(
            modifier = Modifier
                .clickable {
                    navController.navigate("channel/${channel.id}/info")
                }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelIcon(
                channelType = channel.channelType!!,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = channel.name ?: channel.id!!,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }

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
                            viewModel.fetchOlderMessages()
                        }
                    }
                }
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.BottomEnd
        ) {
            LazyColumn(state = lazyListState, reverseLayout = true) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                items(viewModel.renderableMessages) { message ->
                    Message(message)
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(16.dp)
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                !isScrolledToBottom.value,
                enter = slideInHorizontally(
                    animationSpec = RevoltTweenInt,
                    initialOffsetX = { it },
                ) + fadeIn(animationSpec = RevoltTweenFloat),
                exit = slideOutHorizontally(
                    animationSpec = RevoltTweenInt,
                    targetOffsetX = { it },
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

        AnimatedVisibility(visible = viewModel.attachments.isNotEmpty()) {
            AttachmentManager(
                attachments = viewModel.attachments,
                uploading = viewModel.sendingMessage,
                onRemove = viewModel::removeAttachment
            )
        }

        MessageField(
            messageContent = viewModel.messageContent,
            onMessageContentChange = viewModel::setMessageContent,
            onSendMessage = viewModel::sendPendingMessage,
            onAddAttachment = {
                pickFileLauncher.launch(arrayOf("*/*"))
            },
            channelType = channel.channelType!!,
            channelName = channel.name ?: channel.id!!,
            forceSendButton = viewModel.attachments.isNotEmpty(),
            disabled = viewModel.attachments.isNotEmpty() && viewModel.sendingMessage
        )
    }
}
