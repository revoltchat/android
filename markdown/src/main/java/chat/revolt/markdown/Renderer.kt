package chat.revolt.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

object Renderer {
    fun annotateMarkdown(text: String): AnnotatedString {
        // TODO this is all placeholder code
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        return buildAnnotatedString {
            append(text)

            boldRegex.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = match.groups[1]!!.range.first,
                    end = match.groups[1]!!.range.last + 1
                )
            }

            toAnnotatedString()
        }
    }
}