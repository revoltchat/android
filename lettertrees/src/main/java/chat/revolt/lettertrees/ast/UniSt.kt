package chat.revolt.lettertrees.ast

/*
 * SPECS:
 * - Unist, the universal syntax tree @ https://github.com/syntax-tree/unist
 * - Mdast, the markdown syntax tree @ https://github.com/syntax-tree/mdast
 */

class Point(
    /**
     * Line number in the document, starting at 1.
     */
    val line: Int = 1,

    /**
     * Column on line in the document, starting at 1.
     */
    val column: Int = 1,

    /**
     * Character offset in the document, starting at 0.
     */
    val offset: Int = 0
)

class Position(
    /**
     * Place of the first character of the parsed source region.
     */
    val start: Point = Point(),

    /**
     * Place of the first character after the parsed source region.
     */
    val end: Point = Point(),

    /**
     * Start column at each index (plus start line) in the source region,
     * for elements that span multiple lines.
     */
    val indent: List<Int> = emptyList()
)

open class Node(
    /**
     * The variant of the node.
     */
    open val type: String = "",

    /**
     * Information from the ecosystem.
     */
    val data: Map<String, Any> = emptyMap(),

    /**
     * Location of the node in a source document.
     * Must not be present if a node is generated.
     */
    val position: Position = Position()
)

open class UnistParent(
    /**
     * List representing the children of a node.
     */
    val children: List<Node> = emptyList()
) : Node()
