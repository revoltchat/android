package chat.revolt.screens.chat.views

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import chat.revolt.components.chat.MessageField

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

    fun fetchChannel(id: String) {
        if (id in RevoltAPI.channelCache) {
            _channel = RevoltAPI.channelCache[id]
        } else {
            Log.e("ChannelScreen", "Channel $id not in cache, for now this is fatal!") // FIXME
        }

        registerCallback()
    }

    fun sendPendingMessage() {
        viewModelScope.launch {
            sendMessage(channel!!.id!!, messageContent)
        }
        _messageContent = ""
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
fun ChannelScreen(
    navController: NavController,
    channelId: String,
    viewModel: ChannelScreenViewModel = viewModel()
) {
    val channel = viewModel.channel
    val scrollState = rememberScrollState()

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

    Column {
        Text(text = "#" + channel.name!!)

        Divider()

        // Column nesting is needed to make the vertical scroll work properly
        Column(Modifier.weight(1f)) {
            Column(Modifier.verticalScroll(scrollState)) {
                viewModel.renderableMessages.forEach {
                    Message(message = it)
                }
            }
        }

        AnimatedVisibility(visible = viewModel.typingUsers.isNotEmpty()) {
            Row(
                Modifier
                    .padding(all = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = stringResource(
                        id = viewModel.typingMessageResource(),
                        viewModel.getTypingUsernames()
                    )
                )
            }
        }


        channel.channelType?.let {
            MessageField(
                showButtons = viewModel.showButtons,
                onToggleButtons = viewModel::setShowButtons,
                messageContent = viewModel.messageContent,
                onMessageContentChange = viewModel::setMessageContent,
                onSendMessage = viewModel::sendPendingMessage,
                channelType = it,
                channelName = channel.name
            )
        }
    }
}