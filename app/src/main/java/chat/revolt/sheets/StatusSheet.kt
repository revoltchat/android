package chat.revolt.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ULID
import chat.revolt.components.generic.SheetClickable
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.presenceFromStatus

@Composable
fun StatusSheet(
    onBeforeNavigation: () -> Unit,
    onGoSettings: () -> Unit
) {
    val selfUser = RevoltAPI.userCache[RevoltAPI.selfId]!!

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                username = selfUser.displayName ?: stringResource(id = R.string.unknown),
                userId = selfUser.id ?: ULID.makeSpecial(0),
                avatar = selfUser.avatar,
                size = 48.dp,
                presence = presenceFromStatus(selfUser.status?.presence),
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
            onBeforeNavigation()
            onGoSettings()
        }
    }
}