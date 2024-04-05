package chat.revolt.components.screens.voice

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun VoiceChannelOverlay(channelId: String, onCollapse: () -> Unit) {
    val context = LocalContext.current
    val windowManager =
        remember { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    val metrics = DisplayMetrics().apply {
        @Suppress("DEPRECATION") // We *need* the real metrics here, not the window metrics
        windowManager.defaultDisplay.getRealMetrics(this)
    }
    val (width, height) = with(LocalDensity.current) {
        Pair(metrics.widthPixels.toDp(), metrics.heightPixels.toDp())
    }

    Dialog(
        onDismissRequest = {
            onCollapse()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true,
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    ) {
        Column(
            Modifier
                .requiredSize(width, height)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            Text(text = "Voice channel overlay for $channelId", textAlign = TextAlign.Center)
            Button(onClick = {
                onCollapse()
            }) {
                Text("Close")
            }
        }
    }
}