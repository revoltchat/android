package chat.revolt.screens.settings

import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.BuildConfig
import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.components.generic.PageHeader
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UpdateState {
    UpToDate,
    UpdateAvailable,
    NotChecked,
    Checking,
    RequestingUpdateToken,
    ErrorChecking
}

fun viewUrlInBrowser(ctx: android.content.Context, url: String) {
    val customTab = CustomTabsIntent
        .Builder()
        .build()
    customTab.launchUrl(ctx, Uri.parse(url))
}

@Serializable
data class UpdaterBody(
    val author: String,
    @SerialName("current_build")
    val currentBuild: String
)

@Serializable
data class UpdaterResponse(
    val outdated: Boolean,
    @SerialName("newest_build")
    val newestBuild: Int?,
    val token: String?
)

class ClosedBetaUpdaterScreenViewModel : ViewModel() {
    var updateState by mutableStateOf(UpdateState.NotChecked)
    var newestBuild by mutableIntStateOf(0)
    var newestDownloadToken by mutableStateOf("")

    fun checkForUpdates() {
        updateState = UpdateState.Checking

        viewModelScope.launch {
            val outdatedResponse = RevoltHttp.post(
                "${BuildConfig.ANALYSIS_BASEURL}/api/distribution/android"
            ) {
                contentType(ContentType.Application.Json)
                setBody(
                    UpdaterBody(
                        author = RevoltAPI.selfId ?: "SelfID is null",
                        currentBuild = BuildConfig.VERSION_CODE.toString()
                    )
                )
            }.bodyAsText()

            try {
                val outdated = RevoltJson.decodeFromString(
                    UpdaterResponse.serializer(),
                    outdatedResponse
                )

                if (outdated.outdated) {
                    updateState = UpdateState.RequestingUpdateToken

                    delay(1000)

                    updateState = UpdateState.UpdateAvailable
                    newestBuild = outdated.newestBuild ?: -1
                    newestDownloadToken = outdated.token ?: ""
                } else {
                    updateState = UpdateState.UpToDate
                }
            } catch (e: Exception) {
                updateState = UpdateState.ErrorChecking
                Log.e("Updater", "Error checking for updates", e)
                return@launch
            }
        }
    }
}

@Composable
fun ClosedBetaUpdaterScreen(
    navController: NavController,
    viewModel: ClosedBetaUpdaterScreenViewModel = viewModel()
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        PageHeader(
            text = "Closed Beta Updater",
            showBackButton = true,
            onBackButtonClicked = {
                navController.popBackStack()
            }
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Revolt ${BuildConfig.VERSION_NAME}/${BuildConfig.VERSION_CODE}",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                when (viewModel.updateState) {
                    UpdateState.NotChecked -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF585858),
                            modifier = Modifier.size(100.dp)
                        )
                        Text(
                            text = "Not yet checked for updates",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    UpdateState.UpdateAvailable -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(100.dp)
                        )
                        Text(
                            text = AnnotatedString.Builder().apply {
                                append("You are out of date\n\nBuild ")
                                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                append("${viewModel.newestBuild}")
                                pop()
                                append(" is available")
                            }.toAnnotatedString(),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        AnimatedVisibility(
                            visible = viewModel.updateState == UpdateState.UpdateAvailable
                        ) {
                            ElevatedButton(onClick = {
                                viewUrlInBrowser(
                                    ctx = context,
                                    url = "${BuildConfig.ANALYSIS_BASEURL}/api/distribution/android/download?build=${viewModel.newestBuild}&token=${viewModel.newestDownloadToken}"
                                )
                            }) {
                                Text(text = "Download")
                            }
                        }
                    }

                    UpdateState.Checking -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFFEEFF41),
                            modifier = Modifier.size(100.dp)
                        )
                        Text(
                            text = "Checking for updates...",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    UpdateState.UpToDate -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(100.dp)
                        )
                        Text(
                            text = "Up to date",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    UpdateState.ErrorChecking -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(100.dp)
                        )
                        Text(
                            text = "Error checking for updates",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    UpdateState.RequestingUpdateToken -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFFEEFF41),
                            modifier = Modifier.size(100.dp)
                        )
                        Text(
                            text = "Requesting update token...",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    viewModel.checkForUpdates()
                },
                modifier = Modifier
                    .padding(bottom = 10.dp)
            ) {
                Text(text = "Check for updates")
            }
        }
    }
}
