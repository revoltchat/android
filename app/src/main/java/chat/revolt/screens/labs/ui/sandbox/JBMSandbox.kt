package chat.revolt.screens.labs.ui.sandbox

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import chat.revolt.components.markdown.jbm.JBM
import chat.revolt.components.markdown.jbm.JBMRenderer
import chat.revolt.settings.dsl.SettingsPage

@OptIn(JBM::class)
@Composable
fun JBMSandbox(navController: NavController) {
    var mdSource by remember { mutableStateOf("") }
    var submitMdSource by remember { mutableStateOf<String?>(null) }

    SettingsPage(
        navController = navController,
        title = {
            Text(
                text = "JB Markdown Sandbox",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    ) {
        Subcategory(
            title = { Text("Source", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        ) {
            TextField(
                value = mdSource,
                onValueChange = { mdSource = it },
                label = { Text("Markdown source") },
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(onClick = {
                submitMdSource = mdSource
            }) {
                Text("Submit")
            }
        }
        Subcategory(
            title = { Text("Output", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        ) {
            submitMdSource?.let { JBMRenderer(it, Modifier) }
                ?: Text("Submit some Markdown and see the output.")
        }
    }
}