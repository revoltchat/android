package chat.revolt.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.SpecialUsers

annotation class FeatureFlag(val name: String)
annotation class Treatment(val description: String)

@FeatureFlag("ClosedBetaAccessControl")
sealed class ClosedBetaAccessControlVariates {
    @Treatment(
        "Restrict access to the app to users that meet certain or all criteria (implementation-specific)"
    )
    data class Restricted(val predicate: () -> Boolean) : ClosedBetaAccessControlVariates()

    @Treatment("Allow access to the app to all users")
    data object Unrestricted : ClosedBetaAccessControlVariates()
}

@FeatureFlag("LabsAccessControl")
sealed class LabsAccessControlVariates {
    @Treatment(
        "Restrict access to Labs to users that meet certain or all criteria (implementation-specific)"
    )
    data class Restricted(val predicate: () -> Boolean) : LabsAccessControlVariates()
}

object FeatureFlags {
    @FeatureFlag("ClosedBetaAccessControl")
    var closedBetaAccessControl by mutableStateOf<ClosedBetaAccessControlVariates>(
        ClosedBetaAccessControlVariates.Restricted {
            RevoltAPI.channelCache.containsKey("01H7X2KRB0CA4QDSMB4N7WGERF")
        }
    )

    @FeatureFlag("LabsAccessControl")
    var labsAccessControl by mutableStateOf<LabsAccessControlVariates>(
        LabsAccessControlVariates.Restricted {
            RevoltAPI.selfId == SpecialUsers.JENNIFER
        }
    )
}
