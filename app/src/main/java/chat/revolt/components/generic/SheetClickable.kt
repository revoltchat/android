package chat.revolt.components.generic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SheetClickable(
    icon: @Composable (Modifier) -> Unit,
    label: @Composable (TextStyle) -> Unit,
    modifier: Modifier = Modifier,
    dangerous: Boolean = false,
    onClick: () -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides if (dangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
    ) {
        Box(modifier = modifier.padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onClick)
                    .padding(all = 4.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon(Modifier.padding(end = 16.dp))
                label(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = LocalContentColor.current,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsCategoryPreview() {
    SheetClickable(
        icon = { modifier ->
            Icon(
                modifier = modifier,
                imageVector = Icons.Default.Person,
                contentDescription = "Account"
            )
        },
        label = { textStyle ->
            Text(text = "Account", style = textStyle)
        },
        onClick = {}
    )
}
