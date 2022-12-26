package chat.revolt.screens.chat

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import chat.revolt.api.RevoltAPI
import chat.revolt.api.realtime.RealtimeSocket
import chat.revolt.api.realtime.frames.receivable.ChannelStartTypingFrame
import chat.revolt.api.realtime.frames.receivable.ChannelStopTypingFrame
import chat.revolt.api.realtime.frames.receivable.MessageFrame
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.Message

class ChannelScreenViewModel : ViewModel() {
    private var _channel by mutableStateOf<Channel?>(null)
    val channel: Channel?
        get() = _channel

    private var _callbacks = mutableStateOf<RealtimeSocket.ChannelCallback?>(null)
    val callbacks: RealtimeSocket.ChannelCallback?
        get() = _callbacks.value

    private var _renderableMessages = mutableStateListOf<Message>()
    val renderableMessages: List<Message>
        get() = _renderableMessages

    inner class ChannelScreenCallback : RealtimeSocket.ChannelCallback {
        override fun onMessage(message: MessageFrame) {
            Log.d("ChannelScreen", "onMessage: $message")
            _renderableMessages.add(message)
        }

        override fun onStartTyping(typing: ChannelStartTypingFrame) {
            Log.d("ChannelScreen", "onStartTyping: $typing")
        }

        override fun onStopTyping(typing: ChannelStopTypingFrame) {
            Log.d("ChannelScreen", "onStopTyping: $typing")
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
}

@Composable
fun ChannelScreen(
    navController: NavController,
    channelId: String,
    viewModel: ChannelScreenViewModel = hiltViewModel()
) {
    val channel = viewModel.channel

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
        Text(text = channel.name!!)
        
        viewModel.renderableMessages.forEach {
            Text(text = "[" + it.getAuthor()!!.username + "] " + it.content!!)
        }
    }
}