package chat.revolt.components.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LinkOnHome(
    heading: @Composable () -> Unit,
    icon: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    description: @Composable () -> Unit = {},
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(20.dp)
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon(Modifier.padding(end = 14.dp))

            Column {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    heading()
                }

                Spacer(modifier = Modifier.height(2.dp))

                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Normal
                    )
                ) {
                    description()
                }
            }
        }
    }
}

@Preview
@Composable
fun LinkOnHomePreview() {
    LinkOnHome(
        heading = { Text("Heading") },
        description = { Text("Description") },
        icon = { mod -> Icon(Icons.Default.AccountBox, null, modifier = mod) },
        onClick = { }
    )
}
