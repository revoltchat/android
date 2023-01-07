package chat.revolt.screens.chat.views

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.api.RevoltAPI
import chat.revolt.api.realtime.RealtimeSocket
import chat.revolt.api.realtime.frames.receivable.ChannelStartTypingFrame
import chat.revolt.api.realtime.frames.receivable.ChannelStopTypingFrame
import chat.revolt.api.realtime.frames.receivable.MessageFrame
import chat.revolt.api.routes.channel.sendMessage
import chat.revolt.api.routes.user.addUserIfUnknown
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.Message as MessageSchema
import chat.revolt.components.chat.Message
import kotlinx.coroutines.launch
import chat.revolt.R
import chat.revolt.RevoltTweenFloat
import chat.revolt.RevoltTweenInt
import chat.revolt.api.routes.channel.fetchMessagesFromChannel
import chat.revolt.api.routes.microservices.autumn.FileArgs
import chat.revolt.components.chat.MessageField
import chat.revolt.components.generic.CollapsibleCard
import chat.revolt.components.generic.PageHeader
import chat.revolt.components.screens.chat.ChannelIcon
import androidx.compose.runtime.getValue
import chat.revolt.api.routes.microservices.autumn.MAX_ATTACHMENTS_PER_MESSAGE
import chat.revolt.api.routes.microservices.autumn.uploadToAutumn
import chat.revolt.components.screens.chat.AttachmentManager
import io.ktor.http.*
import java.io.File

class ChannelScreenViewModel : ViewModel() {
    private var _channel by mutableStateOf<Channel?>(null)
    val channel: Channel?
        get() = _channel

    private var _callbacks = mutableStateOf<RealtimeSocket.ChannelCallback?>(null)
    val callbacks: RealtimeSocket.ChannelCallback?
        get() = _callbacks.value

    private var _renderableMessages = mutableStateListOf<MessageSchema>()
    val renderableMessages: List<MessageSchema>
        get() = _renderableMessages

    private var _typingUsers = mutableStateListOf<String>()
    val typingUsers: List<String>
        get() = _typingUsers

    private var _messageContent by mutableStateOf("")
    val messageContent: String
        get() = _messageContent

    fun setMessageContent(content: String) {
        _messageContent = content

        if (content.isEmpty()) {
            _showButtons = true
        } else if (showButtons) {
            _showButtons = false
        }
    }

    private var _showButtons by mutableStateOf(true)
    val showButtons: Boolean
        get() = _showButtons

    fun setShowButtons(show: Boolean) {
        _showButtons = show
    }

    private var _attachments = mutableStateListOf<FileArgs>()
    val attachments: List<FileArgs>
        get() = _attachments

    fun setAttachments(attachments: List<FileArgs>) {
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

            _renderableMessages.add(message)
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
    }

    private fun registerCallback() {
        _callbacks.value = ChannelScreenCallback()
        RealtimeSocket.registerChannelCallback(channel!!.id!!, callbacks!!)
    }

    fun fetchMessages() {
        if (channel == null) {
            return
        }

        _renderableMessages.clear()

        viewModelScope.launch {
            fetchMessagesFromChannel(channel!!.id!!, limit = 50, false).let {
                it.messages!!.reversed().forEach { message ->
                    addUserIfUnknown(message.author!!)
                    if (!RevoltAPI.messageCache.containsKey(message.id)) {
                        RevoltAPI.messageCache[message.id!!] = message
                    }
                    _renderableMessages.add(message)
                }
            }
        }
    }

    fun fetchOlderMessages() {
        if (channel == null) {
            return
        }

        viewModelScope.launch {
            fetchMessagesFromChannel(
                channel!!.id!!,
                limit = 20,
                true,
                before = renderableMessages.first().id
            ).let {
                it.messages!!.forEach { message ->
                    addUserIfUnknown(message.author!!)
                    if (!RevoltAPI.messageCache.containsKey(message.id)) {
                        RevoltAPI.messageCache[message.id!!] = message
                    }
                    _renderableMessages.add(0, message)
                }
            }
        }
    }

    fun fetchChannel(id: String) {
        if (id in RevoltAPI.channelCache) {
            _channel = RevoltAPI.channelCache[id]
        } else {
            Log.e("ChannelScreen", "Channel $id not in cache, for now this is fatal!") // FIXME
        }

        registerCallback()
        fetchMessages()
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

    fun typingMessageResource(): Int {
        return when (typingUsers.size) {
            0 -> R.string.typing_blank
            1 -> R.string.typing_one
            in 2..4 -> R.string.typing_many
            else -> R.string.typing_several
        }
    }

    fun getTypingUsernames(): String {
        return typingUsers.joinToString {
            RevoltAPI.userCache[it]?.let { u ->
                u.username ?: u.id
            } ?: it
        }
    }
}

@Composable
fun ChannelInfoScreen(
    channel: Channel,
    viewModel: ChannelScreenViewModel,
    onClosed: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager =
        LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelIcon(
                channelType = channel.channelType!!,
                modifier = Modifier.size(32.dp)
            )
            PageHeader(
                text = channel.name ?: channel.id!!,
                modifier = Modifier.offset((-8).dp, 0.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            CollapsibleCard(title = "Advanced") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Channel ID: ${channel.id}")

                    Button(onClick = {
                        clipboardManager.setText(AnnotatedString(channel.id!!))
                        Toast.makeText(
                            context,
                            "Copied",
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text("Copy ID")
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.fetchMessages()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Refetch messages")
                    }
                }
            }
        }

        Button(
            onClick = onClosed,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Close")
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChannelScreen(
    navController: NavController,
    channelId: String,
    viewModel: ChannelScreenViewModel = viewModel()
) {
    val channel = viewModel.channel

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val channelInfoOpen = remember { mutableStateOf(false) }

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

    LaunchedEffect(channelId) {
        viewModel.fetchChannel(channelId)
    }

    DisposableEffect(channelId) {
        onDispose {
            viewModel.callbacks?.let {
                RealtimeSocket.unregisterChannelCallback(channelId, it)
            }
        }
    }

    if (channel == null) {
        CircularProgressIndicator()
        return
    }

    if (channelInfoOpen.value) {
        Dialog(
            onDismissRequest = {
                channelInfoOpen.value = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
            )
        ) {
            ChannelInfoScreen(channel, viewModel) {
                channelInfoOpen.value = false
            }
        }
    }

    Column {
        Row(
            modifier = Modifier
                .clickable {
                    coroutineScope.launch {
                        channelInfoOpen.value = true
                    }
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

        LazyColumn(Modifier.weight(1f)) {
            item {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.fetchOlderMessages()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp)
                ) {
                    Text("Load older")
                }
            }
            items(viewModel.renderableMessages) { message ->
                Message(message)
            }
        }

        AnimatedVisibility(
            visible = viewModel.typingUsers.isNotEmpty(),
            enter = slideInVertically(
                animationSpec = RevoltTweenInt,
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = RevoltTweenFloat),
            exit = slideOutVertically(
                animationSpec = RevoltTweenInt,
                targetOffsetY = { it }
            ) + fadeOut(animationSpec = RevoltTweenFloat)
        ) {
            Row(
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .fillMaxWidth()
                    .padding(all = 4.dp)
            ) {
                Text(
                    text = stringResource(
                        id = viewModel.typingMessageResource(),
                        viewModel.getTypingUsernames()
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        AnimatedVisibility(visible = viewModel.attachments.isNotEmpty()) {
            AttachmentManager(
                attachments = viewModel.attachments,
                uploading = viewModel.sendingMessage,
                onRemove = viewModel::removeAttachment
            )
        }

        MessageField(
            showButtons = viewModel.showButtons,
            onToggleButtons = viewModel::setShowButtons,
            messageContent = viewModel.messageContent,
            onMessageContentChange = viewModel::setMessageContent,
            onSendMessage = viewModel::sendPendingMessage,
            onAddAttachment = {
                pickFileLauncher.launch(arrayOf("*/*"))
            },
            channelType = channel.channelType!!,
            channelName = channel.name ?: channel.id!!,
            forceSendButton = viewModel.attachments.isNotEmpty(),
            disabled = viewModel.sendingMessage
        )
    }
}
