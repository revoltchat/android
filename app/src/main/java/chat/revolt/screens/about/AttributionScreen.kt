package chat.revolt.screens.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.ui.theme.DarkColorScheme
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults

@Composable
fun AttributionScreen(navController: NavController) {
    Column() {
        Text(
            text = stringResource(R.string.oss_attribution),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Left,
                fontSize = 24.sp
            ),
            modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 15.dp)
                .fillMaxWidth(),
        )
        LibrariesContainer(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            colors = LibraryDefaults.libraryColors(
                backgroundColor = DarkColorScheme.background,
                contentColor = DarkColorScheme.onBackground,
                badgeBackgroundColor = DarkColorScheme.primary,
                badgeContentColor = DarkColorScheme.onPrimary
            )
        )
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 30.dp, top = 5.dp, start = 20.dp, end = 20.dp)
        ) {
            Text(text = stringResource(id = R.string.back))
        }
    }

}