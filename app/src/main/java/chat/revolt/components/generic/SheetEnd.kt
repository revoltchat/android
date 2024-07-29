package chat.revolt.components.generic

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SheetEnd(modifier: Modifier = Modifier) {
    Spacer(modifier.windowInsetsPadding(WindowInsets.navigationBars))
}