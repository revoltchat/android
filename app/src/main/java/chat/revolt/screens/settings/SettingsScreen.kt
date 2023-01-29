package chat.revolt.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.components.generic.PageHeader
import chat.revolt.components.screens.settings.SettingsCategory

@Composable
fun SettingsScreen(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PageHeader(
            text = stringResource(id = R.string.settings),
            showBackButton = true,
            onBackButtonClicked = {
                navController.popBackStack()
            })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            SettingsCategory(
                icon = { modifier ->
                    Icon(
                        painter = painterResource(id = R.drawable.ic_palette_24dp),
                        contentDescription =
                        stringResource(id = R.string.settings_appearance),
                        modifier = modifier
                    )
                },
                label = { textStyle ->
                    Text(
                        text = stringResource(id = R.string.settings_appearance),
                        style = textStyle
                    )
                })
            {
                navController.navigate("settings/appearance")
            }

            SettingsCategory(
                icon = { modifier ->
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(id = R.string.about),
                        modifier = modifier
                    )
                },
                label = { textStyle ->
                    Text(text = stringResource(id = R.string.about), style = textStyle)
                })
            {
                navController.navigate("about")
            }
        }
    }
}