package chat.revolt.screens

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.generic.drawableResource
import chat.revolt.components.screens.splash.DisconnectedScreen
import chat.revolt.persistence.KVStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw =
            connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun checkLoggedInState() {
        viewModelScope.launch {
            setIsConnected(hasInternetConnection())

            if (!isConnected) return@launch

            val token = kvStorage.get("sessionToken") ?: return@launch setNavigateTo("login")

            val valid = RevoltAPI.checkSessionToken(token)

            if (!valid) {
                kvStorage.remove("sessionToken")
                setNavigateTo("login")
            } else {
                RevoltAPI.loginAs(token)
                setNavigateTo("home")
            }
        }
    }

    init {
        checkLoggedInState()
    }
}

@Composable
fun SplashScreen(navController: NavController, viewModel: SplashScreenViewModel = hiltViewModel()) {
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
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RemoteImage(
            url = drawableResource(R.drawable.revolt_logo_wide),
            description = "Revolt Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        )
    }

    if (viewModel.navigateTo.isNotEmpty()) {
        when (viewModel.navigateTo) {
            "login" -> {
                navController.navigate("login/greeting") {
                    popUpTo("splash") {
                        inclusive = true
                    }
                }
            }
            "home" -> {
                navController.navigate("chat") {
                    popUpTo("splash") {
                        inclusive = true
                    }
                }
            }
        }
        viewModel.setNavigateTo("")
    }
}