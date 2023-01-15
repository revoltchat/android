package chat.revolt.lettertrees.ast

/*
 * SPECS:
 * - Unist, the universal syntax tree @ https://github.com/syntax-tree/unist
 * - Mdast, the markdown syntax tree @ https://github.com/syntax-tree/mdast
 */

open class Literal(
    /**
     * The value of a node.
     */
    open val value: String = ""
) : Node()

open class Parent(
    /**
     * List representing the children of a node.
     */
    val children: List<Node> = emptyList()
) : Node()

open class Root(
    override val type: String = "root"
) : Parent()

open class Paragraph(
    override val type: String = "paragraph"
) : Parent()

open class Text(
    override val type: String = "text",
    override val value: String = ""
) : Literal()

open class Heading(
    override val type: String = "heading",
    val depth: Int = 1
) : Parent()

open class ThematicBreak(
    override val type: String = "thematicBreak"
) : Node()

open class Blockquote(
    override val type: String = "blockquote"
) : Parent()

open class MdList(
    override val type: String = "list",
    val ordered: Boolean = false,
    val start: Int = 1,
    val spread: Boolean = false
) : Parent()

open class ListItem(
    override val type: String = "listItem",
    val spread: Boolean = false
) : Parent()

open class Code(
    override val type: String = "code",
    val lang: String? = null,
    val meta: String? = null
) : Literal()

open class Emphasis(
    override val type: String = "emphasis"
) : Parent()

open class Strong(
    override val type: String = "strong"
) : Parent()

open class Delete(
    override val type: String = "delete"
) : Parent()

open class InlineCode(
    override val type: String = "inlineCode"
) : Literal()

open class Break(
    override val type: String = "break"
) : Node()

open class Link(
    override val type: String = "link",
    val title: String? = null,
    val url: String = ""
) : Parent()