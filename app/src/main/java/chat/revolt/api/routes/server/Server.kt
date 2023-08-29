package chat.revolt.api.routes.server

import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltError
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.schemas.Member
import chat.revolt.api.schemas.User
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class FetchMembersResponse(
    val members: List<Member>,
    val users: List<User>
)

suspend fun ackServer(serverId: String) {
    RevoltHttp.put("/servers/$serverId/ack") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)
    }
}

suspend fun fetchMembers(
    serverId: String,
    includeOffline: Boolean = false,
    pure: Boolean = false
): FetchMembersResponse {
    val response = RevoltHttp.get("/servers/$serverId/members") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)

        parameter("exclude_offline", !includeOffline)
    }

    val responseContent = response.bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), responseContent)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val membersResponse =
        RevoltJson.decodeFromString(FetchMembersResponse.serializer(), responseContent)

    if (pure) {
        return membersResponse
    }

    membersResponse.members.forEach { member ->
        if (!RevoltAPI.members.hasMember(serverId, member.id!!.user)) {
            RevoltAPI.members.setMember(serverId, member)
        }
    }

    membersResponse.users.forEach { user ->
        user.id?.let { RevoltAPI.userCache.putIfAbsent(it, user) }
    }

    return membersResponse
}

suspend fun fetchMember(serverId: String, userId: String, pure: Boolean = false): Member {
    val response = RevoltHttp.get("/servers/$serverId/members/$userId") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)
    }

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response.bodyAsText())
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val member = RevoltJson.decodeFromString(Member.serializer(), response.bodyAsText())

    if (!pure) {
        if (!RevoltAPI.members.hasMember(serverId, member.id!!.user)) {
            RevoltAPI.members.setMember(serverId, member)
        }
    }

    return member
}

suspend fun leaveOrDeleteServer(serverId: String, leaveSilently: Boolean = false) {
    RevoltHttp.delete("/servers/$serverId") {
        headers.append(RevoltAPI.TOKEN_HEADER_NAME, RevoltAPI.sessionToken)

        parameter("leave_silently", leaveSilently)
    }
}