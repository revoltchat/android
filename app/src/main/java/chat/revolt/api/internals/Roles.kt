package chat.revolt.api.internals

import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.Role

object Roles {
    // lowest rank = highest role
    private fun highestRoleWithPredicate(roles: List<Role?>, predicate: (Role) -> Boolean): Role? {
        return roles.filter { role ->
            predicate(role!!)
        }.minByOrNull { role ->
            role?.rank ?: 0.0
        }
    }

    fun resolveHighestRole(
        serverId: String,
        userId: String,
        withColour: Boolean = false,
        hoisted: Boolean = false
    ): Role? {
        val server = RevoltAPI.serverCache[serverId] ?: return null
        val member = RevoltAPI.members.getMember(serverId, userId) ?: return null

        val roles = member.roles?.map { roleId ->
            server.roles?.get(roleId)
        } ?: return null

        return highestRoleWithPredicate(roles) { role ->
            val hoistPredicate = if (hoisted) (role.hoist == true) else true
            val colourPredicate = if (withColour) (role.colour != null) else true

            hoistPredicate && colourPredicate
        }
    }

    fun inOrder(serverId: String, predicate: (Role) -> Boolean): List<Role> {
        val server = RevoltAPI.serverCache[serverId] ?: return emptyList()

        return server.roles?.values?.filter(predicate)?.sortedBy { it.rank } ?: emptyList()
    }
}