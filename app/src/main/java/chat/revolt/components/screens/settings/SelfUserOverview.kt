package chat.revolt.components.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ULID
import chat.revolt.api.routes.user.fetchUserProfile
import chat.revolt.api.schemas.Profile
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.presenceFromStatus

@Composable
fun SelfUserOverview() {
    val selfUser = RevoltAPI.userCache[RevoltAPI.selfId] ?: return
    var profile by remember { mutableStateOf<Profile?>(null) }

    LaunchedEffect(selfUser) {
        profile = fetchUserProfile(selfUser.id ?: ULID.makeSpecial(0))
    }

    Box(
        contentAlignment = Alignment.BottomStart,
    ) {
        profile?.background?.let { background ->
            RemoteImage(
                url = "$REVOLT_FILES/backgrounds/${background.id}",
                description = null,
                modifier = Modifier
                    .height(128.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )

            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .height(128.dp)
                    .fillMaxWidth()
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            UserAvatar(
                username = selfUser.displayName ?: stringResource(id = R.string.unknown),
                userId = selfUser.id ?: ULID.makeSpecial(0),
                avatar = selfUser.avatar,
                size = 48.dp,
                presence = presenceFromStatus(selfUser.status?.presence ?: "offline"),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(text = AnnotatedString.Builder().apply {
                if (selfUser.displayName != null) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(selfUser.displayName)
                    pop()
                    append("\n")
                }
                append("${selfUser.username}")
                pushStyle(SpanStyle(fontWeight = FontWeight.ExtraLight))
                append("#${selfUser.discriminator}")
                pop()
            }.toAnnotatedString())
        }
    }
}