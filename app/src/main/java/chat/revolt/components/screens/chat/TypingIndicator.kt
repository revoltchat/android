package chat.revolt.components.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.RevoltTweenFloat
import chat.revolt.RevoltTweenInt
import chat.revolt.api.RevoltAPI

@Composable
fun TypingIndicator(
    users: List<String>,
) {
    fun typingMessageResource(): Int {
        return when (users.size) {
            0 -> R.string.typing_blank
            1 -> R.string.typing_one
            in 2..4 -> R.string.typing_many
            else -> R.string.typing_several
        }
    }

    AnimatedVisibility(
        visible = users.isNotEmpty(),
        enter = slideInVertically(
            animationSpec = RevoltTweenInt,
            initialOffsetY = { it }
        ) + fadeIn(animationSpec = RevoltTweenFloat),
        exit = slideOutVertically(
            animationSpec = RevoltTweenInt,
            targetOffsetY = { it }
        ) + fadeOut(animationSpec = RevoltTweenFloat)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                    )
                )
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(
                    id = typingMessageResource(),
                    users.joinToString {
                        RevoltAPI.userCache[it]?.let { u ->
                            u.username ?: u.id
                        } ?: it
                    }
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}