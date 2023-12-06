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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
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
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.schemas.Member
import chat.revolt.components.generic.RemoteImage
import chat.revolt.internals.Autocomplete
import kotlinx.coroutines.launch

fun String.applyAutocompleteSuggestion(
    suggestion: AutocompleteSuggestion,
    cursorPosition: Int
): String {
    return when (suggestion) {
        is AutocompleteSuggestion.User -> {
            this.replaceRange(
                cursorPosition - suggestion.query.length - 1,
                cursorPosition,
                "@${suggestion.user.username}#${suggestion.user.discriminator} "
            )
        }

        is AutocompleteSuggestion.Channel -> {
            if (suggestion.channel.name?.contains(" ") == true) {
                this.replaceRange(
                    cursorPosition - suggestion.query.length - 1,
                    cursorPosition,
                    "#${suggestion.channel.name} "
                )
            } else {
                this.replaceRange(
                    cursorPosition - suggestion.query.length - 1,
                    cursorPosition,
                    "<#${suggestion.channel.id}> "
                )
            }
        }

        is AutocompleteSuggestion.Emoji -> {
            this.replaceRange(
                cursorPosition - suggestion.query.length - 1,
                cursorPosition,
                suggestion.shortcode + " "
            )
        }
    }
}

sealed class AutocompleteSuggestion {
    data class User(
        val user: chat.revolt.api.schemas.User,
        val member: Member?,
        val query: String
    ) : AutocompleteSuggestion()

    data class Channel(
        val channel: chat.revolt.api.schemas.Channel,
        val query: String
    ) : AutocompleteSuggestion()

    data class Emoji(
        val shortcode: String,
        val unicode: String?,
        val custom: chat.revolt.api.schemas.Emoji?,
        val query: String
    ) : AutocompleteSuggestion()
}

@OptIn(ExperimentalFoundationApi::class)
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
    serverId: String? = null,
    channelId: String? = null,
    cancelEdit: () -> Unit = {},
    onFocusChange: (Boolean) -> Unit = {},
    onSelectionChange: (Pair<Int, Int>) -> Unit = {}
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

    val scope = rememberCoroutineScope()

    val selectionColour = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f).toArgb()
    val cursorColour = MaterialTheme.colorScheme.primary.toArgb()
    val contentColour = LocalContentColor.current.toArgb()
    val placeholderColour = LocalContentColor.current.copy(alpha = 0.5f).toArgb()

    var selection by remember { mutableStateOf(0 to 0) }
    val autocompleteSuggestions = remember { mutableStateListOf<AutocompleteSuggestion>() }
    val autocompleteSuggestionScrollState = rememberScrollState()

    LaunchedEffect(editMode) {
        if (editMode) {
            requestFocus()
        } else {
            clearFocus()
        }
    }

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        AnimatedVisibility(
            visible = autocompleteSuggestions.size > 0,
            enter = expandIn(initialSize = { full ->
                IntSize(
                    full.width,
                    0
                )
            }) + slideInVertically(
                animationSpec = RevoltTweenInt,
                initialOffsetY = { -it }
            ),
            exit = shrinkOut(targetSize = { full ->
                IntSize(
                    full.width,
                    0
                )
            }) + slideOutVertically(
                animationSpec = RevoltTweenInt,
                targetOffsetY = { it }
            )
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(autocompleteSuggestions.size, key = {
                    when (val item = autocompleteSuggestions[it]) {
                        is AutocompleteSuggestion.User -> item.user.id!!
                        is AutocompleteSuggestion.Channel -> item.channel.id!!
                        is AutocompleteSuggestion.Emoji -> item.shortcode
                    }
                }) {
                    when (val item = autocompleteSuggestions[it]) {
                        is AutocompleteSuggestion.User -> {
                            SuggestionChip(
                                onClick = {
                                    onValueChange(
                                        value.applyAutocompleteSuggestion(
                                            item,
                                            selection.first
                                        )
                                    )
                                },
                                label = { Text("@${item.user.username}#${item.user.discriminator}") },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_human_greeting_variant_24dp),
                                        contentDescription = null,
                                        modifier = Modifier.size(SuggestionChipDefaults.IconSize)
                                    )
                                },
                                modifier = Modifier
                                    .animateItemPlacement()
                            )
                        }

                        is AutocompleteSuggestion.Channel -> {
                            SuggestionChip(
                                onClick = {
                                    onValueChange(
                                        value.applyAutocompleteSuggestion(
                                            item,
                                            selection.first
                                        )
                                    )
                                },
                                label = { Text("#${item.channel.name}") },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_pound_24dp),
                                        contentDescription = null,
                                        modifier = Modifier.size(SuggestionChipDefaults.IconSize)
                                    )
                                },
                                modifier = Modifier
                                    .animateItemPlacement()
                            )
                        }

                        is AutocompleteSuggestion.Emoji -> {
                            SuggestionChip(
                                onClick = {
                                    onValueChange(
                                        value.applyAutocompleteSuggestion(
                                            item,
                                            selection.first
                                        )
                                    )
                                },
                                label = {
                                    if (item.custom != null) {
                                        Text(":${item.custom.name}:")
                                    } else {
                                        Text(item.shortcode)
                                    }
                                },
                                icon = {
                                    if (item.unicode != null) {
                                        Text(
                                            item.unicode,
                                            modifier = Modifier
                                                .size(SuggestionChipDefaults.IconSize)
                                                .align(Alignment.CenterHorizontally),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        RemoteImage(
                                            url = "$REVOLT_FILES/emojis/${item.custom?.id}/emoji.gif",
                                            description = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .size(SuggestionChipDefaults.IconSize)
                                                .align(Alignment.CenterHorizontally)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .animateItemPlacement()
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            verticalAlignment = Alignment.CenterVertically
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
                                ic = ic?.let {
                                    InputConnectionCompat.createWrapper(
                                        this,
                                        it,
                                        outAttrs
                                    )
                                }
                            }
                            return ic
                        }

                        override fun onSelectionChanged(selStart: Int, selEnd: Int) {
                            super.onSelectionChanged(selStart, selEnd)
                            onSelectionChange(selStart to selEnd)
                            selection = selStart to selEnd

                            scope.launch {
                                autocompleteSuggestionScrollState.scrollTo(0)
                            }
                            autocompleteSuggestions.clear()

                            if (text?.isNotBlank() == false) return
                            if (selStart != selEnd) return

                            val lastWord =
                                text?.substring(0, selStart)?.split(" ")?.lastOrNull() ?: return

                            when {
                                lastWord.startsWith(':') && !lastWord.endsWith(':') -> {
                                    autocompleteSuggestions.addAll(
                                        Autocomplete.emoji(lastWord.substring(1))
                                    )
                                }
                            }
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
                        onFocusChangeListener =
                            android.view.View.OnFocusChangeListener { _, hasFocus ->
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
        onFocusChange = {},
        onSelectionChange = {}
    )
}
