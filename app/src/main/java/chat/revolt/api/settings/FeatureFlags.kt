package chat.revolt.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

annotation class FeatureFlag(val name: String)
annotation class Treatment(val description: String)

@FeatureFlag("TiramisuFilePicker")
enum class FilePickerFeatureFlagVariates {
    @Treatment("Use the READ_MEDIA_IMAGES or READ_MEDIA_VIDEO permissions introduced in Android Tiramisu")
    TiramisuMediaPermissions,

    @Treatment("Use the DocumentsUI picker introduced in Android KitKat")
    DocumentsUI
}

object FeatureFlags {
    @FeatureFlag("TiramisuFilePicker")
    var filePickerType by mutableStateOf(FilePickerFeatureFlagVariates.TiramisuMediaPermissions)
}