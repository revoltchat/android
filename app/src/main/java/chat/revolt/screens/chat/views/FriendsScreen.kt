package chat.revolt.screens.chat.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import chat.revolt.R
import chat.revolt.api.internals.FriendRequests
import chat.revolt.api.routes.user.unfriendUser
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.components.chat.MemberListItem
import chat.revolt.components.generic.CountableListHeader
import chat.revolt.internals.extensions.zero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FriendsScreen(useDrawer: Boolean, onDrawerClicked: () -> Unit) {
    var overflowMenuShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.friends),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (useDrawer) {
                        IconButton(onClick = {
                            onDrawerClicked()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(id = R.string.menu)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        overflowMenuShown = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.menu)
                        )
                    }
                    DropdownMenu(
                        expanded = overflowMenuShown,
                        onDismissRequest = {
                            overflowMenuShown = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.friends_deny_all_incoming))
                            },
                            onClick = {
                                scope.launch {
                                    overflowMenuShown = false
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
                },
                windowInsets = WindowInsets.zero
            )
        },
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .fillMaxHeight()
        ) {
            LazyColumn {
                stickyHeader(key = "incoming") {
                    CountableListHeader(
                        text = stringResource(id = R.string.friends_incoming_requests),
                        count = FriendRequests.getIncoming().size
                    )
                }

                items(FriendRequests.getIncoming().size) {
                    val item = FriendRequests.getIncoming()[it]
                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }

                stickyHeader(key = "outgoing") {
                    CountableListHeader(
                        text = stringResource(id = R.string.friends_outgoing_requests),
                        count = FriendRequests.getOutgoing().size
                    )
                }

                items(FriendRequests.getOutgoing().size) {
                    val item = FriendRequests.getOutgoing()[it]
                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }

                stickyHeader(key = "online") {
                    CountableListHeader(
                        text = stringResource(id = R.string.status_online),
                        count = FriendRequests.getOnlineFriends().size
                    )
                }

                items(FriendRequests.getOnlineFriends().size) {
                    val item = FriendRequests.getOnlineFriends()[it]
                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }

                stickyHeader(key = "not_online") {
                    CountableListHeader(
                        text = stringResource(id = R.string.friends_all),
                        count = FriendRequests.getFriends(true).size
                    )
                }

                items(FriendRequests.getFriends(true).size) {
                    val item = FriendRequests.getFriends(true)[it]
                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }

                stickyHeader(key = "blocked") {
                    CountableListHeader(
                        text = stringResource(id = R.string.friends_blocked),
                        count = FriendRequests.getBlocked().size
                    )
                }


                items(FriendRequests.getBlocked().size) {
                    val item = FriendRequests.getBlocked()[it]
                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}