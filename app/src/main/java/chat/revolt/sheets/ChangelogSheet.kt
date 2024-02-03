package chat.revolt.sheets

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import chat.revolt.R
import chat.revolt.components.generic.SheetHeaderPadding
import chat.revolt.components.generic.WebMarkdown
import chat.revolt.internals.Changelog
import chat.revolt.internals.Changelogs
import chat.revolt.persistence.KVStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class ChangelogSheetViewModel @Inject constructor(
    val kvStorage: KVStorage,
    @ApplicationContext val context: Context
) : ViewModel() {
    private val changelogs = Changelogs(context, kvStorage)
    var changelogContents by mutableStateOf(null as String?)
    var changelog by mutableStateOf(null as Changelog?)

    private fun getContents(version: String): String {
        return changelogs.getChangelog(version)
    }

    private fun getChangelog(version: String): Changelog {
        return changelogs.index.list[version] ?: throw IllegalStateException("Changelog not found")
    }

    fun populate(version: String) {
        changelogContents = getContents(version)
        changelog = getChangelog(version)
    }
}

@Composable
fun ChangelogSheet(
    version: String,
    new: Boolean = false,
    viewModel: ChangelogSheetViewModel = hiltViewModel()
) {
    LaunchedEffect(version) {
        viewModel.populate(version)
    }

    Column {
        SheetHeaderPadding {
            Text(
                text = if (new) {
                    stringResource(R.string.settings_changelogs_new_header)
                } else {
                    stringResource(
                        R.string.settings_changelogs_historical_version_header,
                        viewModel.changelog?.version
                            ?: stringResource(R.string.settings_changelogs_historical_version_header_placeholder)
                    )
                },
                style = MaterialTheme.typography.headlineSmall
            )
        }

        if (viewModel.changelogContents == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        key(viewModel.changelogContents) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                WebMarkdown(
                    text = viewModel.changelogContents ?: "",
                    maskLoading = true,
                    simpleLineBreaks = false
                )
            }
        }
    }
}
