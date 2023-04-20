package chat.revolt.screens.chat.views.channel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltJson
import chat.revolt.api.internals.ULID
import chat.revolt.api.realtime.frames.receivable.ChannelStartTypingFrame
import chat.revolt.api.realtime.frames.receivable.ChannelStopTypingFrame
import chat.revolt.api.realtime.frames.receivable.MessageDeleteFrame
import chat.revolt.api.realtime.frames.receivable.MessageFrame
import chat.revolt.api.realtime.frames.receivable.MessageUpdateFrame
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
import chat.revolt.callbacks.UiCallback
import chat.revolt.callbacks.UiCallbacks
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private fun setNoMoreMessages() {
        _noMoreMessages = true
    }

    fun fetchOlderMessages() {
        if (channel == null) {
            return
        }

        viewModelScope.launch {
            val messages = arrayListOf<Message>()

            fetchMessagesFromChannel(
                channel!!.id!!,
                limit = 50,
                true,
                before = if (renderableMessages.isNotEmpty()) {
                    renderableMessages.first().id
                } else {
                    null
                }
            ).let {
                if (it.messages.isNullOrEmpty() || it.messages.size < 50) {
                    setNoMoreMessages()
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

    fun fetchChannel(id: String) {
        viewModelScope.launch {
            if (id !in RevoltAPI.channelCache) {
                val channel = fetchSingleChannel(id)
                _channel = channel
                RevoltAPI.channelCache[id] = channel
            } else {
                _channel = RevoltAPI.channelCache[id]
            }

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

    suspend fun listenForWsFrame() {
        withContext(RevoltAPI.realtimeContext) {
            flow {
                while (true) {
                    emit(RevoltAPI.wsFrameChannel.receive())
                }
            }.onEach {
                when (it) {
                    is MessageFrame -> {
                        if (it.channel != channel?.id) return@onEach

                        addUserIfUnknown(it.author!!)
                        regroupMessages(listOf(it) + renderableMessages)
                        ackNewest()
                    }

                    is MessageUpdateFrame -> {
                        if (it.channel != channel?.id) return@onEach

                        val messageFrame =
                            RevoltJson.decodeFromJsonElement(MessageFrame.serializer(), it.data)

                        renderableMessages.find { currentMsg ->
                            currentMsg.id == it.id
                        } ?: return@onEach // Message not found, ignore.

                        regroupMessages(renderableMessages.map { currentMsg ->
                            if (currentMsg.id == it.id) {
                                messageFrame
                            } else {
                                currentMsg
                            }
                        })
                    }

                    is MessageDeleteFrame -> {
                        if (it.channel != channel?.id) return@onEach

                        regroupMessages(renderableMessages.filter { currentMsg ->
                            currentMsg.id != it.id
                        })
                    }

                    is ChannelStartTypingFrame -> {
                        if (it.id != channel?.id) return@onEach
                        if (_typingUsers.contains(it.user)) return@onEach

                        addUserIfUnknown(it.user)
                        _typingUsers.add(it.user)
                    }

                    is ChannelStopTypingFrame -> {
                        if (it.id != channel?.id) return@onEach
                        if (!_typingUsers.contains(it.user)) return@onEach

                        _typingUsers.remove(it.user)
                    }
                }
            }.catch {
                Log.e("ChannelScreen", "Failed to receive WS frame", it)
            }.launchIn(this)
        }
    }

    suspend fun listenForUiCallbacks() {
        withContext(Dispatchers.Main) {
            UiCallbacks.uiCallbackFlow.onEach {
                when (it) {
                    is UiCallback.ReplyToMessage -> {
                        addInReplyTo(
                            SendMessageReply(
                                id = it.messageId,
                                mention = false
                            )
                        )
                    }
                }
            }.catch {
                Log.e("ChannelScreen", "Failed to receive UI callback", it)
            }.launchIn(this)
        }
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
