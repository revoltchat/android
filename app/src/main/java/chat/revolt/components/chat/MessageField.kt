package chat.revolt.components.chat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.schemas.ChannelType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageField(
    showButtons: Boolean,
    onToggleButtons: (Boolean) -> Unit,
    messageContent: String,
    onMessageContentChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    channelType: ChannelType,
    channelName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val placeholderResource = when (channelType) {
        ChannelType.DirectMessage -> R.string.message_field_placeholder_dm
        ChannelType.Group -> R.string.message_field_placeholder_group
        ChannelType.TextChannel -> R.string.message_field_placeholder_text
        ChannelType.VoiceChannel -> R.string.message_field_placeholder_voice
        ChannelType.SavedMessages -> R.string.message_field_placeholder_notes
    }

    Row(modifier) {
        // Additional buttons (currently adding an attachment)
        AnimatedVisibility(visible = showButtons) {
            Row(Modifier.weight(1f)) {
                ElevatedButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Placeholder for adding an attachment",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.size(56.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.add_attachment_alt)
                    )
                }
            }
        }

        // The small chevron you see when the buttons are hidden
        AnimatedVisibility(visible = !showButtons) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.height(56.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = stringResource(id = R.string.show_more_alt),
                    modifier = Modifier
                        .size(24.dp + 8.dp)
                        .padding(vertical = 4.dp)
                        .clickable(onClick = {
                            onToggleButtons(true)
                        })
                )
            }
        }

        TextField(
            value = messageContent,
            onValueChange = onMessageContentChange,
            singleLine = false,
            shape = RoundedCornerShape(100),
            placeholder = {
                Text(
                    stringResource(
                        id = placeholderResource,
                        channelName
                    )
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                placeholderColor = Color.Gray,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        // Send button (visible when text is entered)
        AnimatedVisibility(visible = messageContent.isNotBlank()) {
            Button(
                onClick = onSendMessage,
                modifier = Modifier
                    .size(56.dp),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = stringResource(id = R.string.send_alt)
                )
            }
        }
    }
}

@Preview
@Composable
fun MessageFieldPreview() {
    MessageField(
        showButtons = true,
        onToggleButtons = {},
        messageContent = "",
        onMessageContentChange = {},
        onSendMessage = {},
        channelType = ChannelType.TextChannel,
        channelName = "general"
    )
}