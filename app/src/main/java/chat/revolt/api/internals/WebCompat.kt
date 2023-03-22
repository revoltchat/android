package chat.revolt.api.internals

import android.util.Log
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object WebCompat {
    @Composable
    fun parseColour(colour: String): Color {
        if (colour.startsWith("var(")) {
            Log.d(
                "WebCompat",
                "Parsing colour $colour. ${colour.substringAfter("var(").substringBefore(")")}"
            )
            return when (colour.substringAfter("var(").substringBefore(")")) {
                "--accent" -> MaterialTheme.colorScheme.primary
                "--foreground" -> MaterialTheme.colorScheme.onBackground
                "--background" -> MaterialTheme.colorScheme.background
                "--error" -> MaterialTheme.colorScheme.error
                else -> LocalContentColor.current
            }
        } else {
            try {
                return Color(android.graphics.Color.parseColor(colour))
            } catch (e: IllegalArgumentException) {
                Log.d(
                    "WebCompat",
                    "Failed to parse colour $colour, falling back to LocalContentColor.current"
                )
                return LocalContentColor.current
            }
        }
    }
}