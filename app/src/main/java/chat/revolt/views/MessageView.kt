package chat.revolt.views

import android.content.Context
import android.icu.text.DateFormat
import android.text.format.DateUtils
import androidx.constraintlayout.widget.ConstraintLayout
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.Roles
import chat.revolt.api.internals.TextViewCompat
import chat.revolt.api.internals.ULID
import chat.revolt.api.schemas.Message
import chat.revolt.api.schemas.User
import chat.revolt.databinding.ViewMessageBinding
import chat.revolt.internals.markdown.MarkdownContext
import chat.revolt.internals.markdown.MarkdownParser
import chat.revolt.internals.markdown.MarkdownState
import chat.revolt.internals.markdown.addRevoltRules
import chat.revolt.internals.markdown.createCodeRule
import chat.revolt.internals.markdown.createInlineCodeRule
import com.bumptech.glide.GenericTransitionOptions
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.discord.simpleast.core.simple.SimpleMarkdownRules
import com.discord.simpleast.core.simple.SimpleRenderer
import com.google.android.material.color.MaterialColors
import com.google.android.material.elevation.SurfaceColors


class MessageView(
    ctx: Context
) : ConstraintLayout(ctx) {
    private var binding: ViewMessageBinding
    private val parser = MarkdownParser()
        .addRules(
            SimpleMarkdownRules.createEscapeRule()
        )
        .addRevoltRules(context)
        .addRules(
            createCodeRule(context, SurfaceColors.SURFACE_2.getColor(ctx)),
            createInlineCodeRule(
                context,
                SurfaceColors.SURFACE_2.getColor(ctx),
            )
        )
        .addRules(
            SimpleMarkdownRules.createSimpleMarkdownRules(
                includeEscapeRule = false
            )
        )

    private var messageServer: String? = null

    constructor(
        ctx: Context,
        onLongPress: (() -> Unit)? = null
    ) : this(ctx) {
        binding.root.setOnLongClickListener {
            onLongPress?.invoke()
            onLongPress != null
        }
    }

    init {
        inflate(ctx, R.layout.view_message, this)
        binding = ViewMessageBinding.bind(this)
    }

    fun setAuthor(author: String) {
        binding.author.text = author
    }

    fun setContent(content: String) {
        binding.messageContent.text = SimpleRenderer.render(
            source = content,
            parser = parser,
            initialState = MarkdownState(0),
            renderContext = MarkdownContext(
                memberMap = messageServer?.let { RevoltAPI.members.markdownMemberMapFor(it) }
                    ?: mapOf(),
                userMap = RevoltAPI.userCache.toMap(),
                channelMap = RevoltAPI.channelCache.mapValues { ch ->
                    ch.value.name ?: ch.value.id
                    ?: "#DeletedChannel"
                },
                emojiMap = RevoltAPI.emojiCache,
                serverId = messageServer,
                // check if message consists solely of one *or more* custom emotes
                useLargeEmojis = content.matches(
                    Regex("(:([0-9A-Z]{26}):)+")
                )
            )
        )
    }

    fun setTimestamp(timestamp: String) {
        binding.timestamp.text = timestamp
    }

    fun setAvatarUrl(avatar: String?) {
        if (avatar == null) {
            Glide.with(this).clear(binding.avatar)
        }

        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()

        Glide.with(this).load(avatar).diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(GenericTransitionOptions.with(factory))
            .circleCrop()
            .into(binding.avatar)
    }

    private fun formatLongAsTime(time: Long): String {
        val date = java.util.Date(time)

        val withinLastWeek = System.currentTimeMillis() - time < 604800000

        return if (withinLastWeek) {
            val relativeDate = DateUtils.getRelativeTimeSpanString(
                time,
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL
            )
            val relativeTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(date)

            "$relativeDate $relativeTime"
        } else {
            val absoluteDate = DateFormat.getDateInstance(DateFormat.SHORT).format(date)
            val absoluteTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(date)

            "$absoluteDate $absoluteTime"
        }
    }

    private fun authorName(message: Message): String {
        if (message.masquerade?.name != null) {
            return message.masquerade.name
        }

        messageServer
            ?: return RevoltAPI.userCache[message.author]?.let { User.resolveDefaultName(it) }
                ?: context.getString(R.string.unknown)

        val member = messageServer?.let { sid ->
            message.author?.let {
                RevoltAPI.members.getMember(
                    sid,
                    it
                )
            }
        }
            ?: return context.getString(R.string.unknown)

        return member.nickname
            ?: RevoltAPI.userCache[message.author]?.let { User.resolveDefaultName(it) }
            ?: context.getString(R.string.unknown)
    }

    private fun resetAuthorColour() {
        binding.author.setTextColor(
            MaterialColors.getColor(
                binding.author,
                com.google.android.material.R.attr.colorOnBackground
            )
        )
        binding.author.paint.shader = null
    }

    private fun setAuthorColour(message: Message) {
        resetAuthorColour()

        if (message.masquerade?.colour != null) {
            TextViewCompat.setColourFromRoleColour(binding.author, message.masquerade.colour)
        } else {
            val serverId = RevoltAPI.channelCache[message.channel]?.server ?: return

            val highestRole = message.author?.let {
                Roles.resolveHighestRole(serverId, it, withColour = true)
            } ?: return

            highestRole.colour?.let {
                TextViewCompat.setColourFromRoleColour(binding.author, it)
            }
        }
    }

    fun fromMessage(message: Message) {
        messageServer = RevoltAPI.channelCache[message.channel]?.server

        message.content?.let { setContent(it) }
        message.id?.let { setTimestamp(formatLongAsTime(ULID.asTimestamp(it))) }
        // dont have this
        val resolvedAuthor = RevoltAPI.userCache[message.author]
        // dont inline this
        setAvatarUrl(resolvedAuthor?.avatar?.let { "$REVOLT_FILES/avatars/${it.id}?max_side=256" }
            ?: "")
        setAuthor(authorName(message))

        setAuthorColour(message)
    }

    fun reset() {
        resetAuthorColour()
        setContent("")
        setTimestamp("")
        setAuthor("")
        setAvatarUrl(null)
    }
}