package chat.revolt.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.Members
import chat.revolt.api.internals.WebCompat
import chat.revolt.api.internals.solidColor
import chat.revolt.api.routes.user.fetchUserProfile
import chat.revolt.api.schemas.Profile
import chat.revolt.components.generic.UIMarkdown
import chat.revolt.components.screens.settings.RawUserOverview

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserContextSheet(
    userId: String,
    serverId: String? = null,
    onHideSheet: suspend () -> Unit,
) {
    val user = RevoltAPI.userCache[userId]

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
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
        ) {
            member?.roles?.let {
                Text(
                    text = stringResource(id = R.string.user_context_sheet_category_roles),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    it.forEach { roleId ->
                        val role = server?.roles?.get(roleId)
                        role?.let {
                            Box(
                                modifier = Modifier
                                    .border(
                                        border = BorderStroke(
                                            width = 1.dp,
                                            brush = role.colour?.let { WebCompat.parseColour(it) }
                                                ?: Brush.solidColor(LocalContentColor.current),
                                        ),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = role.name ?: roleId,
                                    style = LocalTextStyle.current.copy(
                                        brush = role.colour?.let { WebCompat.parseColour(it) }
                                            ?: Brush.solidColor(LocalContentColor.current)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.user_context_sheet_category_bio),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 10.dp)
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