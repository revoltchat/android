package chat.revolt.components.screens.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ThemeChip(
    modifier: Modifier = Modifier,
    color: Color,
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .then(modifier)
            .then(
                if (selected) {
                    Modifier
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                } else {
                    Modifier
                }
            )
            .padding(4.dp)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(color)
                .height(60.dp)
                .fillMaxWidth(1f)
        )
        Text(
            text = text,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Preview
@Composable
fun SelectedThemeChipPreview() {
    ThemeChip(
        color = Color.Red,
        text = "Red",
        selected = true,
        onClick = {}
    )
}
