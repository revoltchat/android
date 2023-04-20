package chat.revolt.screens.chat.sheets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.REVOLT_APP
import chat.revolt.api.RevoltAPI
import chat.revolt.callbacks.UiCallbacks
import chat.revolt.components.chat.Message
import chat.revolt.components.generic.SheetClickable
import kotlinx.coroutines.launch

@Composable
fun MessageContextSheet(
    navController: NavController,
    messageId: String,
) {
    val message = RevoltAPI.messageCache[messageId]
    if (message == null) {
        navController.popBackStack()
        return
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                .padding(bottom = 8.dp)
        ) {
            Message(
                message = message.copy(
                    tail = false,
                    masquerade = null
                ),
                truncate = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SheetClickable(
            icon = { modifier ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_reply_24dp),
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.message_context_sheet_actions_reply),
                    style = style
                )
            },
        ) {
            coroutineScope.launch {
                UiCallbacks.replyToMessage(messageId)
            }
            navController.popBackStack()
        }

        SheetClickable(
            icon = { modifier ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_hamburger_plus_24dp),
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.message_context_sheet_actions_react),
                    style = style
                )
            },
        ) {
            Toast.makeText(
                context,
                context.getString(R.string.comingsoon_toast),
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }

        SheetClickable(
            icon = { modifier ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_content_copy_24dp),
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.message_context_sheet_actions_copy),
                    style = style
                )
            },
        ) {
            if (message.content.isNullOrEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.message_context_sheet_actions_copy_failed_empty),
                    Toast.LENGTH_SHORT
                ).show()
                navController.popBackStack()
                return@SheetClickable
            }

            clipboardManager.setText(AnnotatedString(message.content))
            Toast.makeText(
                context,
                context.getString(R.string.copied),
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }

        SheetClickable(
            icon = { modifier ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_link_variant_24dp),
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.message_context_sheet_actions_copy_link),
                    style = style
                )
            },
        ) {
            if (message.content.isNullOrEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.message_context_sheet_actions_copy_failed_empty),
                    Toast.LENGTH_SHORT
                ).show()
                navController.popBackStack()
                return@SheetClickable
            }

            val server = RevoltAPI.serverCache.values.find { server ->
                server.channels?.contains(message.channel) ?: false
            }
            val messageLink =
                "$REVOLT_APP/server/${server?.id}/channel/${message.channel}/${message.id}"

            clipboardManager.setText(AnnotatedString(messageLink))
            Toast.makeText(
                context,
                context.getString(R.string.message_context_sheet_actions_copy_link_copied),
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }

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
                    text = stringResource(id = R.string.message_context_sheet_actions_copy_id),
                    style = style
                )
            },
        ) {
            if (message.id == null) return@SheetClickable

            clipboardManager.setText(AnnotatedString(message.id))
            Toast.makeText(
                context,
                context.getString(R.string.message_context_sheet_actions_copy_id_copied),
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }


        SheetClickable(
            icon = { modifier ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_eye_off_24dp),
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.message_context_sheet_actions_mark_unread),
                    style = style
                )
            },
        ) {
            Toast.makeText(
                context,
                context.getString(R.string.comingsoon_toast),
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }

        SheetClickable(
            icon = { modifier ->
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.message_context_sheet_actions_edit),
                    style = style
                )
            },
        ) {
            Toast.makeText(
                context,
                context.getString(R.string.comingsoon_toast),
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }

        SheetClickable(
            icon = { modifier ->
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.message_context_sheet_actions_delete),
                    style = style
                )
            },
        ) {
            Toast.makeText(
                context,
                context.getString(R.string.comingsoon_toast),
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }

        SheetClickable(
            icon = { modifier ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_flag_24dp),
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.message_context_sheet_actions_report),
                    style = style
                )
            },
        ) {
            navController.navigate("report/message/${message.id}")
        }
    }
}