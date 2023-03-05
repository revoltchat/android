package chat.revolt.components.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.RevoltTweenFloat
import chat.revolt.RevoltTweenInt
import chat.revolt.api.RevoltAPI
import chat.revolt.components.generic.UserAvatar

@Composable
fun StackedUserAvatars(
    users: List<String>,
    amount: Int = 3,
) {
    Box(
        modifier = Modifier
            .size(16.dp + (8.dp * minOf(users.size, amount)), 16.dp)
    ) {
        users.take(amount).forEachIndexed { index, userId ->
            val user = RevoltAPI.userCache[userId]
            UserAvatar(
                avatar = user?.avatar,
                userId = userId,
                username = user?.username ?: stringResource(id = R.string.unknown),
                size = 16.dp,
                modifier = Modifier
                    .offset(
                        x = (index * 8).dp,
                    ),
            )
        }
    }
}

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
                .padding(top = 4.dp, start = 16.dp, end = 16.dp),
        ) {
            StackedUserAvatars(users = users)

            Text(
                text = stringResource(
                    id = typingMessageResource(),
                    users.joinToString {
                        RevoltAPI.userCache[it]?.let { u ->
                            u.username ?: u.id
                        } ?: it
                    }
                ),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}