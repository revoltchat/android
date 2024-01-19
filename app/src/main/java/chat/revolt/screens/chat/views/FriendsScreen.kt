package chat.revolt.screens.chat.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.internals.FriendRequests
import chat.revolt.api.routes.user.unfriendUser
import chat.revolt.api.schemas.User
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.components.generic.PageHeader
import chat.revolt.components.generic.SheetClickable
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.presenceFromStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun FriendsOptionsSheet(onDenyAll: () -> Unit) {
    SheetClickable(
        icon = { modifier ->
            Icon(
                modifier = modifier,
                painter = painterResource(R.drawable.ic_account_cancel_24dp),
                contentDescription = null
            )
        },
        label = { style ->
            Text(
                text = stringResource(R.string.friends_deny_all_incoming),
                style = style
            )
        },
        onClick = { onDenyAll() }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FriendsScreen(useDrawer: Boolean, onDrawerClicked: () -> Unit) {
    var optionsSheetShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (optionsSheetShown) {
        val sheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            onDismissRequest = {
                optionsSheetShown = false
            },
            sheetState = sheetState
        ) {
            FriendsOptionsSheet(
                onDenyAll = {
                    scope.launch {
                        sheetState.hide()
                    }
                    with(Dispatchers.IO) {
                        scope.launch {
                            FriendRequests.getIncoming()
                                .forEach { it.id?.let { id -> unfriendUser(id) } }
                        }
                    }
                }
            )
        }
    }

    Column {
        PageHeader(
            text = stringResource(R.string.friends),
            startButtons = {
                if (useDrawer) {
                    IconButton(onClick = onDrawerClicked) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.menu)
                        )
                    }
                }
            },
            additionalButtons = {
                IconButton(onClick = {
                    optionsSheetShown = true
                }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.menu)
                    )
                }
            }
        )

        LazyColumn {
            stickyHeader(key = "incoming") {
                Text(
                    text = AnnotatedString.Builder().apply {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(stringResource(id = R.string.friends_incoming_requests))
                        pop()

                        pushStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = LocalTextStyle.current.fontSize * 0.8,
                                color = LocalContentColor.current.copy(alpha = 0.6f)
                            )
                        )
                        append("—${FriendRequests.getIncoming().size}")
                        pop()
                    }.toAnnotatedString(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(10.dp)
                )
            }

            items(FriendRequests.getIncoming().size) {
                val item = FriendRequests.getIncoming()[it]
                UserItem(item, onClick = {
                    scope.launch {
                        item.id?.let { userId ->
                            ActionChannel.send(Action.OpenUserSheet(userId, null))
                        }
                    }
                })
            }

            stickyHeader(key = "outgoing") {
                Text(
                    text = AnnotatedString.Builder().apply {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(stringResource(id = R.string.friends_outgoing_requests))
                        pop()

                        pushStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = LocalTextStyle.current.fontSize * 0.8,
                                color = LocalContentColor.current.copy(alpha = 0.6f)
                            )
                        )
                        append("—${FriendRequests.getOutgoing().size}")
                        pop()
                    }.toAnnotatedString(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(10.dp)
                )
            }

            items(FriendRequests.getOutgoing().size) {
                val item = FriendRequests.getOutgoing()[it]
                UserItem(item, onClick = {
                    scope.launch {
                        item.id?.let { userId ->
                            ActionChannel.send(Action.OpenUserSheet(userId, null))
                        }
                    }
                })
            }

            stickyHeader(key = "online") {
                Text(
                    text = AnnotatedString.Builder().apply {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(stringResource(id = R.string.status_online))
                        pop()

                        pushStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = LocalTextStyle.current.fontSize * 0.8,
                                color = LocalContentColor.current.copy(alpha = 0.6f)
                            )
                        )
                        append("—${FriendRequests.getOnlineFriends().size}")
                        pop()
                    }.toAnnotatedString(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(10.dp)
                )
            }

            items(FriendRequests.getOnlineFriends().size) {
                val item = FriendRequests.getOnlineFriends()[it]
                UserItem(item, onClick = {
                    scope.launch {
                        item.id?.let { userId ->
                            ActionChannel.send(Action.OpenUserSheet(userId, null))
                        }
                    }
                })
            }

            stickyHeader(key = "not_online") {
                Text(
                    text = AnnotatedString.Builder().apply {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(stringResource(id = R.string.friends_all))
                        pop()

                        pushStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = LocalTextStyle.current.fontSize * 0.8,
                                color = LocalContentColor.current.copy(alpha = 0.6f)
                            )
                        )
                        append("—${FriendRequests.getFriends(true).size}")
                        pop()
                    }.toAnnotatedString(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(10.dp)
                )
            }

            items(FriendRequests.getFriends(true).size) {
                val item = FriendRequests.getFriends(true)[it]
                UserItem(item, onClick = {
                    scope.launch {
                        item.id?.let { userId ->
                            ActionChannel.send(Action.OpenUserSheet(userId, null))
                        }
                    }
                })
            }

            stickyHeader(key = "blocked") {
                Text(
                    text = AnnotatedString.Builder().apply {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(stringResource(id = R.string.friends_blocked))
                        pop()

                        pushStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = LocalTextStyle.current.fontSize * 0.8,
                                color = LocalContentColor.current.copy(alpha = 0.6f)
                            )
                        )
                        append("—${FriendRequests.getBlocked().size}")
                        pop()
                    }.toAnnotatedString(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(10.dp)
                )
            }


            items(FriendRequests.getBlocked().size) {
                val item = FriendRequests.getBlocked()[it]
                UserItem(item, onClick = {
                    scope.launch {
                        item.id?.let { userId ->
                            ActionChannel.send(Action.OpenUserSheet(userId, null))
                        }
                    }
                })
            }
        }
    }
}

@Composable
fun UserItem(user: User, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            username = user.displayName
                ?: user.username
                ?: user.id!!,
            avatar = user.avatar,
            userId = user.id!!,
            presence = presenceFromStatus(
                user.status?.presence,
                user.online ?: false
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = user.displayName
                ?: user.username
                ?: user.id,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}