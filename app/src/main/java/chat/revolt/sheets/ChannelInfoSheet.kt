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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ChannelUtils
import chat.revolt.api.internals.PermissionBit
import chat.revolt.api.internals.Roles
import chat.revolt.api.internals.has
import chat.revolt.api.schemas.ChannelType
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.components.generic.SheetClickable
import chat.revolt.components.screens.chat.ChannelSheetHeader
import chat.revolt.internals.extensions.rememberChannelPermissions
import chat.revolt.screens.chat.dialogs.InviteDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelInfoSheet(channelId: String, onHideSheet: suspend () -> Unit) {
    val channel = RevoltAPI.channelCache[channelId]
    var memberListSheetShown by remember { mutableStateOf(false) }
    var inviteDialogShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val permissions by rememberChannelPermissions(channelId)

    if (memberListSheetShown) {
        val memberListSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = memberListSheetState,
            onDismissRequest = {
                memberListSheetShown = false
            }
        ) {
            MemberListSheet(
                channelId = channelId,
                serverId = channel?.server
            )
        }
    }

    if (inviteDialogShown) {
        Dialog(
            onDismissRequest = {
                inviteDialogShown = false
            }
        ) {
            InviteDialog(
                channelId = channelId,
                onDismissRequest = {
                    inviteDialogShown = false
                }
            )
        }
    }

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
            .verticalScroll(rememberScrollState())
    ) {
        val isDM = ChannelUtils.resolveDMPartner(channel) != null
        val partner = ChannelUtils
            .resolveDMPartner(channel)
            ?.let {
                RevoltAPI.userCache[it]
            }

        ChannelSheetHeader(
            channelName = channel.name
                ?: ChannelUtils.resolveDMName(channel)
                ?: stringResource(id = R.string.unknown),
            channelIcon = channel.icon,
            channelType = channel.channelType ?: ChannelType.TextChannel,
            dmPartner = partner
        )

        if (!isDM) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.channel_info_sheet_description),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Text(
                text = if (channel.description.isNullOrBlank()) {
                    stringResource(
                        id = R.string.channel_info_sheet_description_empty
                    )
                } else {
                    channel.description
                },
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(id = R.string.channel_info_sheet_options),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        when (channel.channelType) {
            ChannelType.TextChannel, ChannelType.VoiceChannel, ChannelType.Group -> {
                SheetClickable(
                    icon = { modifier ->
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.List,
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
                    memberListSheetShown = true
                }
            }

            else -> {}
        }

        if (
            Roles.permissionFor(
                channel,
                RevoltAPI.userCache[RevoltAPI.selfId]
            ) has PermissionBit.InviteOthers
        ) {
            when (channel.channelType) {
                ChannelType.TextChannel, ChannelType.VoiceChannel -> {
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
                        inviteDialogShown = true
                    }
                }

                ChannelType.Group -> {
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
                                text = stringResource(id = R.string.channel_info_sheet_options_add),
                                style = style
                            )
                        }
                    ) {
                    }
                }

                else -> {}
            }
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
                    text = stringResource(
                        id = R.string.channel_info_sheet_options_notifications_manage
                    ),
                    style = style
                )
            }
        ) {
        }

        if (
            (permissions has PermissionBit.ManageChannel || permissions has PermissionBit.ManageRole)
            && (channel.channelType != ChannelType.SavedMessages && channel.channelType != ChannelType.DirectMessage)
        ) {
            SheetClickable(
                icon = { modifier ->
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = modifier
                    )
                },
                label = { style ->
                    Text(
                        text = stringResource(
                            id = R.string.settings
                        ),
                        style = style
                    )
                }
            ) {
                scope.launch {
                    onHideSheet()
                }
                scope.launch {
                    delay(100) // wait for the sheet to close or at least start closing
                    ActionChannel.send(Action.TopNavigate("settings/channel/${channel.id}"))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
