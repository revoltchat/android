package chat.revolt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import chat.revolt.screens.about.AboutScreen
import chat.revolt.screens.about.AttributionScreen
import chat.revolt.screens.about.PlaceholderScreen
import chat.revolt.screens.chat.HomeScreen
import chat.revolt.screens.login.GreeterScreen
import chat.revolt.screens.login.LoginScreen
import chat.revolt.screens.login.MfaScreen
import chat.revolt.ui.theme.RevoltTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RevoltTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppEntrypoint()
                }
            }
        }
    }
}

@Composable
fun AppEntrypoint() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "setup/greeting"
    ) {
        composable("setup/greeting") { GreeterScreen(navController) }
        composable("setup/login") { LoginScreen(navController) }
        composable("setup/mfa/{mfaTicket}/{allowedAuthTypes}") { backStackEntry ->
            val mfaTicket = backStackEntry.arguments?.getString("mfaTicket") ?: ""
            val allowedAuthTypes =
                backStackEntry.arguments?.getString("allowedAuthTypes") ?: ""

            MfaScreen(navController, allowedAuthTypes, mfaTicket)
        }

        composable("chat/home") { HomeScreen(navController) }

        composable("about") { AboutScreen(navController) }
        composable("about/oss") { AttributionScreen(navController) }
        composable("about/placeholder") { PlaceholderScreen(navController) }
    }
}
