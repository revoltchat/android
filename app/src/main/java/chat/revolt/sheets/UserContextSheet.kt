package chat.revolt.sheets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.user.fetchUserProfile
import chat.revolt.api.schemas.Profile
import chat.revolt.components.generic.UIMarkdown
import chat.revolt.components.screens.settings.RawUserOverview

@Composable
fun UserContextSheet(
    userId: String,
    onHideSheet: suspend () -> Unit,
) {
    val user = RevoltAPI.userCache[userId]

    var profile by remember { mutableStateOf<Profile?>(null) }

    LaunchedEffect(user) {
        try {
            user?.id?.let { fetchUserProfile(it) }?.let { profile = it }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (user == null) {
        // TODO fetch user in this scenario
        Text(text = "not in user cache, but for now there's always this message")
        return
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        RawUserOverview(user, profile)
        Text(
            text = stringResource(id = R.string.user_context_sheet_category_bio),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 10.dp, start = 16.dp, top = 20.dp, end = 16.dp)
        )

        if (profile?.content != null) {
            UIMarkdown(
                text = profile!!.content!!,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else if (profile != null) {
            Text(
                text = stringResource(id = R.string.user_context_sheet_bio_empty),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}