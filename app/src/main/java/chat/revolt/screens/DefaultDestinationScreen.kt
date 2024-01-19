package chat.revolt.screens

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import chat.revolt.RevoltApplication
import chat.revolt.api.internals.getComponentActivity
import chat.revolt.components.screens.splash.DisconnectedScreen
import com.google.android.material.color.DynamicColors

@Composable
fun DefaultDestinationScreen(
    navController: NavController,
    nextDestination: String? = null,
    isConnected: Boolean = false,
    onRetryConnection: () -> Unit = {}
) {
    val context = LocalContext.current

    if (!isConnected) {
        DisconnectedScreen(
            onRetry = {
                onRetryConnection()
            }
        )
        return
    }

    LaunchedEffect(nextDestination) {
        nextDestination?.let {
            // Fix for SDK >=31, where core-splashscreen accidentally removes dynamic colours
            // See the other one in MainActivity.kt
            DynamicColors.applyToActivityIfAvailable(context.getComponentActivity() as Activity)
            DynamicColors.applyToActivitiesIfAvailable(RevoltApplication.instance)

            navController.popBackStack(navController.graph.startDestinationRoute!!, true)
            navController.navigate(it)
        }
    }

    Box(Modifier.fillMaxSize())
}