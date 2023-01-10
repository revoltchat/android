package chat.revolt.screens.about

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
import chat.revolt.api.routes.misc.Root
import chat.revolt.api.routes.misc.getRootRoute
import kotlinx.coroutines.launch
import java.net.URI

class AboutViewModel(
) : ViewModel() {
    private val _root = mutableStateOf<Root?>(null)
    val root: State<Root?>
        get() = _root

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
            color = Color(0xccffffff),
            style = MaterialTheme.typography.titleMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .padding(horizontal = 2.5.dp, vertical = 2.5.dp)
        )
        Text(
            text = value,
            color = Color(0xccffffff),
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
fun ComponentVersions(
    apiVersion: String
) {
    // App Info
    VersionItem(key = BuildConfig.APPLICATION_ID, value = BuildConfig.VERSION_NAME)

    // API Info
    VersionItem(key = URI(REVOLT_BASE).host, value = apiVersion)

    // Device Info
    VersionItem(key = "Runtime SDK", value = Build.VERSION.SDK_INT.toString())
    VersionItem(
        key = "Model",
        value = "${Build.MANUFACTURER} ${
            Build.DEVICE.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }"
    )
}

@Composable
fun AboutScreen(
    navController: NavController,
    viewModel: AboutViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                    color = Color(0xaaffffff),
                    style = MaterialTheme.typography.titleMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Normal
                    )
                )
            } else {
                Text(
                    text = stringResource(R.string.about),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                )

                ComponentVersions(apiVersion = viewModel.root.value!!.revolt)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 30.dp)
        ) {
            ElevatedButton(
                onClick = { navController.navigate("about/oss") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.oss_attribution))
            }
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.back))
            }
        }
    }
}
