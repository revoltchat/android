package chat.revolt.screens.chat.views.channel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ULID
import chat.revolt.api.routes.channel.SendMessageReply
import chat.revolt.api.routes.channel.ackChannel
import chat.revolt.api.routes.channel.fetchMessagesFromChannel
import chat.revolt.api.routes.channel.fetchSingleChannel
import chat.revolt.api.routes.channel.sendMessage
import chat.revolt.api.routes.microservices.autumn.FileArgs
import chat.revolt.api.routes.microservices.autumn.MAX_ATTACHMENTS_PER_MESSAGE
import chat.revolt.api.routes.microservices.autumn.uploadToAutumn
import chat.revolt.api.routes.user.addUserIfUnknown
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.Message
import chat.revolt.callbacks.ChannelCallbacks
import chat.revolt.callbacks.UiCallbacks
import io.ktor.http.ContentType
import io.sentry.Sentry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

class ChannelScreenViewModel : ViewModel() {
    private var _channel by mutableStateOf<Channel?>(null)
    val channel: Channel?
        get() = _channel

    private var _renderableMessages = mutableStateListOf<Message>()
    val renderableMessages: List<Message>
        get() = _renderableMessages

    private fun setRenderableMessages(messages: List<Message>) {
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

    private var _replies = mutableStateListOf<SendMessageReply>()
    val replies: List<SendMessageReply>
        get() = _replies

    fun addInReplyTo(reply: SendMessageReply) {
        _replies.add(reply)
    }

    fun removeReply(reply: SendMessageReply) {
        _replies.remove(reply)
    }

    fun toggleReplyMentionFor(reply: SendMessageReply) {
        val index = _replies.indexOf(reply)
        val newReply = SendMessageReply(
            reply.id,
            !reply.mention
        )

        _replies[index] = newReply
    }

    private fun clearInReplyTo() {
        _replies.clear()
    }

    private var _noMoreMessages by mutableStateOf(false)
    val noMoreMessages: Boolean
        get() = _noMoreMessages

    private fun setNoMoreMessages(noMore: Boolean) {
        _noMoreMessages = noMore
    }

    private var _uiCallbackReceiver = mutableStateOf<UiCallbacks.CallbackReceiver?>(null)
    val uiCallbackReceiver: UiCallbacks.CallbackReceiver?
        get() = _uiCallbackReceiver.value

    private var _uiCallbackRegistered by mutableStateOf(false)

    private var _channelCallbackReceiver = mutableStateOf<ChannelCallbacks.CallbackReceiver?>(null)
    val channelCallbackReceiver: ChannelCallbacks.CallbackReceiver?
        get() = _channelCallbackReceiver.value

    private var _channelCallbackRegistered by mutableStateOf(false)

    /*
    inner class ChannelScreenCallback : RealtimeSocket.ChannelCallback {
        override fun onMessage(message: Message) {
            viewModelScope.launch {
                addUserIfUnknown(message.author!!)
            }

            regroupMessages(listOf(message) + renderableMessages)
            ackNewest()
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
    }*/

    inner class UiCallbackReceiver : UiCallbacks.CallbackReceiver {
        override fun onQueueMessageForReply(messageId: String) {
            viewModelScope.launch {
                addInReplyTo(SendMessageReply(messageId, true))
            }
        }
    }

    inner class ChannelCallbackReceiver : ChannelCallbacks.CallbackReceiver {
        override fun onReconnect() {
            fetchMessages()
            _typingUsers.clear()
            // TODO push time rift to messages
        }

        override fun onStartTyping(channelId: String, userId: String) {
            viewModelScope.launch {
                addUserIfUnknown(userId)

                if (!_typingUsers.contains(userId)) {
                    _typingUsers.add(userId)
                }
            }
        }

        override fun onStopTyping(channelId: String, userId: String) {
            if (_typingUsers.contains(userId)) {
                _typingUsers.remove(userId)
            }
        }

        override fun onMessage(messageId: String) {
            viewModelScope.launch {
                val message = RevoltAPI.messageCache[messageId] ?: return@launch

                addUserIfUnknown(message.author!!)

                regroupMessages(listOf(message) + renderableMessages)
                ackNewest()
            }
        }

        override fun onMessageUpdate(messageId: String) {
            val message = RevoltAPI.messageCache[messageId] ?: return

            Log.d("ChannelScreen", "Handler Message updated: $message")

            regroupMessages(renderableMessages.map {
                if (it.id == message.id) {
                    message
                } else {
                    it
                }
            })
        }

        override fun onMessageDelete(messageId: String) {
            // TODO Not implemented
            Log.d("ChannelScreen", "Handler Message deleted: $messageId")
        }

        override fun onMessageBulkDelete(messageIds: List<String>) {
            // TODO Not implemented
            Log.d("ChannelScreen", "Handler Messages bulk deleted: $messageIds")
        }

        override fun onMessageReactionAdd(messageId: String, emoji: String, userId: String) {
            // TODO Not implemented
            Log.d("ChannelScreen", "Handler Message reaction added: $messageId $emoji $userId")
        }

        override fun onMessageReactionRemove(messageId: String, emoji: String, userId: String) {
            // TODO Not implemented
            Log.d("ChannelScreen", "Handler Message reaction removed: $messageId $emoji $userId")
        }

        override fun onMessageReactionRemoveAll(messageId: String) {
            // TODO Not implemented
            Log.d("ChannelScreen", "Handler Message reactions removed: $messageId")
        }
    }

    private fun registerCallbacks() {
        if (channel?.id == null) {
            Sentry.captureException(IllegalStateException("Channel ID is null while trying to register callbacks"))
            return
        }

        if (!_channelCallbackRegistered) {
            _channelCallbackReceiver.value = ChannelCallbackReceiver()
            ChannelCallbacks.registerReceiver(channel!!.id!!, _channelCallbackReceiver.value!!)
            _channelCallbackRegistered = true
        } else {
            Log.d(
                "ChannelScreenViewModel",
                "Channel Callbacks already registered but trying to register again. Ignoring but this is a bug."
            )
        }

        if (!_uiCallbackRegistered) {
            _uiCallbackReceiver.value = UiCallbackReceiver()
            UiCallbacks.registerReceiver(_uiCallbackReceiver.value!!)
            _uiCallbackRegistered = true
        } else {
            Log.d(
                "ChannelScreenViewModel",
                "UI Callbacks already registered but trying to register again. Ignoring but this is a bug."
            )
        }
    }

    fun fetchMessages() {
        if (channel == null) {
            return
        }

        _renderableMessages.clear()

        viewModelScope.launch {
            val messages = arrayListOf<Message>()
            fetchMessagesFromChannel(channel!!.id!!, limit = 50, false).let {
                if (it.messages.isNullOrEmpty() || it.messages.size < 50) {
                    setNoMoreMessages(true)
                }

                it.messages?.forEach { message ->
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
            val messages = arrayListOf<Message>()

            if (renderableMessages.isNotEmpty()) {
                fetchMessagesFromChannel(
                    channel!!.id!!,
                    limit = 50,
                    true,
                    before = renderableMessages.last().id
                ).let {
                    if (it.messages.isNullOrEmpty() || it.messages.size < 50) {
                        setNoMoreMessages(true)
                    }

                    it.messages?.forEach { message ->
                        addUserIfUnknown(message.author ?: return@forEach)
                        if (!RevoltAPI.messageCache.containsKey(message.id)) {
                            RevoltAPI.messageCache[message.id!!] = message
                        }
                        messages.add(message)
                    }
                }
            } else {
                fetchMessagesFromChannel(channel!!.id!!, limit = 50, true).let {
                    if (it.messages.isNullOrEmpty() || it.messages.size < 50) {
                        setNoMoreMessages(true)
                    }

                    it.messages?.forEach { message ->
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
        viewModelScope.launch {
            if (id !in RevoltAPI.channelCache) {
                val channel = fetchSingleChannel(id)
                _channel = channel
                RevoltAPI.channelCache[id] = channel
            } else {
                _channel = RevoltAPI.channelCache[id]
            }

            registerCallbacks()

            if (_channel?.lastMessageID != null) {
                ackNewest()
            } else {
                Log.d("ChannelScreen", "No last message ID, not acking.")
            }
        }
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
                attachments = if (attachmentIds.isEmpty()) null else attachmentIds,
                replies = replies
            )

            _messageContent = ""
            popAttachmentBatch()
            clearInReplyTo()
            setSendingMessage(false)
        }
    }

    private fun regroupMessages(newMessages: List<Message> = renderableMessages) {
        val groupedMessages = mutableMapOf<String, Message>()

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

            if (groupedMessages.containsKey(message.id!!)) return@forEach

            groupedMessages[message.id] = message.copy(tail = tail)
        }

        setRenderableMessages(groupedMessages.values.toList())
    }

    private var debouncedChannelAck: Job? = null
    private fun ackNewest() {
        if (debouncedChannelAck?.isActive == true) {
            debouncedChannelAck?.cancel()

            Log.d("ChannelScreen", "Cancelling channel ack")
        }

        if (channel?.lastMessageID == null) return

        RevoltAPI.unreads.processExternalAck(channel!!.id!!, channel!!.lastMessageID!!)

        debouncedChannelAck = viewModelScope.launch {
            delay(1000)
            if (channel?.lastMessageID == null) return@launch
            ackChannel(channel!!.id!!, channel!!.lastMessageID!!)

            Log.d("ChannelScreen", "Acking channel")
        }
    }
}
