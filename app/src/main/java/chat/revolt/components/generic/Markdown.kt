package chat.revolt.components.generic

import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.internals.markdown.ChannelMentionRule
import chat.revolt.internals.markdown.CustomEmoteRule
import chat.revolt.internals.markdown.MarkdownContext
import chat.revolt.internals.markdown.MarkdownParser
import chat.revolt.internals.markdown.MarkdownState
import chat.revolt.internals.markdown.UserMentionRule
import chat.revolt.internals.markdown.createCodeRule
import chat.revolt.internals.markdown.createInlineCodeRule
import com.discord.simpleast.core.simple.SimpleMarkdownRules
import com.discord.simpleast.core.simple.SimpleRenderer

/**
 * A Markdown rendering component for Markdown embedded in UI (e.g. in a button).
 * @param text The text to render.
 * @param fontSize The font size to use.
 * @param modifier The modifier to apply to the rendered text. Will be applied to AndroidView and thus subject to AndroidView's limitations.
 * @param maxLines The maximum number of lines to display. Text will always be ellipsized on overflow. Defaults to [Int.MAX_VALUE].
 */
@Composable
fun UIMarkdown(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
    maxLines: Int = Int.MAX_VALUE,
) {
    val context = LocalContext.current
    val foregroundColor = LocalContentColor.current
    val codeBlockColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    val spannableStringBuilder = remember { mutableStateOf(SpannableStringBuilder()) }

    LaunchedEffect(text) {
        val parser = MarkdownParser()
            .addRules(
                SimpleMarkdownRules.createEscapeRule(),
                UserMentionRule(),
                ChannelMentionRule(),
                CustomEmoteRule(),
            )
            .addRules(
                createCodeRule(context, codeBlockColor.toArgb()),
                createInlineCodeRule(context, codeBlockColor.toArgb()),
            )
            .addRules(
                SimpleMarkdownRules.createSimpleMarkdownRules(
                    includeEscapeRule = false
                )
            )

        spannableStringBuilder.value = SimpleRenderer.render(
            source = text,
            parser = parser,
            initialState = MarkdownState(0),
            renderContext = MarkdownContext(
                memberMap = mapOf(),
                userMap = RevoltAPI.userCache.toMap(),
                channelMap = RevoltAPI.channelCache.mapValues { ch ->
                    ch.value.name ?: ch.value.id ?: "{this does not exist 🤫}"
                },
                emojiMap = RevoltAPI.emojiCache,
                serverId = null
            )
        )

        Log.d("Markdown", "Rendered: ${spannableStringBuilder.value}")
    }

    AndroidView(
        factory = {
            androidx.appcompat.widget.AppCompatTextView(it).apply {
                ellipsize = TextUtils.TruncateAt.END
                typeface = ResourcesCompat.getFont(it, R.font.inter)

                setTextColor(foregroundColor.toArgb())
                setMaxLines(maxLines)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.value)

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        modifier = modifier,
        update = {
            it.text = spannableStringBuilder.value
        },
    )
}

@Preview
@Composable
fun UIMarkdownPreview() {
    // Will not render in side preview but will render on device
    UIMarkdown(
        text = "Hello, **world**!",
        fontSize = 16.sp,
    )
}