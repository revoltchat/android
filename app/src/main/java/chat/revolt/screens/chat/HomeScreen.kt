package chat.revolt.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.api.RevoltAPI
import chat.revolt.persistence.KVStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val kvStorage: KVStorage
) : ViewModel() {
    private var _messageContent by mutableStateOf("")
    val messageContent: String
        get() = _messageContent

    fun setMessageContent(value: String) {
        _messageContent = value
    }

    fun logout() {
        runBlocking {
            kvStorage.remove("sessionToken")
            RevoltAPI.logout()
        }
    }

    fun sendMessage() {
        viewModelScope.launch {
            chat.revolt.api.routes.channel.sendMessage(
                "01F7ZSBSFHCAAJQ92ZGTY67HMN", // revolt lounge #general (temporarily hardcoded) FIXME
                messageContent
            )
        }
        setMessageContent("")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeScreenViewModel = hiltViewModel()) {
    val channelDrawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    DismissibleNavigationDrawer(drawerState = channelDrawerState, drawerContent = {
        ModalDrawerSheet {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Revolt Lounge",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
            )
            Divider()
            Column(Modifier.verticalScroll(rememberScrollState())) {
                RevoltAPI.channelCache.values
                    .filter { channel ->
                        channel.server == "01F7ZSBSFHQ8TA81725KQCSDDP"
                    }
                    .forEach { channel ->
                        NavigationDrawerItem(
                            selected = false,
                            label = { Text(text = "#" + channel.name) },
                            onClick = {
                                scope.launch {
                                    channelDrawerState.close()
                                    navController.navigate("chat/channel/${channel.id}")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
            }
        }
    }) {
        Column() {
            Text(
                text = "Home (placeholder)",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Left,
                    fontSize = 24.sp
                ),
                modifier = Modifier
                    .padding(horizontal = 15.dp, vertical = 15.dp)
                    .fillMaxWidth(),
            )

            Button(
                onClick = {
                    viewModel.logout()
                    navController.navigate("login/greeting") {
                        popUpTo("chat/home") {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp, top = 5.dp, start = 20.dp, end = 20.dp)
            ) {
                Text("Logout")
            }
        }
    }
}