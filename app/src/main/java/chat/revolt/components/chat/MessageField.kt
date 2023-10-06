package chat.revolt.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.activities.RevoltTweenFloat
import chat.revolt.activities.RevoltTweenInt
import chat.revolt.api.schemas.ChannelType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onAddAttachment: () -> Unit,
    onPickEmoji: () -> Unit,
    onSendMessage: () -> Unit,
    channelType: ChannelType,
    channelName: String,
    modifier: Modifier = Modifier,
    forceSendButton: Boolean = false,
    disabled: Boolean = false,
    editMode: Boolean = false,
    cancelEdit: () -> Unit = {},
    onFocusChange: (Boolean) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val placeholderResource = when (channelType) {
        ChannelType.DirectMessage -> R.string.message_field_placeholder_dm
        ChannelType.Group -> R.string.message_field_placeholder_group
        ChannelType.TextChannel -> R.string.message_field_placeholder_text
        ChannelType.VoiceChannel -> R.string.message_field_placeholder_voice
        ChannelType.SavedMessages -> R.string.message_field_placeholder_notes
    }

    val sendButtonVisible = (value.text.isNotBlank() || forceSendButton) && !disabled

    LaunchedEffect(editMode) {
        if (editMode) {
            focusRequester.requestFocus()
        } else {
            focusRequester.freeFocus()
        }
    }

    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = false,
            enabled = !disabled,
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    onFocusChange(state.isFocused)
                },
            keyboardOptions = KeyboardOptions.Default,
            keyboardActions = KeyboardActions.Default,
            decorationBox = @Composable { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = value.text,
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
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        unfocusedPlaceholderColor = Color.Gray,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                            1.dp
                        ),
                        focusedContainerColor = Color.Transparent,
                    ),
                    contentPadding = PaddingValues(16.dp),
                    leadingIcon = {
                        Icon(
                            when {
                                editMode -> Icons.Default.Close
                                else -> Icons.Default.Add
                            },
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            contentDescription = stringResource(id = R.string.add_attachment_alt),
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(32.dp)
                                .clickable {
                                    when {
                                        editMode -> cancelEdit()
                                        else -> {
                                            focusRequester.freeFocus() // hide keyboard because it's annoying
                                            onAddAttachment()
                                        }
                                    }
                                }
                                .padding(4.dp)
                                .testTag("add_attachment")
                        )
                    },
                    trailingIcon = {
                        Row {
                            Icon(
                                painter = painterResource(R.drawable.ic_emoticon_24dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                contentDescription = stringResource(id = R.string.pick_emoji_alt),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .size(32.dp)
                                    .clickable {
                                        focusRequester.freeFocus() // hide keyboard because it's annoying
                                        onPickEmoji()
                                    }
                                    .padding(4.dp)
                                    .testTag("pick_emoji")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            AnimatedVisibility(sendButtonVisible,
                                enter = expandIn(initialSize = { full ->
                                    IntSize(
                                        0,
                                        full.height
                                    )
                                }) + slideInHorizontally(
                                    animationSpec = RevoltTweenInt,
                                    initialOffsetX = { -it }
                                ) + fadeIn(animationSpec = RevoltTweenFloat),
                                exit = shrinkOut(targetSize = { full ->
                                    IntSize(
                                        0,
                                        full.height
                                    )
                                }) + slideOutHorizontally(
                                    animationSpec = RevoltTweenInt,
                                    targetOffsetX = { it }
                                ) + fadeOut(animationSpec = RevoltTweenFloat)) {
                                Icon(
                                    when {
                                        editMode -> Icons.Default.Edit
                                        else -> Icons.Default.Send
                                    },
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = stringResource(id = R.string.send_alt),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .clip(CircleShape)
                                        .clickable { onSendMessage() }
                                        .size(32.dp)
                                        .padding(4.dp)
                                        .testTag("send_message")
                                )
                            }
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
        value = TextFieldValue("Hello world!"),
        onValueChange = {},
        onSendMessage = {},
        onAddAttachment = {},
        onPickEmoji = {},
        channelType = ChannelType.TextChannel,
        channelName = "general"
    )
}