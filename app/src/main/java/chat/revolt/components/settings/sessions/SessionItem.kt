package chat.revolt.components.settings.sessions

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.internals.ULID
import chat.revolt.api.schemas.Session

@Composable
fun SessionItem(
    session: Session,
    modifier: Modifier = Modifier,
    currentSession: Boolean = false,
    onLogout: (Session) -> Unit
) {
    val context = LocalContext.current
    val decodedUlid by remember(session) { mutableLongStateOf(ULID.asTimestamp(session.id)) }
    val formattedTimestamp = remember(decodedUlid) {
        DateUtils.getRelativeTimeSpanString(
            decodedUlid,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = MaterialTheme.shapes.medium)
            .background(
                color = if (currentSession) {
                    MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                } else {
                    MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = session.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = stringResource(R.string.settings_sessions_first_seen, formattedTimestamp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall
            )
        }

        if (!currentSession) {
            ElevatedButton(onClick = { onLogout(session) }) {
                Text(stringResource(R.string.logout))
            }
        }
    }
}