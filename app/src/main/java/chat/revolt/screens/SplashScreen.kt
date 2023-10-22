package chat.revolt.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.activities.WebChallengeActivity
import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import chat.revolt.api.internals.WebChallenge
import chat.revolt.api.routes.onboard.needsOnboarding
import chat.revolt.api.settings.GlobalState
import chat.revolt.api.settings.SyncedSettings
import chat.revolt.components.screens.splash.DisconnectedScreen
import chat.revolt.persistence.KVStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class SplashScreenViewModel @Inject constructor(
    private val kvStorage: KVStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var _navigateTo by mutableStateOf("")
    val navigateTo: String
        get() = _navigateTo

    fun setNavigateTo(value: String) {
        _navigateTo = value
    }

    private var _isConnected by mutableStateOf(false)
    val isConnected: Boolean
        get() = _isConnected

    fun setIsConnected(value: Boolean) {
        _isConnected = value
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private suspend fun canReachRevolt(): Boolean {
        val res = RevoltHttp.get("/")
        return res.status.value == 200
    }

    fun checkLoggedInState() {
        Log.d("SplashScreenViewModel", "Checking logged in state")
        viewModelScope.launch {
            setIsConnected(hasInternetConnection())

            if (!isConnected) return@launch

            val needsCloudflare = WebChallenge.needsCloudflare()

            if (needsCloudflare) {
                setNavigateTo("webchallenge")
                return@launch
            }

            val token = kvStorage.get("sessionToken") ?: return@launch setNavigateTo("login")
            val id = kvStorage.get("sessionId") ?: ""

            val canReachRevolt = canReachRevolt()
            val valid = RevoltAPI.checkSessionToken(token)

            if (canReachRevolt && !valid) {
                Toast.makeText(
                    context,
                    context.getString(R.string.token_invalid_toast),
                    Toast.LENGTH_SHORT
                ).show()
                kvStorage.remove("sessionToken")
                kvStorage.remove("sessionId")
                setNavigateTo("login")
            } else {
                val onboard = needsOnboarding()
                if (onboard) {
                    setNavigateTo("onboarding")
                    return@launch
                }

                RevoltAPI.loginAs(token)
                RevoltAPI.setSessionId(id)
                loadSettings()
                setNavigateTo("home")
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            SyncedSettings.fetch()
            GlobalState.hydrateWithSettings(SyncedSettings)
        }
    }

    init {
        checkLoggedInState()
    }
}

@Composable
fun SplashScreen(navController: NavController, viewModel: SplashScreenViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val webChallengeActivityResult = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == 0) {
            viewModel.checkLoggedInState()
        }
    }

    if (!viewModel.isConnected) {
        DisconnectedScreen(
            onRetry = {
                viewModel.checkLoggedInState()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.revolt_logo_wide),
            contentDescription = "Revolt Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        )
    }

    LaunchedEffect(viewModel.navigateTo) {
        when (viewModel.navigateTo) {
            "login" -> {
                navController.navigate("login/greeting") {
                    popUpTo("splash") {
                        inclusive = true
                    }
                }
            }

            "onboarding" -> {
                navController.navigate("register/onboarding") {
                    popUpTo("splash") {
                        inclusive = true
                    }
                }
            }

            "webchallenge" -> {
                val intent = Intent(context, WebChallengeActivity::class.java)
                webChallengeActivityResult.launch(intent)
            }

            "home" -> {
                navController.navigate("chat") {
                    popUpTo("splash") {
                        inclusive = true
                    }
                }
            }
        }
    }
}
