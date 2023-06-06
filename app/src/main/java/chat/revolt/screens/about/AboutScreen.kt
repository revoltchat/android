package chat.revolt.screens.about

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.BuildConfig
import chat.revolt.R
import chat.revolt.api.REVOLT_BASE
import chat.revolt.api.RevoltJson
import chat.revolt.api.routes.misc.Root
import chat.revolt.api.routes.misc.getRootRoute
import chat.revolt.components.generic.PageHeader
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.net.URI

class AboutViewModel(
) : ViewModel() {
    private val _root = mutableStateOf<Root?>(null)
    val root: State<Root?>
        get() = _root

    fun getDebugInformation(): Map<String, String> {
        return mapOf(
            "App ID" to BuildConfig.APPLICATION_ID,
            "App Version" to BuildConfig.VERSION_NAME,
            "API Host" to URI(REVOLT_BASE).host,
            "API Version" to (root.value?.revolt ?: "Unknown"),
            "Runtime SDK" to Build.VERSION.SDK_INT.toString(),
            "Model" to "${Build.MANUFACTURER} ${
                Build.DEVICE.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            } (${Build.MODEL})"
        )
    }

    init {
        viewModelScope.launch {
            _root.value = getRootRoute().copy()
        }
    }
}

@Composable
fun VersionItem(
    key: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier) {
        Text(
            text = key,
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = 1.0f
            ),
            style = MaterialTheme.typography.titleMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .padding(horizontal = 2.5.dp, vertical = 2.5.dp)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = 0.9f
            ),
            style = MaterialTheme.typography.titleMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier
                .padding(horizontal = 2.5.dp, vertical = 2.5.dp)
        )
    }
}

@Composable
fun DebugInfo(viewModel: AboutViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        viewModel.getDebugInformation().forEach { (key, value) ->
            VersionItem(key, value)
        }
    }
}

@Composable
fun AboutScreen(
    navController: NavController,
    viewModel: AboutViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager =
        LocalClipboardManager.current

    fun copyDebugInformation() {
        clipboardManager.setText(AnnotatedString(RevoltJson.encodeToString(viewModel.getDebugInformation())))
        Toast.makeText(
            context,
            context.getString(R.string.copied),
            Toast.LENGTH_SHORT
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PageHeader(
            text = stringResource(R.string.about),
            showBackButton = true,
            onBackButtonClicked = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (viewModel.root.value == null) {
                Text(
                    text = stringResource(R.string.loading),
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.5f
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Normal
                    )
                )
            } else {
                DebugInfo(viewModel)
                TextButton(onClick = ::copyDebugInformation) {
                    Text(text = stringResource(id = R.string.copy))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedButton(
                onClick = { navController.navigate("about/oss") },
                modifier = Modifier
                    .testTag("view_oss_attribution")
            ) {
                Text(text = stringResource(id = R.string.oss_attribution))
            }
        }
    }
}
