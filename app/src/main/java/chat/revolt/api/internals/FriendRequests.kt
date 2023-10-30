package chat.revolt.api.internals

import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.User

object FriendRequests {
    fun getIncoming(): List<User> {
        return RevoltAPI.userCache.values.filter { user ->
            user.relationship == "Incoming"
        }
    }

    fun getIncomingCount(): Int {
        return getIncoming().size
    }

    fun getOutgoing(): List<User> {
        return RevoltAPI.userCache.values.filter { user ->
            user.relationship == "Outgoing"
        }
    }

    fun getOutgoingCount(): Int {
        return getOutgoing().size
    }

    fun getBlocked(): List<User> {
        return RevoltAPI.userCache.values.filter { user ->
            user.relationship == "Blocked"
        }
    }

    fun getBlockedCount(): Int {
        return getBlocked().size
    }
}