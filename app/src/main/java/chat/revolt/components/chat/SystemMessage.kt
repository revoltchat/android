package chat.revolt.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import chat.revolt.components.generic.UIMarkdown

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
    TEXT("text")
}

fun String?.mention(): String {
    return "<@$this>"
}

@Composable
fun SystemMessage(message: Message) {
    if (message.system == null) return

    val systemMessageType =
        SystemMessageType.entries.firstOrNull { it.type == message.system.type }

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
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .fillMaxWidth()
        ) {
            SystemMessageIconWithBackground(type = systemMessageType)

            Spacer(modifier = Modifier.width(10.dp))

            when (systemMessageType) {
                SystemMessageType.CHANNEL_OWNERSHIP_CHANGED -> {
                    UIMarkdown(
                        stringResource(
                            R.string.system_message_ownership_changed,
                            message.system.from.mention(),
                            message.system.to.mention()
                        )
                    )
                }

                SystemMessageType.CHANNEL_ICON_CHANGED -> {
                    UIMarkdown(
                        stringResource(
                            R.string.system_message_channel_icon_changed,
                            message.system.by.mention()
                        )
                    )
                }

                SystemMessageType.CHANNEL_DESCRIPTION_CHANGED -> {
                    UIMarkdown(
                        stringResource(
                            R.string.system_message_channel_description_changed,
                            message.system.by.mention()
                        )
                    )
                }

                SystemMessageType.CHANNEL_RENAMED -> {
                    UIMarkdown(
                        stringResource(
                            R.string.system_message_channel_renamed,
                            message.system.by.mention(),
                            "**${message.system.name ?: stringResource(R.string.unknown)}**"
                        )
                    )
                }

                SystemMessageType.USER_REMOVE -> {
                    UIMarkdown(
                        stringResource(
                            R.string.system_message_user_removed,
                            message.system.by.mention(),
                            message.system.id.mention()
                        )
                    )
                }

                SystemMessageType.USER_ADDED -> {
                    UIMarkdown(
                        stringResource(
                            R.string.system_message_user_added,
                            message.system.by.mention(),
                            message.system.id.mention()
                        )
                    )
                }

                SystemMessageType.USER_BANNED -> {
                    UIMarkdown(
                        stringResource(
                            R.string.system_message_user_banned,
                            message.system.id.mention()
                        )
                    )
                }

                SystemMessageType.USER_KICKED -> {
                    UIMarkdown(
                        stringResource(
                            R.string.system_message_user_kicked,
                            message.system.id.mention()
                        )
                    )
                }

                SystemMessageType.USER_LEFT -> {
                    UIMarkdown(
                        stringResource(
                            R.string.system_message_user_left,
                            message.system.id.mention()
                        )
                    )
                }

                SystemMessageType.USER_JOINED -> {
                    UIMarkdown(
                        stringResource(
                            R.string.system_message_user_joined,
                            message.system.id.mention()
                        )
                    )
                }

                SystemMessageType.TEXT -> {
                    message.system.content?.let { UIMarkdown(it) }
                }
            }
        }
    }
}

@Composable
fun SystemMessageIcon(type: SystemMessageType, modifier: Modifier = Modifier, size: Dp = 24.dp) {
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
                contentDescription = stringResource(
                    R.string.system_message_channel_icon_changed_alt
                ),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.CHANNEL_DESCRIPTION_CHANGED -> {
            Icon(
                painter = painterResource(R.drawable.ic_text_box_multiple_24dp),
                contentDescription = stringResource(
                    R.string.system_message_channel_description_changed_alt
                ),
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
