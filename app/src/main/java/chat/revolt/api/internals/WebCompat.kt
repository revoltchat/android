package chat.revolt.api.internals

import android.util.Log
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// color is spelled american because Color from compose is spelled american
fun Brush.Companion.solidColor(colour: Color) = linearGradient(
    colorStops = arrayOf(
        0f to colour,
        1f to colour
    )
)

object WebCompat {
    @Composable
    fun parseColour(colour: String): Brush {
        if (colour.startsWith("var(")) {
            Log.d(
                "WebCompat",
                "Parsing colour $colour. ${colour.substringAfter("var(").substringBefore(")")}"
            )
            return when (colour.substringAfter("var(").substringBefore(")")) {
                "--accent" -> Brush.solidColor(MaterialTheme.colorScheme.primary)
                "--foreground" -> Brush.solidColor(MaterialTheme.colorScheme.onBackground)
                "--background" -> Brush.solidColor(MaterialTheme.colorScheme.background)
                "--error" -> Brush.solidColor(MaterialTheme.colorScheme.error)
                else -> Brush.solidColor(LocalContentColor.current)
            }
        } else {
            try {
                return Brush.solidColor(Color(android.graphics.Color.parseColor(colour)))
            } catch (e: IllegalArgumentException) {
                Log.d(
                    "WebCompat",
                    "Failed to parse colour $colour, falling back to LocalContentColor.current"
                )
                return Brush.solidColor(LocalContentColor.current)
            }
        }
    }
}