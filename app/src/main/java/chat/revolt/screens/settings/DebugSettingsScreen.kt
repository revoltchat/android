package chat.revolt.screens.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.components.generic.PageHeader
import chat.revolt.persistence.KVStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugSettingsScreenViewModel @Inject constructor(
    private val kvStorage: KVStorage,
) : ViewModel() {
    fun forgetSidebarSparkShown() {
        viewModelScope.launch {
            kvStorage.remove("sidebarSpark")
        }
    }

    fun forgetAllSparks() {
        this.forgetSidebarSparkShown()
    }

    fun forgetLatestChangelog() {
        viewModelScope.launch {
            kvStorage.remove("latestChangelogRead")
        }
    }
}

@Composable
fun DebugSettingsScreen(
    navController: NavController,
    viewModel: DebugSettingsScreenViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        PageHeader(
            text = "Debug",
            showBackButton = true,
            onBackButtonClicked = {
                navController.popBackStack()
            })


        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Sparks",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                TextButton(onClick = { viewModel.forgetSidebarSparkShown() }) {
                    Text("Forget sidebar spark")
                }
                ElevatedButton(onClick = { viewModel.forgetAllSparks() }) {
                    Text("Forget all sparks")
                }
            }

            Text(
                text = "Changelogs",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                ElevatedButton(onClick = { viewModel.forgetLatestChangelog() }) {
                    Text("Mark latest changelog as unread")
                }
            }
        }
    }
}