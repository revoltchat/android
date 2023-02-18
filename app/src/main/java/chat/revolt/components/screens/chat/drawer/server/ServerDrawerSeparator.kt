package chat.revolt.components.screens.chat.drawer.server

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ServerDrawerSeparator() {
    Box(
        Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(1.dp)
            .width(48.dp)
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.1f
                )
            )
    )
}