package chat.revolt.components.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import chat.revolt.R

enum class InlineBadge {
    Bot,
    Bridge,
    PlatformModeration,
    Developer
}

@Composable
fun InlineBadge(
    badge: InlineBadge,
    modifier: Modifier = Modifier,
    colour: Color = Color.Unspecified,
) {
    when (badge) {
        InlineBadge.Bot -> Icon(
            painter = painterResource(id = R.drawable.ic_robot_24dp),
            contentDescription = stringResource(id = R.string.badge_bot_alt),
            tint = colour,
            modifier = modifier
        )
        InlineBadge.Bridge -> Icon(
            painter = painterResource(id = R.drawable.ic_link_variant_24dp),
            contentDescription = stringResource(id = R.string.badge_masquerade_alt),
            tint = colour,
            modifier = modifier
        )
        InlineBadge.PlatformModeration -> TODO()
        InlineBadge.Developer -> TODO()
    }
}

@Composable
fun InlineBadges(
    modifier: Modifier = Modifier,
    bot: Boolean = false,
    bridge: Boolean = false,
    platformModeration: Boolean = false,
    developer: Boolean = false,
    colour: Color = Color.Unspecified,
    precedingIfAny: @Composable () -> Unit = {},
    followingIfAny: @Composable () -> Unit = {},
) {
    val hasBadges = bot || bridge || platformModeration || developer

    if (hasBadges) {
        precedingIfAny()
    }

    Row {
        if (bot) {
            InlineBadge(
                badge = InlineBadge.Bot,
                modifier = modifier,
                colour = colour
            )
        }
        if (bridge) {
            InlineBadge(
                badge = InlineBadge.Bridge,
                modifier = modifier,
                colour = colour
            )
        }
        if (platformModeration) {
            InlineBadge(
                badge = InlineBadge.PlatformModeration,
                modifier = modifier,
                colour = colour
            )
        }
        if (developer) {
            InlineBadge(
                badge = InlineBadge.Developer,
                modifier = modifier,
                colour = colour
            )
        }
    }

    if (hasBadges) {
        followingIfAny()
    }
}