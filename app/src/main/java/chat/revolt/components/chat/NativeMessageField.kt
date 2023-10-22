package chat.revolt.components.chat

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import chat.revolt.R
import chat.revolt.activities.RevoltTweenFloat
import chat.revolt.activities.RevoltTweenInt
import chat.revolt.api.schemas.ChannelType


@Composable
fun NativeMessageField(
    value: String,
    onValueChange: (String) -> Unit,
    onAddAttachment: () -> Unit,
    onCommitAttachment: (Uri) -> Unit,
    onPickEmoji: () -> Unit,
    onSendMessage: () -> Unit,
    channelType: ChannelType,
    channelName: String,
    modifier: Modifier = Modifier,
    forceSendButton: Boolean = false,
    disabled: Boolean = false,
    editMode: Boolean = false,
    cancelEdit: () -> Unit = {},
    onFocusChange: (Boolean) -> Unit = {}
) {
    val placeholderResource = when (channelType) {
        ChannelType.DirectMessage -> R.string.message_field_placeholder_dm
        ChannelType.Group -> R.string.message_field_placeholder_group
        ChannelType.TextChannel -> R.string.message_field_placeholder_text
        ChannelType.VoiceChannel -> R.string.message_field_placeholder_voice
        ChannelType.SavedMessages -> R.string.message_field_placeholder_notes
    }

    var requestFocus by remember { mutableStateOf({}) }
    var clearFocus by remember { mutableStateOf({}) }

    val sendButtonVisible = (value.isNotBlank() || forceSendButton) && !disabled

    val density = LocalDensity.current

    val selectionColour = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f).toArgb()
    val cursorColour = MaterialTheme.colorScheme.primary.toArgb()
    val contentColour = LocalContentColor.current.toArgb()
    val placeholderColour = LocalContentColor.current.copy(alpha = 0.5f).toArgb()

    LaunchedEffect(editMode) {
        if (editMode) {
            requestFocus()
        } else {
            clearFocus()
        }
    }

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(8.dp))

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
                            // hide keyboard because it's annoying
                            clearFocus()
                            onAddAttachment()
                        }
                    }
                }
                .padding(4.dp)
                .testTag("add_attachment")
        )

        AndroidView(
            factory = { context ->
                object : androidx.appcompat.widget.AppCompatEditText(context) {
                    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
                        var ic = super.onCreateInputConnection(outAttrs)
                        EditorInfoCompat.setContentMimeTypes(
                            outAttrs,
                            arrayOf("image/*")
                        )
                        val mimeTypes = ViewCompat.getOnReceiveContentMimeTypes(this)
                        if (mimeTypes != null) {
                            EditorInfoCompat.setContentMimeTypes(outAttrs, mimeTypes)
                            ic = ic?.let { InputConnectionCompat.createWrapper(this, it, outAttrs) }
                        }
                        return ic
                    }
                }.apply {
                    background = null
                    textSize = 16f
                    setPadding((density.density * 16.dp.value).toInt())

                    // Propagate text changes to parent
                    addTextChangedListener {
                        onValueChange(it.toString())
                    }

                    // Hide/show keyboard on focus change and propagate to parent
                    onFocusChangeListener = android.view.View.OnFocusChangeListener { _, hasFocus ->
                        val keyboard =
                            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        if (hasFocus) {
                            keyboard.showSoftInput(
                                this,
                                0
                            )
                        } else {
                            keyboard.hideSoftInputFromWindow(this.windowToken, 0)
                        }

                        onFocusChange(hasFocus)
                    }

                    ViewCompat.setOnReceiveContentListener(
                        this,
                        arrayOf("image/*")
                    ) { _, payload ->
                        // Check mimetype
                        if (payload.clip.description.hasMimeType("image/*")) {
                            // Get image
                            val item = payload.clip.getItemAt(0)
                            val uri = item.uri

                            if (uri == null) {
                                Log.e("MessageField", "Received payload with null uri")
                                return@setOnReceiveContentListener payload
                            }

                            onCommitAttachment(uri)

                            return@setOnReceiveContentListener null
                        }
                        payload
                    }

                    isFocusable = true
                    isFocusableInTouchMode = true

                    typeface = ResourcesCompat.getFont(context, R.font.inter)

                    // Set colours
                    highlightColor = selectionColour
                    setTextColor(contentColour)
                    setHintTextColor(ColorStateList.valueOf(placeholderColour))

                    // Caret colour and size
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val shapeDrawable = ShapeDrawable(RectShape())
                        shapeDrawable.paint.color = cursorColour
                        val sizeInDp = 1
                        val sizeInPixels = (sizeInDp * resources.displayMetrics.density).toInt()
                        shapeDrawable.intrinsicWidth = sizeInPixels
                        shapeDrawable.intrinsicHeight = sizeInPixels

                        setTextCursorDrawable(shapeDrawable)
                    }

                    clearFocus = {
                        this.clearFocus()
                    }
                    requestFocus = {
                        this.requestFocus()
                    }
                }
            },
            update = {
                if (value != it.text.toString()) {
                    it.setText(value)
                    it.setSelection(value.length)
                }
                it.hint = it.context.getString(placeholderResource, channelName)
            },
            modifier = Modifier
                .weight(1f)
                .testTag("message_field")
        )

        Icon(
            painter = painterResource(R.drawable.ic_emoticon_24dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            contentDescription = stringResource(id = R.string.pick_emoji_alt),
            modifier = Modifier
                .clip(CircleShape)
                .size(32.dp)
                .clickable {
                    clearFocus()
                    onPickEmoji()
                }
                .padding(4.dp)
                .testTag("pick_emoji")
        )

        Spacer(modifier = Modifier.width(8.dp))

        AnimatedVisibility(
            sendButtonVisible,
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
            ) + fadeOut(animationSpec = RevoltTweenFloat)
        ) {
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

@Preview
@Composable
fun NativeMessageFieldPreview() {
    NativeMessageField(
        value = "Hello world!",
        onValueChange = {},
        onAddAttachment = {},
        onCommitAttachment = {},
        onPickEmoji = {},
        onSendMessage = {},
        channelType = ChannelType.DirectMessage,
        channelName = "Test",
        modifier = Modifier,
        forceSendButton = false,
        disabled = false,
        editMode = false,
        cancelEdit = {},
        onFocusChange = {}
    )
}