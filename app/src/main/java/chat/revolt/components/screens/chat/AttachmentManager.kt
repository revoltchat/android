package chat.revolt.components.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.routes.microservices.autumn.FileArgs
import java.io.File

@Composable
fun AttachmentManager(
    attachments: List<FileArgs>,
    uploading: Boolean,
    uploadProgress: Float = 0f,
    onRemove: (FileArgs) -> Unit,
    canRemove: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uploadProgress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "Upload progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
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
                    if (canRemove) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.remove_attachment_alt)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        AnimatedVisibility(visible = uploading) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun AttachmentManagerPreview() {
    AttachmentManager(
        attachments = listOf(
            FileArgs(
                filename = "file1.png",
                contentType = "image/png",
                file = File("file1.png")
            ),
            FileArgs(
                filename = "file2.png",
                contentType = "image/png",
                file = File("file2.png")
            ),
            FileArgs(
                filename = "file3.png",
                contentType = "image/png",
                file = File("file3.png")
            )
        ),
        uploading = false,
        onRemove = {}
    )
}
