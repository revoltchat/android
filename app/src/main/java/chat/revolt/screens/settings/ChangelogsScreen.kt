package chat.revolt.screens.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.internals.Changelogs
import chat.revolt.persistence.KVStorage
import chat.revolt.sheets.ChangelogSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class ChangelogsSettingsScreenViewModel @Inject constructor(
    kvStorage: KVStorage,
    @ApplicationContext context: Context
) : ViewModel() {
    private val changelogs = Changelogs(context, kvStorage)
    val index = changelogs.index
    val list = changelogs.getList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogsSettingsScreen(
    navController: NavController,
    viewModel: ChangelogsSettingsScreenViewModel = hiltViewModel()
) {
    var currentChangelog by remember { mutableStateOf(viewModel.index.latest) }
    var sheetOpen by remember { mutableStateOf(false) }

    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                sheetOpen = false
            }
        ) {
            ChangelogSheet(version = currentChangelog)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings_changelogs),
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
            LazyColumn {
                items(
                    viewModel.list.size,
                    key = { viewModel.list.keys.elementAt(it) }
                ) { index ->
                    val version = viewModel.list.keys.elementAt(index)
                    val changelog = viewModel.list[version]!!

                    Column(
                        modifier = Modifier
                            .clickable {
                                currentChangelog = version
                                sheetOpen = true
                            }
                            .fillMaxWidth()
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = changelog.version,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = changelog.summary,
                                )
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
