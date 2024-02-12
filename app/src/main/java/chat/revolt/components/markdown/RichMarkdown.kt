package chat.revolt.components.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.revolt.ndk.Stendal

@Composable
fun RichMarkdown(input: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        MarkdownTree(node = Stendal.render(input))
    }
}