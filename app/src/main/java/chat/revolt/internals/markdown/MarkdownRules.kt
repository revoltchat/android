package chat.revolt.internals.markdown

import android.content.Context
import android.text.style.BackgroundColorSpan
import android.text.style.TextAppearanceSpan
import chat.revolt.R
import com.discord.simpleast.code.CodeRules
import com.discord.simpleast.code.CodeStyleProviders
import com.discord.simpleast.core.node.Node
import com.discord.simpleast.core.node.StyleNode
import com.discord.simpleast.core.parser.ParseSpec
import com.discord.simpleast.core.parser.Parser
import com.discord.simpleast.core.parser.Rule
import java.util.regex.Matcher
import java.util.regex.Pattern

class UserMentionRule<S> :
    Rule<MarkdownContext, UserMentionNode, S>(Pattern.compile("^<@([0-9A-Z]{26})>")) {
    override fun parse(
        matcher: Matcher,
        parser: Parser<MarkdownContext, in UserMentionNode, S>,
        state: S
    ): ParseSpec<MarkdownContext, S> {
        return ParseSpec.createTerminal(UserMentionNode(matcher.group(1)!!), state)
    }
}

class ChannelMentionRule<S> :
    Rule<MarkdownContext, ChannelMentionNode, S>(Pattern.compile("^<#([0-9A-Z]{26})>")) {
    override fun parse(
        matcher: Matcher,
        parser: Parser<MarkdownContext, in ChannelMentionNode, S>,
        state: S
    ): ParseSpec<MarkdownContext, S> {
        return ParseSpec.createTerminal(ChannelMentionNode(matcher.group(1)!!), state)
    }
}

class CustomEmoteRule<S> :
    Rule<MarkdownContext, CustomEmoteNode, S>(Pattern.compile("^:([0-9A-Z]{26}):")) {
    override fun parse(
        matcher: Matcher,
        parser: Parser<MarkdownContext, in CustomEmoteNode, S>,
        state: S
    ): ParseSpec<MarkdownContext, S> {
        return ParseSpec.createTerminal(CustomEmoteNode(matcher.group(1)!!), state)
    }
}

fun <RC, S> createInlineCodeRule(context: Context, backgroundColor: Int): Rule<RC, Node<RC>, S> {
    return CodeRules.createInlineCodeRule(
        { listOf(TextAppearanceSpan(context, R.style.Code_TextAppearance)) },
        { listOf(BackgroundColorSpan(backgroundColor)) }
    )
}

fun <RC, S : MarkdownState> createCodeRule(
    context: Context,
    backgroundColor: Int
): Rule<RC, Node<RC>, S> {
    val codeStyleProviders = CodeStyleProviders<RC>(
        defaultStyleProvider = { listOf(TextAppearanceSpan(context, R.style.Code_TextAppearance)) },
        commentStyleProvider = {
            listOf(
                TextAppearanceSpan(
                    context,
                    R.style.Code_TextAppearance_Comment
                )
            )
        },
        literalStyleProvider = {
            listOf(
                TextAppearanceSpan(
                    context,
                    R.style.Code_TextAppearance_Literal
                )
            )
        },
        keywordStyleProvider = {
            listOf(
                TextAppearanceSpan(
                    context,
                    R.style.Code_TextAppearance_Keyword
                )
            )
        },
        identifierStyleProvider = {
            listOf(
                TextAppearanceSpan(
                    context,
                    R.style.Code_TextAppearance_Identifier
                )
            )
        },
        typesStyleProvider = {
            listOf(
                TextAppearanceSpan(
                    context,
                    R.style.Code_TextAppearance_Types
                )
            )
        },
        genericsStyleProvider = {
            listOf(
                TextAppearanceSpan(
                    context,
                    R.style.Code_TextAppearance_Generics
                )
            )
        },
        paramsStyleProvider = {
            listOf(
                TextAppearanceSpan(
                    context,
                    R.style.Code_TextAppearance_Params
                )
            )
        },
    )
    val languageMap = CodeRules.createCodeLanguageMap<RC, S>(codeStyleProviders)

    return CodeRules.createCodeRule(
        codeStyleProviders.defaultStyleProvider,
        languageMap
    ) { codeNode, block, state ->
        if (!block) {
            StyleNode<RC, Any>(listOf(BackgroundColorSpan(backgroundColor)))
                .apply { addChild(codeNode) }
        } else {
            BlockBackgroundNode(
                state.currentQuoteDepth,
                backgroundColor,
                backgroundColor,
                codeNode
            )
        }
    }
}