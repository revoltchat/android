package chat.revolt.screens.services

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.components.generic.PageHeader
import chat.revolt.components.screens.services.DiscoverView

@Composable
fun DiscoverScreen(navController: NavController) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        PageHeader(
            text = stringResource(R.string.discover),
            showBackButton = true,
            onBackButtonClicked = {
                navController.popBackStack()
            }
        )
        DiscoverView()
    }
}