package chat.revolt.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.generic.drawableResource
import chat.revolt.screens.chat.views.HomeScreen
import chat.revolt.R
import chat.revolt.screens.chat.views.ChannelScreen
import kotlinx.coroutines.launch

class ChatRouterViewModel : ViewModel() {
    private var _currentServer =
        mutableStateOf(RevoltAPI.serverCache.values.firstOrNull()?.id ?: "home")
    val currentServer: String
        get() = _currentServer.value

    fun setCurrentServer(serverId: String) {
        _currentServer.value = serverId
    }

    fun goToHome() {
        _currentServer.value = "home"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRouterScreen(topNav: NavController, viewModel: ChatRouterViewModel = viewModel()) {
    val channelDrawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    DismissibleNavigationDrawer(drawerState = channelDrawerState, drawerContent = {
        ModalDrawerSheet {
            Column(Modifier.fillMaxWidth()) {
                Row {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        RemoteImage(
                            url = drawableResource(R.drawable.ic_launcher_monochrome),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.goToHome() },
                            description = "Home",
                        )
                        RevoltAPI.serverCache.values.forEach { server ->
                            server.icon?.let { icon ->
                                RemoteImage(
                                    url = "$REVOLT_FILES/icons/${icon.id!!}/server.png?max_side=256",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.setCurrentServer(server.id!!) },
                                    description = "${server.name}"
                                )
                            }
                        }
                    }
                    Column(
                        Modifier
                            .weight(1f)
                    ) {
                        if (viewModel.currentServer != "home") {
                            val server = RevoltAPI.serverCache[viewModel.currentServer]

                            Text(
                                text = server?.name ?: "Unknown Server",
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            )

                            Column(
                                Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                server?.channels?.forEach { channelId ->
                                    RevoltAPI.channelCache[channelId]?.let {
                                        Text(
                                            text = it.name ?: "Unnamed Channel",
                                            modifier = Modifier.clickable {
                                                scope.launch { channelDrawerState.close() }
                                                navController.navigate("channel/${it.id}")
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(text = "Home not implemented!")
                        }
                    }
                }
            }
        }
    }) {
        Column(Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(navController = navController)
                }
                composable("channel/{channelId}") { backStackEntry ->
                    val channelId = backStackEntry.arguments?.getString("channelId")
                    if (channelId != null) {
                        ChannelScreen(navController, channelId = channelId)
                    }
                }
            }
        }
    }
}