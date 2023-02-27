package chat.revolt.screens.chat.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.ChannelType
import chat.revolt.components.generic.PageHeader
import chat.revolt.components.screens.chat.ChannelIcon

@Composable
fun ChannelInfoSheet(
    navController: NavController,
    channelId: String,
) {
    val channel = RevoltAPI.channelCache[channelId]
    if (channel == null) {
        navController.popBackStack()
        return
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelIcon(
                channelType = channel.channelType ?: ChannelType.TextChannel,
                modifier = Modifier.size(32.dp)
            )
            PageHeader(
                text = channel.name ?: channel.id ?: "",
                modifier = Modifier.offset((-4).dp, 0.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
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
    }
}