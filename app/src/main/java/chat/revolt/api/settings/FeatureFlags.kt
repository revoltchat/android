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

@FeatureFlag("BuiltInColourPicker")
sealed class BuiltInColourPickerVariates {
    @Treatment(
        "Use the built-in colour picker"
    )
    object Enabled : BuiltInColourPickerVariates()

    @Treatment(
        "Use the built-in colour picker for users that meet certain or all criteria (implementation-specific)"
    )
    data class Restricted(val predicate: () -> Boolean) : BuiltInColourPickerVariates()

    @Treatment(
        "Use the colour picker from the external library"
    )
    object Disabled : BuiltInColourPickerVariates()
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

    @FeatureFlag("BuiltInColourPicker")
    var builtInColourPicker by mutableStateOf<BuiltInColourPickerVariates>(
        BuiltInColourPickerVariates.Restricted {
            RevoltAPI.selfId == SpecialUsers.JENNIFER
        }
    )

    val builtInColourPickerGranted: Boolean
        get() = when (builtInColourPicker) {
            is BuiltInColourPickerVariates.Enabled -> true
            is BuiltInColourPickerVariates.Restricted -> (builtInColourPicker as BuiltInColourPickerVariates.Restricted).predicate()
            is BuiltInColourPickerVariates.Disabled -> false
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
}
