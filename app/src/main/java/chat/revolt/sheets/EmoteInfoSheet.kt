package chat.revolt.sheets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.custom.fetchEmoji
import chat.revolt.api.schemas.Emoji
import chat.revolt.api.schemas.Server
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.generic.SheetClickable
import chat.revolt.internals.Platform
import kotlinx.coroutines.launch

@Composable
fun EmoteInfoSheet(id: String, onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var emoteInfo by remember { mutableStateOf<Emoji?>(null) }
    var parentServer by remember { mutableStateOf<Server?>(null) }

    LaunchedEffect(id) {
        emoteInfo = RevoltAPI.emojiCache[id] ?: fetchEmoji(id)
        when (emoteInfo?.parent?.type) {
            "Server" -> parentServer = RevoltAPI.serverCache[emoteInfo?.parent?.id]
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(0.dp))
        ) {
            RemoteImage(
                url = "$REVOLT_FILES/emojis/$id",
                description = emoteInfo?.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(16.dp)
                    .size(32.dp)
            )

            Column(
                modifier = Modifier.padding(
                    top = 16.dp,
                    start = 0.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
            ) {
                Text(
                    text = emoteInfo?.name ?: id,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.15.sp
                )

                Text(
                    text = if (parentServer != null) {
                        stringResource(
                            id = R.string.emote_info_from_server,
                            parentServer?.name ?: ""
                        )
                    } else {
                        stringResource(id = R.string.emote_info_from_server_unknown)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SheetClickable(
            icon = { modifier ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_content_copy_24dp),
                    contentDescription = null,
                    modifier = modifier
                )
            },
            label = { style ->
                Text(
                    text = stringResource(id = R.string.copy),
                    style = style
                )
            },
        ) {
            coroutineScope.launch {
                clipboardManager.setText(AnnotatedString(":$id:"))
                if (Platform.needsShowClipboardNotification()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.copied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            onDismiss()
        }
    }
}