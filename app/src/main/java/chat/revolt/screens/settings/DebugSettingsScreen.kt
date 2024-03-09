package chat.revolt.screens.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.persistence.Database
import chat.revolt.persistence.KVStorage
import chat.revolt.persistence.SqlStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugSettingsScreenViewModel @Inject constructor(
    private val kvStorage: KVStorage
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

    val serverQueries = Database(SqlStorage.driver).serverQueries.selectAll().executeAsList()
    val channelQueries = Database(SqlStorage.driver).channelQueries.selectAll().executeAsList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettingsScreen(
    navController: NavController,
    viewModel: DebugSettingsScreenViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = "Debug",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .fillMaxSize()
        ) {
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

                Text(
                    text = "Database",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Text(
                    text = "Servers: ${viewModel.serverQueries.size}",
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(viewModel.serverQueries.size) { index ->
                        Text(
                            text = viewModel.serverQueries[index].toString(),
                            style = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    }
                }
                Text(
                    text = "Channels: ${viewModel.channelQueries.size}",
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(viewModel.channelQueries.size) { index ->
                        Text(
                            text = viewModel.channelQueries[index].toString(),
                            style = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    }
                }
            }
        }
    }
}
