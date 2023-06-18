package chat.revolt.sheets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import chat.revolt.api.internals.Members
import chat.revolt.api.internals.RvxDummyMemberAPI
import chat.revolt.api.routes.user.fetchUserProfile
import chat.revolt.api.schemas.Profile
import chat.revolt.components.generic.UIMarkdown
import chat.revolt.components.screens.settings.RawUserOverview

@Composable
fun UserContextSheet(
    userId: String,
    serverId: String? = null,
    onHideSheet: suspend () -> Unit,
) {
    val user = RevoltAPI.userCache[userId]

    @OptIn(RvxDummyMemberAPI::class)
    val member = serverId?.let { Members.getMember(it, userId) }

    val server = RevoltAPI.serverCache[serverId]

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
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        RawUserOverview(user, profile)

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "sheet for ${server?.name ?: "serverless (omg jamstack reference??)"}",
            )

            Text(
                text = stringResource(id = R.string.user_context_sheet_category_bio),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 10.dp, top = 20.dp)
            )

            if (profile?.content != null) {
                UIMarkdown(
                    text = profile!!.content!!,
                )
            } else if (profile != null) {
                Text(
                    text = stringResource(id = R.string.user_context_sheet_bio_empty),
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}