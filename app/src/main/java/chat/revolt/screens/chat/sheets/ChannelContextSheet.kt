package chat.revolt.screens.chat.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.components.generic.SheetClickable
import kotlinx.coroutines.launch

@Composable
fun ChannelContextSheet(
    navController: NavController,
    channelId: String,
) {
    val channel = RevoltAPI.channelCache[channelId]
    if (channel == null) {
        navController.popBackStack()
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
            Toast.makeText(
                context,
                context.getString(R.string.channel_context_sheet_actions_copy_id_copied),
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
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
            }
            navController.popBackStack()
        }
    }
}