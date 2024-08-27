package chat.revolt.components.markdown.jbm

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.components.markdown.Annotations
import chat.revolt.components.utils.detectTapGesturesConditionalConsume
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

data class JBMarkdownTreeState(
    val sourceText: String = "",
    val ignoreLineBreaks: Boolean = false,
    val listDepth: Int = 0,
    val fontSizeMultiplier: Float = 1f,
    val linksClickable: Boolean = true
)

val LocalJBMarkdownTreeState =
    compositionLocalOf(structuralEqualityPolicy()) { JBMarkdownTreeState() }

@Composable
@JBM
fun JBMRenderer(content: String, modifier: Modifier = Modifier) {
    var tree by remember { mutableStateOf(JBMApi.parse(content)) }

    LaunchedEffect(content) {
        tree = JBMApi.parse(content)

        Log.d("JBMRenderer", "Parsed tree: ${tree.children.map { it.type.name }}")
    }

    CompositionLocalProvider(
        LocalJBMarkdownTreeState provides JBMarkdownTreeState(content)
    ) {
        tree.children.map {
            JBMBlock(it, modifier)
        }
    }
}

private fun annotateText(
    state: JBMarkdownTreeState,
    node: ASTNode
): AnnotatedString {
    val sourceText = state.sourceText

    return try {
        buildAnnotatedString {
            when (node.type) {
                MarkdownTokenTypes.TEXT -> {
                    append(node.getTextInNode(sourceText))
                }

                MarkdownElementTypes.EMPH -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        for (child in node.children) {
                            append(annotateText(state, child))
                        }
                    }
                }

                MarkdownElementTypes.STRONG -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        for (child in node.children) {
                            append(annotateText(state, child))
                        }
                    }
                }

                GFMElementTypes.STRIKETHROUGH -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        // Skip the first two children and the last two children
                        // because they are the tilde characters
                        for (child in node.children.subList(2, node.children.size - 2)) {
                            append(annotateText(state, child))
                        }
                    }
                }

                MarkdownTokenTypes.LIST_BULLET -> {
                    append(" ".repeat(state.listDepth) + " " + if (state.listDepth % 2 == 0) "•" else "◦" + " ")
                }

                MarkdownTokenTypes.LIST_NUMBER -> {
                    withStyle(SpanStyle(fontFeatureSettings = "'tnum'")) {
                        append(" ".repeat(state.listDepth) + "${node.getTextInNode(sourceText)} ")
                    }
                }

                MarkdownElementTypes.UNORDERED_LIST,
                MarkdownElementTypes.ORDERED_LIST,
                MarkdownElementTypes.LIST_ITEM -> {
                    for (child in node.children) {
                        append(annotateText(state, child))
                    }
                }

                GFMTokenTypes.CHECK_BOX -> {
                    if (node.getTextInNode(sourceText).trim() == "[ ]") {
                        appendInlineContent("checkbox", "❌")
                    } else {
                        appendInlineContent("checkbox", "✅")
                    }
                    append(" ")
                }

                MarkdownElementTypes.PARAGRAPH, MarkdownElementTypes.HTML_BLOCK -> {
                    for (child in node.children) {
                        append(annotateText(state, child))
                    }
                }

                // re-render types
                // for example, various syntactic elements like exclamation marks, brackets, etc.
                // we simply append the text as is
                MarkdownTokenTypes.EXCLAMATION_MARK,
                MarkdownTokenTypes.LBRACKET,
                MarkdownTokenTypes.RBRACKET,
                MarkdownTokenTypes.LPAREN,
                MarkdownTokenTypes.RPAREN,
                MarkdownTokenTypes.LT,
                MarkdownTokenTypes.GT,
                MarkdownTokenTypes.BACKTICK,
                MarkdownTokenTypes.DOUBLE_QUOTE,
                MarkdownTokenTypes.SINGLE_QUOTE,
                MarkdownTokenTypes.EOL,
                MarkdownTokenTypes.WHITE_SPACE,
                MarkdownTokenTypes.COLON,
                GFMTokenTypes.TILDE -> {
                    append(node.getTextInNode(sourceText))
                }

                // no-op types
                // for example, the special characters that are used to denote the markup are here
                MarkdownTokenTypes.EMPH -> {
                }

                else -> {
                    append("[${node.type.name}]{\n")
                    append(node.getTextInNode(sourceText))
                    append("\n}")
                }
            }
        }
    } catch (e: Exception) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFFFF0000), background = Color(0xFF000000))) {
                append("[${node.type.name}] Error: ${e.message}")
            }

            Log.e("JBMRenderer", "Error rendering node: ${node.type.name}", e)
        }
    }
}

@Composable
private fun JBMText(node: ASTNode, modifier: Modifier) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val mdState = LocalJBMarkdownTreeState.current
    val annotatedText = remember(node) { annotateText(mdState, node) }
    val colours = MaterialTheme.colorScheme

    val shouldConsumeTap = handler@{ offset: Int ->
        Annotations.entries.filter { it.clickable }.map { it.tag }.forEach { tag ->
            if (annotatedText.getStringAnnotations(
                    tag = tag,
                    start = offset,
                    end = offset
                ).isNotEmpty()
            ) {
                return@handler true
            }
        }

        return@handler false
    }

    val onClick = handler@{ offset: Int ->
        if (mdState.linksClickable) {
        }
    }

    val onLongClick = handler@{ offset: Int ->
        if (mdState.linksClickable) {
        }
    }

    Text(
        text = annotatedText,
        onTextLayout = { layoutResult = it },
        modifier = modifier.pointerInput(onClick, onLongClick) {
            detectTapGesturesConditionalConsume(
                onTap = { pos ->
                    val index =
                        layoutResult?.getOffsetForPosition(pos)
                            ?: return@detectTapGesturesConditionalConsume
                    onClick(index)
                },
                onLongPress = { pos ->
                    val index =
                        layoutResult?.getOffsetForPosition(pos)
                            ?: return@detectTapGesturesConditionalConsume
                    onLongClick(index)
                },
                shouldConsumeTap = { pos ->
                    val index =
                        layoutResult?.getOffsetForPosition(pos)
                            ?: return@detectTapGesturesConditionalConsume false
                    shouldConsumeTap(index)
                }
            )
        },
        inlineContent = mapOf(
            "checkbox" to InlineTextContent(
                placeholder = Placeholder(
                    width = LocalTextStyle.current.fontSize * 1.5,
                    height = LocalTextStyle.current.fontSize * 1.5,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                ),
                children = { alternateText ->
                    val isCheck = alternateText == "✅"

                    with(LocalDensity.current) {
                        Canvas(modifier = Modifier.size((LocalTextStyle.current.fontSize * 1.5).toDp())) {
                            drawRoundRect(
                                color = if (isCheck) colours.primaryContainer else colours.surfaceContainer,
                                cornerRadius = CornerRadius(size.width * 0.1f),
                                topLeft = Offset(size.width * 0.1f, size.height * 0.1f),
                                size = size.copy(
                                    width = size.width * 0.8f,
                                    height = size.height * 0.8f
                                )
                            )

                            if (isCheck) {
                                drawPath(
                                    path = Path().apply {
                                        moveTo(size.width * 0.8f, size.height * 0.3f)
                                        lineTo(size.width * 0.4f, size.height * 0.7f)
                                        lineTo(size.width * 0.2f, size.height * 0.5f)
                                    },
                                    color = colours.onPrimaryContainer,
                                    style = Stroke(width = size.width * 0.1f)
                                )
                            }
                        }
                    }
                }
            )
        )
    )
}

@Composable
private fun JBMBlock(node: ASTNode, modifier: Modifier) {
    when (node.type) {
        MarkdownElementTypes.PARAGRAPH,
        MarkdownElementTypes.HTML_BLOCK -> {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontSize = LocalTextStyle.current.fontSize * LocalJBMarkdownTreeState.current.fontSizeMultiplier
                )
            ) {
                JBMText(node, modifier)
            }
        }

        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4,
        MarkdownElementTypes.ATX_5,
        MarkdownElementTypes.ATX_6 -> {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = when (node.type) {
                        MarkdownElementTypes.ATX_1 -> 32.sp * LocalJBMarkdownTreeState.current.fontSizeMultiplier
                        MarkdownElementTypes.ATX_2 -> 24.sp * LocalJBMarkdownTreeState.current.fontSizeMultiplier
                        MarkdownElementTypes.ATX_3 -> 20.sp * LocalJBMarkdownTreeState.current.fontSizeMultiplier
                        MarkdownElementTypes.ATX_4 -> 16.sp * LocalJBMarkdownTreeState.current.fontSizeMultiplier
                        MarkdownElementTypes.ATX_5 -> 14.sp * LocalJBMarkdownTreeState.current.fontSizeMultiplier
                        else -> 12.sp * LocalJBMarkdownTreeState.current.fontSizeMultiplier
                    },
                    color = when (node.type) {
                        MarkdownElementTypes.ATX_1 -> Color(0xFFFF0000)
                        MarkdownElementTypes.ATX_2 -> Color(0xFF00FF00)
                        MarkdownElementTypes.ATX_3 -> Color(0xFF0000FF)
                        MarkdownElementTypes.ATX_4 -> Color(0xFFFF00FF)
                        MarkdownElementTypes.ATX_5 -> Color(0xFF00FFFF)
                        else -> Color(0xFFFFFF00)
                    }
                )
            ) {
                if (node.startOffset != 0) {
                    Box(Modifier.padding(top = 8.dp))
                }
                JBMText(node, modifier)
            }
        }

        MarkdownElementTypes.ORDERED_LIST,
        MarkdownElementTypes.UNORDERED_LIST -> {
            CompositionLocalProvider(
                LocalJBMarkdownTreeState provides LocalJBMarkdownTreeState.current.copy(
                    listDepth = LocalJBMarkdownTreeState.current.listDepth + 1
                )
            ) {
                JBMText(node, modifier)
            }
        }

        else -> {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFFF7F50))) {
                        append("[Unknown block type ${node.type.name}]")
                    }
                },
                modifier = modifier
            )
        }
    }
}