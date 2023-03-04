package chat.revolt.components.chat

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.RevoltTweenFloat
import chat.revolt.RevoltTweenInt
import chat.revolt.api.schemas.ChannelType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageField(
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

    val sendButtonVisible = (messageContent.isNotBlank() || forceSendButton) && !disabled

    Row(modifier) {
        BasicTextField(
            value = messageContent,
            onValueChange = onMessageContentChange,
            singleLine = false,
            enabled = !disabled,
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions.Default,
            keyboardActions = KeyboardActions.Default,
            decorationBox = @Composable { innerTextField ->
                TextFieldDefaults.TextFieldDecorationBox(
                    value = messageContent,
                    innerTextField = innerTextField,
                    enabled = !disabled,
                    singleLine = false,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = remember { MutableInteractionSource() },
                    placeholder = {
                        Text(
                            text = stringResource(
                                id = placeholderResource,
                                channelName
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        placeholderColor = Color.Gray,
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    contentPadding = PaddingValues(16.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Add,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            contentDescription = stringResource(id = R.string.unknown),
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(32.dp)
                                .clickable {
                                    focusRequester.freeFocus() // hide keyboard because it's annoying
                                    onAddAttachment()
                                }
                                .padding(4.dp)
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(sendButtonVisible,
                            enter = slideInVertically(
                                animationSpec = RevoltTweenInt,
                                initialOffsetY = { it }
                            ) + fadeIn(animationSpec = RevoltTweenFloat),
                            exit = slideOutVertically(
                                animationSpec = RevoltTweenInt,
                                targetOffsetY = { it }
                            ) + fadeOut(animationSpec = RevoltTweenFloat)) {
                            Icon(
                                Icons.Default.Send,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = stringResource(id = R.string.unknown),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .size(32.dp)
                                    .clickable { onSendMessage() }
                                    .padding(4.dp)
                            )
                        }
                    }
                )
            }
        )
    }
}

@Preview
@Composable
fun MessageFieldPreview() {
    MessageField(
        messageContent = "Hello world!",
        onMessageContentChange = {},
        onSendMessage = {},
        onAddAttachment = {},
        channelType = ChannelType.TextChannel,
        channelName = "general"
    )
}