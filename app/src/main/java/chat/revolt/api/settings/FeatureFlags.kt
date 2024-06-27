package chat.revolt.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.SpecialUsers

annotation class FeatureFlag(val name: String)
annotation class Treatment(val description: String)

@FeatureFlag("LabsAccessControl")
sealed class LabsAccessControlVariates {
    @Treatment(
        "Restrict access to Labs to users that meet certain or all criteria (implementation-specific)"
    )
    data class Restricted(val predicate: () -> Boolean) : LabsAccessControlVariates()
}

@FeatureFlag("MediaConversations")
sealed class MediaConversationsVariates {
    @Treatment(
        "Enable voice, video and screen conversations for all users"
    )
    object Enabled : MediaConversationsVariates()

    @Treatment(
        "Enable voice, video and screen conversations for users that meet certain or all criteria (implementation-specific)"
    )
    data class Restricted(val predicate: () -> Boolean) : MediaConversationsVariates()
}

@FeatureFlag("VideoViewActivity2")
sealed class ViewViewActivity2Variates {
    @Treatment(
        "Enable the new XML-based video player activity for all users"
    )
    object Enabled : ViewViewActivity2Variates()

    @Treatment(
        "Enable the new XML-based video player activity for users that meet certain or all criteria (implementation-specific)"
    )
    data class Restricted(val predicate: () -> Boolean) : ViewViewActivity2Variates()
}

object FeatureFlags {
    @FeatureFlag("LabsAccessControl")
    var labsAccessControl by mutableStateOf<LabsAccessControlVariates>(
        LabsAccessControlVariates.Restricted {
            RevoltAPI.selfId == SpecialUsers.JENNIFER
        }
    )

    val labsAccessControlGranted: Boolean
        get() = when (labsAccessControl) {
            is LabsAccessControlVariates.Restricted -> (labsAccessControl as LabsAccessControlVariates.Restricted).predicate()
        }

    @FeatureFlag("MediaConversations")
    var mediaConversations by mutableStateOf<MediaConversationsVariates>(
        MediaConversationsVariates.Restricted {
            RevoltAPI.selfId == SpecialUsers.JENNIFER
        }
    )

    val mediaConversationsGranted: Boolean
        get() = when (mediaConversations) {
            is MediaConversationsVariates.Enabled -> true
            is MediaConversationsVariates.Restricted -> (mediaConversations as MediaConversationsVariates.Restricted).predicate()
        }

    @FeatureFlag("VideoViewActivity2")
    var videoViewActivity2 by mutableStateOf<ViewViewActivity2Variates>(
        ViewViewActivity2Variates.Restricted {
            RevoltAPI.selfId == SpecialUsers.JENNIFER
        }
    )

    val videoViewActivity2Granted: Boolean
        get() = when (videoViewActivity2) {
            is ViewViewActivity2Variates.Enabled -> true
            is ViewViewActivity2Variates.Restricted -> (videoViewActivity2 as ViewViewActivity2Variates.Restricted).predicate()
        }
}
