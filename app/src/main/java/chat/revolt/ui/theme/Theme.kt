package chat.revolt.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

const val FOREGROUND = 0xffffffff

val DarkColorScheme = darkColorScheme(
    primary = Color(0xfffe4654),
    onPrimary = Color(FOREGROUND),
    secondary = Color(0xfffd6671),
    onSecondary = Color(FOREGROUND),
    tertiary = Color(0xffff6667),
    onTertiary = Color(FOREGROUND),
    background = Color(0xff101823),
    onBackground = Color(FOREGROUND),
    surfaceVariant = Color(0xff172333),
    onSurfaceVariant = Color(FOREGROUND),
    surface = Color(0xff111a26),
    onSurface = Color(FOREGROUND),
)

@Composable
fun RevoltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && darkTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        dynamicColor && !darkTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RevoltTypography,
        content = content
    )
}