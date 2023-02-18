package chat.revolt.screens.chat.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.components.generic.SheetClickable

@Composable
fun StatusSheet(
    navController: NavController,
    topNav: NavController,
) {
    if (RevoltAPI.selfId == null || RevoltAPI.userCache[RevoltAPI.selfId] == null) {
        navController.popBackStack()
        return
    }

    val selfUser = RevoltAPI.userCache[RevoltAPI.selfId]!!

    Surface {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = "Logged in as @${selfUser.username} (${selfUser.id})")

            Spacer(modifier = Modifier.height(8.dp))

            SheetClickable(
                icon = { modifier ->
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = modifier
                    )
                },
                label = { style ->
                    Text(
                        text = stringResource(id = R.string.settings),
                        style = style
                    )
                }
            ) {
                topNav.navigate("settings")
            }
        }
    }
}