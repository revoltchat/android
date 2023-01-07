package chat.revolt.components.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.revolt.api.routes.microservices.autumn.FileArgs
import chat.revolt.R

@Composable
fun AttachmentManager(
    attachments: List<FileArgs>,
    uploading: Boolean,
    onRemove: (FileArgs) -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
    ) {
        AnimatedVisibility(uploading) {
            CircularProgressIndicator()
        }
        attachments.forEach { attachment ->
            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        MaterialTheme.shapes.small
                    )
                    .clickable {
                        onRemove(attachment)
                    }
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