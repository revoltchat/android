package chat.revolt.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.settings.GlobalState
import chat.revolt.api.settings.MessageReplyStyle
import chat.revolt.api.settings.SyncedSettings
import chat.revolt.components.generic.ListHeader
import chat.revolt.components.generic.RadioItem
import kotlinx.coroutines.launch

class ChatSettingsScreenViewModel : ViewModel() {
    fun updateMessageReplyStyle(next: MessageReplyStyle) {
        viewModelScope.launch {
            SyncedSettings.updateAndroid(SyncedSettings.android.copy(messageReplyStyle = next.name))
            GlobalState.messageReplyStyle = next
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(
    navController: NavController,
    viewModel: ChatSettingsScreenViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings_chat),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
    ) { pv ->
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .padding(pv)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            ListHeader {
                Text(
                    text = stringResource(R.string.settings_chat_quick_reply)
                )
            }

            Column(Modifier.selectableGroup()) {
                RadioItem(
                    selected = GlobalState.messageReplyStyle == MessageReplyStyle.None,
                    onClick = { viewModel.updateMessageReplyStyle(MessageReplyStyle.None) },
                    label = { Text(text = stringResource(R.string.settings_chat_quick_reply_none)) }
                )
                RadioItem(
                    selected = GlobalState.messageReplyStyle == MessageReplyStyle.SwipeFromEnd,
                    onClick = { viewModel.updateMessageReplyStyle(MessageReplyStyle.SwipeFromEnd) },
                    label = { Text(text = stringResource(R.string.settings_chat_quick_reply_swipe_from_end)) }
                )
                RadioItem(
                    selected = GlobalState.messageReplyStyle == MessageReplyStyle.DoubleTap,
                    onClick = { viewModel.updateMessageReplyStyle(MessageReplyStyle.DoubleTap) },
                    label = { Text(text = stringResource(R.string.settings_chat_quick_reply_double_tap)) }
                )
            }
        }
    }
}