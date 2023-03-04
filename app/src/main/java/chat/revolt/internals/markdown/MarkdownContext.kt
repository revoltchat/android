package chat.revolt.internals.markdown

import androidx.compose.runtime.snapshots.SnapshotStateMap
import chat.revolt.api.schemas.User

data class MarkdownContext(
    val memberMap: SnapshotStateMap<String, String>,
    val userMap: SnapshotStateMap<String, User>,
    val channelMap: SnapshotStateMap<String, String>,
    val serverId: String,
)