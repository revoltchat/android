package chat.revolt.internals

import android.content.Context
import chat.revolt.api.RevoltJson
import chat.revolt.persistence.KVStorage
import kotlinx.serialization.Serializable

@Serializable
data class Changelog(
    val summary: String,
    val version: String,
    val date: String,
)

@Serializable
data class ChangelogIndex(
    val list: Map<String, Changelog>,
    val latest: String
)

class Changelogs(val context: Context, val kvStorage: KVStorage? = null) {
    val index = context.assets.open("changelogs/index.json").use {
        it.reader().readText()
    }.let {
        RevoltJson.decodeFromString(ChangelogIndex.serializer(), it)
    }

    fun getChangelog(version: String): String {
        return context.assets.open("changelogs/${version}.md").use {
            it.reader().readText()
        }
    }

    suspend fun hasSeenLatest(): Boolean {
        if (kvStorage == null) throw IllegalStateException("Not supported for non-KVStorage instances of Changelogs")

        return kvStorage.get("latestChangelogRead") == index.latest
    }

    suspend fun markAsSeen() {
        if (kvStorage == null) throw IllegalStateException("Not supported for non-KVStorage instances of Changelogs")

        kvStorage.set("latestChangelogRead", index.latest)
    }
}