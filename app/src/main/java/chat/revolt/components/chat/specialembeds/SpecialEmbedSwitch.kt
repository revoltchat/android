package chat.revolt.components.chat.specialembeds

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.revolt.api.schemas.Special

@Composable
fun SpecialEmbedSwitch(special: Special, modifier: Modifier = Modifier) {
    when (special.type) {
        "YouTube" -> YoutubeEmbedSwitch(special, modifier)
        "AppleMusic" -> AppleMusicEmbed(special, modifier)
        else -> {}
    }
}