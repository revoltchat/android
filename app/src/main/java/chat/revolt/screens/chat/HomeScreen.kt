package chat.revolt.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.components.generic.CollapsibleCard
import chat.revolt.components.generic.FormTextField
import chat.revolt.components.generic.RemoteImage
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
                "01F7ZSBSFHCAAJQ92ZGTY67HMN",
                messageContent
            )
        }
    }
}

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeScreenViewModel = hiltViewModel()) {
    val user = RevoltAPI.userCache[RevoltAPI.selfId]

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
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            user?.let {
                Row {
                    RemoteImage(
                        url = "${REVOLT_FILES}/avatars/${it.avatar?.id}/user.png",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        description = "Avatar for ${it.username} (placeholder!)"
                    )

                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        it.username?.let { it1 -> Text(text = it1) }
                        it.id?.let { it1 -> Text(text = it1) }
                    }
                }
            }

            CollapsibleCard(title = "Send a message") {
                Column() {
                    FormTextField(
                        value = viewModel.messageContent,
                        label = "Content",
                        onChange = viewModel::setMessageContent
                    )
                    Button(onClick = { viewModel.sendMessage() }) {
                        Text(text = "Send")
                    }
                }
            }
        }
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