package chat.revolt.screens.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.components.generic.PageHeader
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults

@Composable
fun AttributionScreen(navController: NavController) {
    Column() {
        PageHeader(
            text = stringResource(R.string.oss_attribution),
            showBackButton = true,
            onBackButtonClicked = { navController.popBackStack() })

        LibrariesContainer(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            colors = LibraryDefaults.libraryColors(
                backgroundColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                badgeBackgroundColor = MaterialTheme.colorScheme.primary,
                badgeContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }

}