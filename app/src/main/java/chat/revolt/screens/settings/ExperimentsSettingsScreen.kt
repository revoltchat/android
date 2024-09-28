package chat.revolt.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.BuildConfig
import chat.revolt.RevoltApplication
import chat.revolt.api.settings.Experiments
import chat.revolt.api.settings.GlobalState
import chat.revolt.persistence.KVStorage
import chat.revolt.settings.dsl.SettingsPage
import chat.revolt.settings.dsl.SubcategoryContentInsets
import kotlinx.coroutines.launch

class ExperimentsSettingsScreenViewModel : ViewModel() {
    private val kv = KVStorage(RevoltApplication.instance)

    fun disableExperiments(then: () -> Unit = {}) {
        viewModelScope.launch {
            kv.remove("experimentsEnabled")
            GlobalState.experimentsEnabled = false
            then()
        }
    }

    val useKotlinMdRendererChecked = mutableStateOf(false)

    fun setUseKotlinMdRendererChecked(value: Boolean) {
        viewModelScope.launch {
            kv.set("exp/useKotlinBasedMarkdownRenderer", value)
            Experiments.useKotlinBasedMarkdownRenderer.setEnabled(value)
            useKotlinMdRendererChecked.value = value
        }
    }
}

@Composable
fun ExperimentsSettingsScreen(
    navController: NavController,
    viewModel: ExperimentsSettingsScreenViewModel = viewModel()
) {
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
                    checked = viewModel.useKotlinMdRendererChecked.value,
                    onCheckedChange = viewModel::setUseKotlinMdRendererChecked
                )
            },
            modifier = Modifier.clickable { viewModel.setUseKotlinMdRendererChecked(!viewModel.useKotlinMdRendererChecked.value) }
        )

        Subcategory(
            title = {
                Text("Disable experiments")
            },
            contentInsets = SubcategoryContentInsets
        ) {
            ElevatedButton(
                onClick = {
                    viewModel.disableExperiments {
                        navController.popBackStack()
                    }
                },
                enabled = !BuildConfig.DEBUG,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (BuildConfig.DEBUG) {
                    Text("Experiments are always enabled in debug builds")
                } else {
                    Text("Disable")
                }
            }
        }
    }
}