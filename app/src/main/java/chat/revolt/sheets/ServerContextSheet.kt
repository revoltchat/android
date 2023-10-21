package chat.revolt.sheets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.server.leaveOrDeleteServer
import chat.revolt.api.schemas.ServerFlags
import chat.revolt.api.schemas.has
import chat.revolt.components.generic.IconPlaceholder
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.generic.SheetClickable
import chat.revolt.components.generic.UIMarkdown
import chat.revolt.internals.Platform
import kotlinx.coroutines.launch

@Composable
fun ServerContextSheet(serverId: String, onHideSheet: suspend () -> Unit) {
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
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large),
            contentAlignment = Alignment.BottomStart
        ) {
            server.banner?.let {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.25f))
                        .height(166.dp)
                        .fillMaxWidth()
                )

                RemoteImage(
                    url = "$REVOLT_FILES/banners/${it.id}",
                    description = null,
                    modifier = Modifier
                        .height(166.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )

                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .height(166.dp)
                        .fillMaxWidth()
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                server.icon?.let {
                    RemoteImage(
                        url = "$REVOLT_FILES/icons/${it.id}/server.png?max_side=256",
                        description = null,
                        modifier = Modifier
                            .clip(CircleShape)
                            .height(48.dp)
                            .width(48.dp),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    IconPlaceholder(
                        name = server.name ?: stringResource(R.string.unknown),
                        modifier = Modifier
                            .clip(CircleShape)
                            .height(48.dp)
                            .width(48.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    if (server.flags has ServerFlags.Official) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_revolt_decagram_24dp),
                            contentDescription = stringResource(R.string.server_flag_official),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                        )
                    }
                    if (server.flags has ServerFlags.Verified) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check_decagram_24dp),
                            contentDescription = stringResource(R.string.server_flag_verified),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                        )
                    }

                    Text(
                        text = server.name ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.server_context_sheet_category_description),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            UIMarkdown(
                text = if (server.description?.isBlank() == false) {
                    server.description
                } else {
                    stringResource(
                        R.string.server_context_sheet_description_empty
                    )
                }
            )

            Text(
                text = stringResource(id = R.string.server_context_sheet_category_actions),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)
            )
        }

        SheetClickable(icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_content_copy_id_24dp),
                contentDescription = null,
                modifier = it
            )
        }, label = {
            Text(
                text = stringResource(id = R.string.server_context_sheet_actions_copy_id),
                style = it
            )
        }) {
            if (server.id == null) return@SheetClickable

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

        SheetClickable(icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_eye_check_24dp),
                contentDescription = null,
                modifier = it
            )
        }, label = {
            Text(
                text = stringResource(id = R.string.server_context_sheet_actions_mark_read),
                style = it
            )
        }) {
            coroutineScope.launch {
                server.id?.let {
                    RevoltAPI.unreads.markServerAsRead(it, sync = true)
                }
                onHideSheet()
            }
        }

        if (server.owner != RevoltAPI.selfId) {
            SheetClickable(icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_left_bold_box_24dp),
                    contentDescription = null,
                    modifier = it
                )
            }, label = {
                Text(
                    text = stringResource(id = R.string.server_context_sheet_actions_leave),
                    style = it
                )
            }, dangerous = true) {
                showLeaveConfirmation = true
            }
        }
    }
}
