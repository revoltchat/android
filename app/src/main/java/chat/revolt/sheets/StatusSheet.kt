package chat.revolt.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.user.patchSelf
import chat.revolt.components.generic.SheetButton
import chat.revolt.components.generic.SheetEnd
import chat.revolt.components.generic.asApiName
import chat.revolt.components.generic.presenceFromStatus
import chat.revolt.components.screens.settings.UserOverview
import chat.revolt.components.settings.profile.StatusPicker
import kotlinx.coroutines.launch

@Composable
fun StatusSheet(onBeforeNavigation: () -> Unit, onGoSettings: () -> Unit) {
    val selfUser = RevoltAPI.userCache[RevoltAPI.selfId]!!
    val scope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        UserOverview(selfUser)

        Spacer(modifier = Modifier.height(16.dp))

        StatusPicker(
            currentStatus = presenceFromStatus(selfUser.status?.presence, selfUser.online ?: false),
            onStatusChange = {
                onBeforeNavigation()
                scope.launch {
                    patchSelf(status = selfUser.status?.copy(presence = it.asApiName()))
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }

    SheetButton(
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null
            )
        },
        headlineContent = {
            Text(
                text = stringResource(id = R.string.settings)
            )
        },
        onClick = {
            onBeforeNavigation()
            onGoSettings()
        }
    )

    SheetEnd()
}
