package chat.revolt.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import chat.revolt.api.routes.server.leaveOrDeleteServer
import chat.revolt.components.generic.SheetButton
import chat.revolt.components.generic.SheetEnd
import chat.revolt.components.markdown.RichMarkdown
import chat.revolt.components.screens.settings.ServerOverview
import chat.revolt.internals.Platform
import kotlinx.coroutines.launch

@Composable
fun ServerContextSheet(
    serverId: String,
    onReportServer: () -> Unit,
    onHideSheet: suspend () -> Unit
) {
    val server = RevoltAPI.serverCache[serverId]

    if (server == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var showLeaveConfirmation by remember { mutableStateOf(false) }
    var leaveSilently by remember { mutableStateOf(false) }

    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showLeaveConfirmation = false
            },
            title = {
                Text(
                    text = stringResource(
                        id = R.string.server_context_sheet_actions_leave_confirm,
                        server.name ?: stringResource(R.string.unknown)
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            id = R.string.server_context_sheet_actions_leave_confirm_eyebrow
                        )
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 0.dp, end = 0.dp, top = 16.dp, bottom = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = leaveSilently,
                            onCheckedChange = { leaveSilently = it }
                        )
                        Text(
                            text = stringResource(
                                id = R.string.server_context_sheet_actions_leave_silently
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            onHideSheet()
                        }
                        coroutineScope.launch {
                            leaveOrDeleteServer(serverId, leaveSilently)
                        }
                    }
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.server_context_sheet_actions_leave_confirm_yes
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirmation = false
                    }
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.server_context_sheet_actions_leave_confirm_no
                        )
                    )
                }
            }
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
    ) {
        ServerOverview(server)

        SelectionContainer {
            RichMarkdown(
                input = if (server.description?.isBlank() == false) {
                    server.description
                } else {
                    stringResource(
                        R.string.server_context_sheet_description_empty
                    )
                }
            )
        }

        HorizontalDivider()
    }

    SheetButton(
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.ic_content_copy_id_24dp),
                contentDescription = null
            )
        },
        headlineContent = {
            Text(
                text = stringResource(id = R.string.server_context_sheet_actions_copy_id)
            )
        },
        onClick = {
            if (server.id == null) return@SheetButton

            clipboardManager.setText(AnnotatedString(server.id))

            if (Platform.needsShowClipboardNotification()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.server_context_sheet_actions_copy_id_copied),
                    Toast.LENGTH_SHORT
                ).show()
            }

            coroutineScope.launch {
                onHideSheet()
            }
        }
    )

    SheetButton(
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.ic_eye_check_24dp),
                contentDescription = null
            )
        },
        headlineContent = {
            Text(
                text = stringResource(id = R.string.server_context_sheet_actions_mark_read)
            )
        },
        onClick = {
            coroutineScope.launch {
                server.id?.let {
                    RevoltAPI.unreads.markServerAsRead(it, sync = true)
                }
                onHideSheet()
            }
        }
    )

    if (server.owner != RevoltAPI.selfId) {
        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_flag_24dp),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.server_context_sheet_actions_report),
                )
            },
            dangerous = true,
            onClick = {
                onReportServer()
            }
        )

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_left_bold_box_24dp),
                    contentDescription = null,
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.server_context_sheet_actions_leave)
                )
            },
            dangerous = true,
            onClick = {
                showLeaveConfirmation = true
            }
        )
    }

    SheetEnd()
}
