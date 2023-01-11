package chat.revolt.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.components.generic.PageHeader

@Composable
fun SettingsScreen(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PageHeader(stringResource(id = R.string.settings))
        ElevatedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                navController.popBackStack()
            }) {
            Text(text = stringResource(id = R.string.back))
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                navController.navigate("about")
            }) {
            Text(text = stringResource(id = R.string.about))
        }
    }
}