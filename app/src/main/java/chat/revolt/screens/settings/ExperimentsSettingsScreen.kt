package chat.revolt.screens.settings

import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import chat.revolt.api.settings.Experiments
import chat.revolt.api.settings.GlobalState
import chat.revolt.persistence.KVStorage
import chat.revolt.settings.dsl.SettingsPage
import chat.revolt.settings.dsl.SubcategoryContentInsets
import kotlinx.coroutines.launch

@Composable
fun ExperimentsSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val kv = remember { KVStorage(context) }
    val scope = rememberCoroutineScope()

    var useKotlinMdRendererChecked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        useKotlinMdRendererChecked = kv.getBoolean("exp/useKotlinBasedMarkdownRenderer") ?: false
    }

    SettingsPage(
        navController,
        title = {
            Text("Experiments", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    ) {
        ListItem(
            headlineContent = {
                Text("New Message Markdown Renderer")
            },
            supportingContent = {
                Text("Use a Kotlin-based Markdown renderer for messages rather than the C++ one. Missing features may be present.")
            },
            trailingContent = {
                Switch(
                    checked = useKotlinMdRendererChecked,
                    onCheckedChange = { isChecked ->
                        scope.launch {
                            kv.set("exp/useKotlinBasedMarkdownRenderer", isChecked)
                            Experiments.useKotlinBasedMarkdownRenderer.setEnabled(isChecked)
                            useKotlinMdRendererChecked = isChecked
                        }
                    }
                )
            }
        )

        Subcategory(
            title = {
                Text("Disable experiments")
            },
            contentInsets = SubcategoryContentInsets
        ) {
            ElevatedButton(
                onClick = {
                    scope.launch {
                        kv.remove("experimentsEnabled")
                        GlobalState.experimentsEnabled = false
                        navController.popBackStack()
                    }
                }
            ) {
                Text("Disable")
            }
        }
    }
}