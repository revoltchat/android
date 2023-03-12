package chat.revolt.internals.markdown

import chat.revolt.api.schemas.Emoji
import chat.revolt.api.schemas.User
import com.discord.simpleast.core.node.Node
import com.discord.simpleast.core.parser.Parser

typealias MarkdownParser = Parser<MarkdownContext, Node<MarkdownContext>, MarkdownState>

data class MarkdownState(val currentQuoteDepth: Int) {
    fun newQuoteDepth(depth: Int): MarkdownState = MarkdownState(depth)
}

data class MarkdownContext(
    val memberMap: Map<String, String>,
    val userMap: Map<String, User>,
    val channelMap: Map<String, String>,
    val emojiMap: Map<String, Emoji>,
    val serverId: String?
)