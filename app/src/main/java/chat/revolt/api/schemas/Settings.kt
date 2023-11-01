package chat.revolt.api.schemas

import chat.revolt.ui.theme.OverridableColourScheme
import kotlinx.serialization.Serializable

@Serializable
data class OrderingSettings(
    val servers: List<String> = emptyList()
)

@Serializable
data class AndroidSpecificSettings(
    /**
     * The theme to use for the app.
     * Can be one of `{ None, Revolt, Light, M3Dynamic, Amoled }`
     */
    var theme: String? = null,
    /**
     * Colour overrides.
     * Map of `primary, onPrimary, primaryContainer, onPrimaryContainer, inversePrimary, secondary, onSecondary, secondaryContainer, onSecondaryContainer, tertiary, onTertiary, tertiaryContainer, onTertiaryContainer, background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant, surfaceTint, inverseSurface, inverseOnSurface, error, onError, errorContainer, onErrorContainer, outline, outlineVariant, scrim` to int colours.
     */
    var colourOverrides: OverridableColourScheme? = null,
)
