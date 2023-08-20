package chat.revolt.api.internals

import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.Role

object Roles {
    // lowest rank = highest role
    private fun resolveHighestRole(roles: List<Role?>): Role? {
        return roles.minByOrNull { role ->
            role?.rank ?: 0.0
        }
    }

    private fun highestRoleWithColour(roles: List<Role?>): Role? {
        return roles.filter { role ->
            role?.colour != null
        }.minByOrNull { role ->
            role?.rank ?: 0.0
        }
    }

    fun resolveHighestRole(serverId: String, userId: String, withColour: Boolean = false): Role? {
        val server = RevoltAPI.serverCache[serverId] ?: return null
        val member = RevoltAPI.members.getMember(serverId, userId) ?: return null

        val roles = member.roles?.map { roleId ->
            server.roles?.get(roleId)
        } ?: return null

        return if (withColour) {
            highestRoleWithColour(roles)
        } else {
            resolveHighestRole(roles)
        }
    }
}