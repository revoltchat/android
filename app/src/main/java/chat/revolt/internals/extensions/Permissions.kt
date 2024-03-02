package chat.revolt.internals.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.Roles

@Composable
fun rememberChannelPermissions(channelId: String): MutableLongState {
    val permissions = remember { mutableLongStateOf(0L) }

    LaunchedEffect(channelId) {
        if (RevoltAPI.selfId == null) return@LaunchedEffect
        if (RevoltAPI.userCache[RevoltAPI.selfId] == null) return@LaunchedEffect
        if (RevoltAPI.channelCache[channelId] == null) return@LaunchedEffect

        val channel = RevoltAPI.channelCache[channelId]
        val selfUser = RevoltAPI.userCache[RevoltAPI.selfId]
        val member = channel?.let {
            it.server?.let { server ->
                RevoltAPI.selfId?.let { selfId ->
                    RevoltAPI.members.getMember(server, selfId)
                }
            }
        }
        channel?.let { permissions.longValue = Roles.permissionFor(it, selfUser, member) }
    }

    return permissions
}