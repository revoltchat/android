package chat.revolt.components.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.routes.microservices.autumn.FileArgs

@Composable
fun AttachmentManager(
    attachments: List<FileArgs>,
    uploading: Boolean,
    onRemove: (FileArgs) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        AnimatedVisibility(uploading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(4.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        attachments.forEach { attachment ->
            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable {
                        onRemove(attachment)
                    }
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(8.dp)
            ) {
                Text(attachment.filename, maxLines = 1)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.remove_attachment_alt)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}