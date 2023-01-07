package chat.revolt.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
    onAddAttachment: () -> Unit,
    onSendMessage: () -> Unit,
    channelType: ChannelType,
    channelName: String,
    modifier: Modifier = Modifier,
    forceSendButton: Boolean = false,
    disabled: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
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
                        focusRequester.freeFocus() // hide keyboard because it's annoying
                        onAddAttachment()
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
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = stringResource(id = R.string.show_more_alt),
                    modifier = Modifier
                        .clickable(onClick = {
                            onToggleButtons(true)
                        })
                        .size(24.dp + 8.dp)
                        .padding(vertical = 4.dp)
                )
            }
        }

        TextField(
            value = messageContent,
            onValueChange = onMessageContentChange,
            singleLine = false,
            shape = MaterialTheme.shapes.extraLarge,
            enabled = !disabled,
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
                .padding(start = 8.dp)
                .focusRequester(focusRequester)
        )

        // Send button (visible when text is entered or when forceSendButton is true)
        AnimatedVisibility(visible = (messageContent.isNotBlank() || forceSendButton) && !disabled) {
            Button(
                onClick = onSendMessage,
                modifier = Modifier
                    .padding(start = 8.dp)
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
        messageContent = "Hello world!",
        onMessageContentChange = {},
        onSendMessage = {},
        onAddAttachment = {},
        channelType = ChannelType.TextChannel,
        channelName = "general"
    )
}