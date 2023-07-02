package chat.revolt.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.schemas.Message

enum class SystemMessageType(val type: String) {
    CHANNEL_OWNERSHIP_CHANGED("channel_ownership_changed"),
    CHANNEL_ICON_CHANGED("channel_icon_changed"),
    CHANNEL_DESCRIPTION_CHANGED("channel_description_changed"),
    CHANNEL_RENAMED("channel_renamed"),
    USER_REMOVE("user_remove"),
    USER_ADDED("user_added"),
    USER_BANNED("user_banned"),
    USER_KICKED("user_kicked"),
    USER_LEFT("user_left"),
    USER_JOINED("user_joined"),
    TEXT("text"),
}

@Composable
fun SystemMessage(
    message: Message
) {
    if (message.system == null) return

    val systemMessageType =
        SystemMessageType.values().firstOrNull { it.type == message.system.type }

    if (systemMessageType == null) {
        Text(text = message.system.toString())
        return
    }

    CompositionLocalProvider(
        LocalContentColor provides LocalContentColor.current.copy(alpha = 0.7f),
        LocalTextStyle provides LocalTextStyle.current.copy(
            fontWeight = FontWeight.Light
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            SystemMessageIconWithBackground(type = systemMessageType)

            Spacer(modifier = Modifier.width(10.dp))

            when (systemMessageType) {
                SystemMessageType.CHANNEL_OWNERSHIP_CHANGED -> {
                    Text(text = "Channel ownership changed from ${message.system.from} to ${message.system.to}")
                }

                SystemMessageType.CHANNEL_ICON_CHANGED -> {
                    Text(text = "Channel icon changed by ${message.system.by}")
                }

                SystemMessageType.CHANNEL_DESCRIPTION_CHANGED -> {
                    Text(text = "Channel description changed by ${message.system.by}")
                }

                SystemMessageType.CHANNEL_RENAMED -> {
                    Text(text = "Channel renamed to ${message.system.name} by ${message.system.by}")
                }

                SystemMessageType.USER_REMOVE -> {
                    Text(text = "User ${message.system.id} removed by ${message.system.by}")
                }

                SystemMessageType.USER_ADDED -> {
                    Text(text = "User ${message.system.id} added by ${message.system.by}")
                }

                SystemMessageType.USER_BANNED -> {
                    Text(text = "User ${message.system.id} banned")
                }

                SystemMessageType.USER_KICKED -> {
                    Text(text = "User ${message.system.id} kicked")
                }

                SystemMessageType.USER_LEFT -> {
                    Text(text = "User ${message.system.id} left")
                }

                SystemMessageType.USER_JOINED -> {
                    Text(text = "User ${message.system.id} joined")
                }

                SystemMessageType.TEXT -> {
                    message.system.content?.let { Text(text = it) }
                }
            }
        }
    }
}

@Composable
fun SystemMessageIcon(
    type: SystemMessageType,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    when (type) {
        SystemMessageType.CHANNEL_OWNERSHIP_CHANGED -> {
            Icon(
                painter = painterResource(R.drawable.ic_key_arrow_right_24dp),
                contentDescription = stringResource(R.string.system_message_ownership_changed_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.CHANNEL_ICON_CHANGED -> {
            Icon(
                painter = painterResource(R.drawable.ic_image_multiple_24dp),
                contentDescription = stringResource(R.string.system_message_channel_icon_changed_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.CHANNEL_DESCRIPTION_CHANGED -> {
            Icon(
                painter = painterResource(R.drawable.ic_text_box_multiple_24dp),
                contentDescription = stringResource(R.string.system_message_channel_description_changed_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.CHANNEL_RENAMED -> {
            Icon(
                painter = painterResource(R.drawable.ic_cursor_text_24dp),
                contentDescription = stringResource(R.string.system_message_channel_renamed_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.USER_REMOVE -> {
            Icon(
                painter = painterResource(R.drawable.ic_account_cancel_24dp),
                contentDescription = stringResource(R.string.system_message_user_removed_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.USER_ADDED -> {
            Icon(
                painter = painterResource(R.drawable.ic_account_plus_24dp),
                contentDescription = stringResource(R.string.system_message_user_added_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.USER_BANNED -> {
            Icon(
                painter = painterResource(R.drawable.ic_gavel_24dp),
                contentDescription = stringResource(R.string.system_message_user_banned_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.USER_KICKED -> {
            Icon(
                painter = painterResource(R.drawable.ic_shield_24dp),
                contentDescription = stringResource(R.string.system_message_user_kicked_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.USER_LEFT -> {
            Icon(
                painter = painterResource(R.drawable.ic_account_arrow_left_24dp),
                contentDescription = stringResource(R.string.system_message_user_left_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.USER_JOINED -> {
            Icon(
                painter = painterResource(R.drawable.ic_account_arrow_right_24dp),
                contentDescription = stringResource(R.string.system_message_user_joined_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.TEXT -> {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.system_message_text_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }
    }
}

@Composable
fun SystemMessageIconWithBackground(
    type: SystemMessageType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            .size(size)
    ) {
        SystemMessageIcon(type = type)
    }
}