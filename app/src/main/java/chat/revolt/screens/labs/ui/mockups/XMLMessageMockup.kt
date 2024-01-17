package chat.revolt.screens.labs.ui.mockups

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.Message
import chat.revolt.components.chat.Message
import chat.revolt.internals.markdown.MarkdownContext
import chat.revolt.internals.markdown.MarkdownParser
import chat.revolt.internals.markdown.MarkdownState
import chat.revolt.internals.markdown.addRevoltRules
import chat.revolt.internals.markdown.createCodeRule
import chat.revolt.internals.markdown.createInlineCodeRule
import chat.revolt.views.MessageView
import com.discord.simpleast.core.simple.SimpleMarkdownRules
import com.discord.simpleast.core.simple.SimpleRenderer

@Composable
fun XMLMessageMockup() {
    var message by remember { mutableStateOf<Message?>(null) }
    val context = LocalContext.current
    val codeBlockColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)

    fun reroll() {
        message = RevoltAPI.messageCache.values.random()
    }

    LaunchedEffect(Unit) {
        reroll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        message?.let {
            AndroidView(
                factory = {
                    MessageView(it, onLongPress = {
                        Toast.makeText(context, "Long pressed!", Toast.LENGTH_SHORT).show()
                    })
                },
                update = {
                    it.fromMessage(message!!)
                },
                modifier = Modifier
                    .fillMaxWidth()
            )

            Message(
                message = message!!.copy(tail = false),
                truncate = false,
                onMessageContextMenu = {
                    Toast.makeText(context, "Context menu!", Toast.LENGTH_SHORT).show()
                },
                parse = {
                    val parser = MarkdownParser()
                        .addRules(
                            SimpleMarkdownRules.createEscapeRule()
                        )
                        .addRevoltRules(context)
                        .addRules(
                            createCodeRule(context, codeBlockColor.toArgb()),
                            createInlineCodeRule(
                                context,
                                codeBlockColor.toArgb()
                            )
                        )
                        .addRules(
                            SimpleMarkdownRules.createSimpleMarkdownRules(
                                includeEscapeRule = false
                            )
                        )

                    SimpleRenderer.render(
                        source = it.content ?: "",
                        parser = parser,
                        initialState = MarkdownState(0),
                        renderContext = MarkdownContext(
                            memberMap = mapOf(),
                            userMap = RevoltAPI.userCache.toMap(),
                            channelMap = RevoltAPI.channelCache.mapValues { ch ->
                                ch.value.name ?: ch.value.id
                                ?: "#DeletedChannel"
                            },
                            emojiMap = RevoltAPI.emojiCache,
                            serverId = message!!.channel?.let { x -> RevoltAPI.channelCache[x] }?.server
                                ?: "",
                            // check if message consists solely of one *or more* custom emotes
                            useLargeEmojis = it.content?.matches(
                                Regex("(:([0-9A-Z]{26}):)+")
                            ) == true
                        )
                    )
                },
            )

            TextButton(onClick = { reroll() }) {
                Text("Different message")
            }
        }
    }
}