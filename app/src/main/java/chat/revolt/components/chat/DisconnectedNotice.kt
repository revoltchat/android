package chat.revolt.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.realtime.DisconnectionState

@Composable
private fun DisconnectedNoticeBase(
    background: Color,
    icon: ImageVector,
    text: String,
    canTapToRetry: Boolean = false,
    onRetry: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clickable(enabled = canTapToRetry, onClick = onRetry),
    ) {
        Icon(
            modifier = Modifier.padding(end = 8.dp),
            imageVector = icon,
            contentDescription = null
        )
        Text(
            text = text,
            fontWeight = FontWeight.Bold
        )
        if (canTapToRetry) {
            Text(
                text = stringResource(R.string.tap_to_reconnect),
                modifier = Modifier.padding(start = 8.dp),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun DisconnectedNotice(
    state: DisconnectionState,
    onReconnect: () -> Unit
) {
    when (state) {
        DisconnectionState.Disconnected -> DisconnectedNoticeBase(
            background = Color(0xfffe4654),
            icon = Icons.Default.Warning,
            text = stringResource(id = R.string.disconnected),
            canTapToRetry = true,
            onRetry = onReconnect
        )
        DisconnectionState.Reconnecting -> DisconnectedNoticeBase(
            background = Color(0xfffcb205),
            icon = Icons.Default.Refresh,
            text = stringResource(id = R.string.reconnecting),
        )
        DisconnectionState.Connected -> DisconnectedNoticeBase(
            background = Color(0xff4b9f6a),
            icon = Icons.Default.Done,
            text = stringResource(id = R.string.reconnected),
        )
    }
}