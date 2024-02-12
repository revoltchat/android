package chat.revolt.components.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.api.internals.solidColor
import chat.revolt.ndk.AstNode

data class MarkdownTreeConfig(
    val linksClickable: Boolean = true,
    val currentServer: String? = null
)

val LocalMarkdownTreeConfig =
    compositionLocalOf(structuralEqualityPolicy()) { MarkdownTreeConfig() }

@Composable
private fun Children(node: AstNode) {
    node.children?.forEach { MarkdownTree(it) }
}

@Composable
fun MarkdownTree(node: AstNode) {
    when (node.stringType) {
        "heading" -> {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = when (node.level) {
                        1 -> 32.sp
                        2 -> 24.sp
                        3 -> 20.sp
                        4 -> 16.sp
                        5 -> 14.sp
                        else -> 12.sp
                    }
                )
            ) {
                if (node.startLine != 1) {
                    Box(Modifier.padding(top = 8.dp)) {
                        MarkdownText(node)
                    }
                } else {
                    MarkdownText(node)
                }
            }
        }

        "paragraph" -> {
            MarkdownText(
                node,
                modifier = Modifier
                    .then(
                        if (node.startLine != 1) {
                            Modifier.padding(top = 8.dp)
                        } else {
                            Modifier
                        }
                    )
            )
        }

        "document" -> {
            Children(node)
        }

        "text" -> {
            MarkdownText(node)
        }

        "code_block" -> {
            MarkdownCodeBlock(node)
        }

        "block_quote" -> {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Stripe at the left side of the blockquote
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.solidColor(LocalContentColor.current)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Children(node)
                }
            }
        }

        else -> {
            Children(node)
        }
    }
}