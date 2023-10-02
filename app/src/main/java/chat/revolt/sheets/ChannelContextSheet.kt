package chat.revolt.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.components.generic.SheetClickable
import chat.revolt.internals.Platform
import kotlinx.coroutines.launch

@Composable
fun ChannelContextSheet(
    channelId: String,
    onHideSheet: suspend () -> Unit,
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

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    Column {
        SheetClickable(
            icon = { modifier ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_content_copy_id_24dp),
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.channel_context_sheet_actions_copy_id),
                    style = style
                )
            },
        ) {
            if (channel.id == null) return@SheetClickable

            clipboardManager.setText(AnnotatedString(channel.id))

            if (Platform.needsShowClipboardNotification()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.channel_context_sheet_actions_copy_id_copied),
                    Toast.LENGTH_SHORT
                ).show()
            }

            coroutineScope.launch {
                onHideSheet()
            }
        }

        SheetClickable(
            icon = { modifier ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_eye_check_24dp),
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.channel_context_sheet_actions_mark_read),
                    style = style
                )
            },
        ) {
            coroutineScope.launch {
                channel.lastMessageID?.let {
                    RevoltAPI.unreads.markAsRead(channelId, it, sync = true)
                }
                onHideSheet()
            }
        }
    }
}