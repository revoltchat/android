package chat.revolt.sheets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.REVOLT_APP
import chat.revolt.api.RevoltAPI
import chat.revolt.callbacks.UiCallbacks
import chat.revolt.components.chat.Message
import chat.revolt.components.generic.SheetClickable
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContextSheet(
    messageId: String,
    onHideSheet: suspend () -> Unit,
    onReportMessage: () -> Unit,
) {
    val message = RevoltAPI.messageCache[messageId]
    if (message == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var showShareSheet by remember { mutableStateOf(false) };

    if (showShareSheet) {
        val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = shareSheetState,
            onDismissRequest = {
                showShareSheet = false
            },
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
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
                        coroutineScope.launch {
                            onHideSheet()
                            Toast.makeText(
                                context,
                                context.getString(R.string.message_context_sheet_actions_copy_failed_empty),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@SheetClickable
                    }

                    Toast.makeText(
                        context,
                        context.getString(R.string.copied),
                        Toast.LENGTH_SHORT
                    ).show()

                    coroutineScope.launch {
                        clipboardManager.setText(AnnotatedString(message.content))
                        onHideSheet()
                    }
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

                        coroutineScope.launch {
                            onHideSheet()
                        }

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

                    coroutineScope.launch {
                        onHideSheet()
                    }
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

                    coroutineScope.launch {
                        onHideSheet()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(0.dp))
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
                onHideSheet()
            }
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

            coroutineScope.launch {
                onHideSheet()
            }
        }

        if (message.author == RevoltAPI.selfId) {
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
                coroutineScope.launch {
                    UiCallbacks.editMessage(messageId)
                    onHideSheet()
                }
            }
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

            coroutineScope.launch {
                onHideSheet()
            }
        }

        SheetClickable(
            icon = { modifier ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_share_24dp),
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.share),
                    style = style
                )
            },
        ) {
            showShareSheet = true
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
            dangerous = true
        ) {
            Toast.makeText(
                context,
                context.getString(R.string.comingsoon_toast),
                Toast.LENGTH_SHORT
            ).show()

            coroutineScope.launch {
                onHideSheet()
            }
        }

        if (message.author != RevoltAPI.selfId) {
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
                dangerous = true
            ) {
                coroutineScope.launch {
                    onReportMessage()
                }
            }
        }
    }
}