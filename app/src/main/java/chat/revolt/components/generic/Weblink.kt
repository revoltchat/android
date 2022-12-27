package chat.revolt.components.generic

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Weblink(text: String, url: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current

    AnyLink(text = text, action = {
        val customTab = CustomTabsIntent
            .Builder()
            .build()
        customTab.launchUrl(ctx, Uri.parse(url))
    }, modifier = modifier)
}

@Composable
fun AnyLink(text: String, action: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color(0xaaffffff),
        style = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp
        ),
        modifier = modifier
            .padding(horizontal = 2.5.dp, vertical = 3.dp)
            .clickable(onClick = action)
    )
}

@Preview
@Composable
fun WeblinkPreview() {
    Weblink(text = "https://revolt.chat", url = "https://revolt.chat")
}

@Preview
@Composable
fun AnyLinkPreview() {
    AnyLink(text = "Click me!", action = {})
}