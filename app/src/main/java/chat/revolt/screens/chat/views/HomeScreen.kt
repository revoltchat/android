package chat.revolt.screens.chat.views

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import chat.revolt.api.RevoltAPI
import chat.revolt.components.screens.home.LinkOnHome
import chat.revolt.persistence.KVStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import chat.revolt.R
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val kvStorage: KVStorage
) : ViewModel() {
    fun logout() {
        runBlocking {
            kvStorage.remove("sessionToken")
            RevoltAPI.logout()
        }
    }
}

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeScreenViewModel = hiltViewModel()) {
    Column() {
        Text(
            text = stringResource(id = R.string.home),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Left,
                fontSize = 24.sp
            ),
            modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 15.dp)
                .fillMaxWidth(),
        )

        LinkOnHome(
            heading = stringResource(id = R.string.logout),
            icon = Icons.Default.Close,
            onClick = {
                viewModel.logout()
                navController.navigate("login/greeting") {
                    popUpTo("chat") {
                        inclusive = true
                    }
                }
            })
    }
}