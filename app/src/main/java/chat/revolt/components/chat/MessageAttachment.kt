package chat.revolt.components.chat

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.components.generic.RemoteImage

@Composable
fun FileAttachment(attachment: AutumnResource) {
    val context = LocalContext.current

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_file_24dp),
                contentDescription = null,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = attachment.filename ?: "File",
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = Formatter.formatShortFileSize(context, attachment.size ?: 0),
                    maxLines = 1,
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
fun ImageAttachment(attachment: AutumnResource) {
    val url = "$REVOLT_FILES/attachments/${attachment.id}/${attachment.filename}"

    RemoteImage(
        url = url,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(attachment.metadata!!.width!!.toFloat() / attachment.metadata.height!!.toFloat()),
        description = attachment.filename ?: "Image",
    )
}

@Composable
fun VideoAttachment(attachment: AutumnResource) {
    // FIXME Use ExoPlayer to play videos.
    FileAttachment(attachment)
}

@Composable
fun AudioAttachment(attachment: AutumnResource) {
    // FIXME Use ExoPlayer to play audio.
    FileAttachment(attachment)
}

@Composable
fun TextAttachment(attachment: AutumnResource) {
    // FIXME Write bespoke viewer for text attachments.
    FileAttachment(attachment)
}

@Composable
fun MessageAttachment(attachment: AutumnResource, onAttachmentClick: (AutumnResource) -> Unit) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onAttachmentClick(attachment) }
    ) {
        if (attachment.metadata?.type == null) {
            FileAttachment(attachment)
            return
        }

        when (attachment.metadata.type) {
            "Image" -> ImageAttachment(attachment)
            "Video" -> VideoAttachment(attachment)
            "Audio" -> AudioAttachment(attachment)
            "Text" -> TextAttachment(attachment)
            else -> FileAttachment(attachment)
        }
    }
}