package chat.revolt.components.screens.chat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import chat.revolt.api.schemas.ChannelType
import chat.revolt.R

@Composable
fun ChannelIcon(
    channelType: ChannelType,
    modifier: Modifier = Modifier,
) {
    when (channelType) {
        ChannelType.TextChannel -> {
            Icon(
                painter = painterResource(R.drawable.ic_pound_24dp),
                contentDescription = stringResource(R.string.channel_text),
                modifier = modifier,
            )
        }
        ChannelType.VoiceChannel -> {
            Icon(
                painter = painterResource(R.drawable.ic_volume_up_24dp),
                contentDescription = stringResource(R.string.channel_voice),
                modifier = modifier,
            )
        }
        ChannelType.SavedMessages -> {
            Icon(
                painter = painterResource(R.drawable.ic_note_24dp),
                contentDescription = stringResource(R.string.channel_notes),
                modifier = modifier,
            )
        }
        ChannelType.DirectMessage -> {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = stringResource(R.string.channel_dm),
                modifier = modifier,
            )
        }
        ChannelType.Group -> {
            Icon(
                imageVector = Icons.Default.AccountBox,
                contentDescription = stringResource(R.string.channel_group),
                modifier = modifier,
            )
        }
    }
}