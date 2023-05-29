package chat.revolt.sheets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.ChannelType
import chat.revolt.components.generic.SheetClickable
import chat.revolt.components.screens.chat.ChannelSheetHeader

@Composable
fun ChannelInfoSheet(
    channelId: String,
) {
    val channel = RevoltAPI.channelCache[channelId]
    if (channel == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        ChannelSheetHeader(
            channelName = channel.name ?: stringResource(id = R.string.unknown),
            channelIcon = channel.icon,
            channelType = channel.channelType ?: ChannelType.TextChannel,
            channelId = channel.id ?: "9",
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.channel_info_sheet_description),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Text(
            text = channel.description
                ?: stringResource(id = R.string.channel_info_sheet_description_empty),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.channel_info_sheet_options),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        SheetClickable(
            icon = { modifier ->
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.channel_info_sheet_options_members),
                    style = style
                )
            }
        ) {

        }

        SheetClickable(
            icon = { modifier ->
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.channel_info_sheet_options_invite),
                    style = style
                )
            }
        ) {

        }

        SheetClickable(
            icon = { modifier ->
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.channel_info_sheet_options_notifications_manage),
                    style = style
                )
            }
        ) {

        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}