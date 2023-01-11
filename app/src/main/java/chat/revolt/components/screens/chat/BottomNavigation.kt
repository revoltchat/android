package chat.revolt.components.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.RevoltTweenIntSize
import kotlinx.coroutines.launch

@Composable
fun BottomNavigation(
    navController: NavController,
    show: Boolean,
) {
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = show,
        enter = expandVertically(
            animationSpec = RevoltTweenIntSize
        ),
        exit = shrinkVertically(
            animationSpec = RevoltTweenIntSize
        ),
    ) {
        BottomAppBar {
            IconButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    scope.launch {
                        if (navController.currentDestination?.route != "chat") {
                            navController.navigate("chat")
                        }
                    }
                }) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = stringResource(id = R.string.home),
                )
            }
            IconButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    scope.launch {
                        if (navController.currentDestination?.route != "settings") {
                            navController.navigate("settings")
                        }
                    }
                }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(id = R.string.settings),
                )
            }
        }
    }
}
