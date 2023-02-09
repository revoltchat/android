package chat.revolt.components.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.revolt.api.routes.channel.SendMessageReply
import chat.revolt.components.chat.InReplyTo

@Composable
fun ManageableReply(
    reply: SendMessageReply,
    onToggleMention: () -> Unit,
    onRemove: () -> Unit,
) {
    // TODO Revamp this. Placeholder design ("functional" but extremely ugly)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove reply",
            modifier = Modifier
                .clickable {
                    onRemove()
                }
        )
        InReplyTo(
            messageId = reply.id,
            withMention = reply.mention,
            modifier = Modifier.weight(1f)
        ) {
            onToggleMention()
        }
    }
}

@Composable
fun ReplyManager(
    replies: List<SendMessageReply>,
    onToggleMention: (SendMessageReply) -> Unit,
    onRemove: (SendMessageReply) -> Unit,
) {
    Column {
        replies.forEach { reply ->
            ManageableReply(
                reply = reply,
                onToggleMention = { onToggleMention(reply) },
                onRemove = { onRemove(reply) }
            )
        }
    }
}