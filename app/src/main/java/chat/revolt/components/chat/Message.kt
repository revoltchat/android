package chat.revolt.components.chat

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.revolt.api.REVOLT_BASE
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.components.generic.RemoteImage
import chat.revolt.api.schemas.Message as MessageSchema

fun viewAttachmentInBrowser(ctx: android.content.Context, attachment: AutumnResource) {
    val customTab = CustomTabsIntent
        .Builder()
        .build()
    customTab.launchUrl(
        ctx,
        Uri.parse("$REVOLT_FILES/attachments/${attachment.id}/${attachment.filename}")
    )
}

@Composable
fun Message(
    message: MessageSchema
) {
    val author = RevoltAPI.userCache[message.author] ?: return CircularProgressIndicator()
    val context = LocalContext.current

    Row(modifier = Modifier.padding(8.dp)) {
        if (author.avatar != null) {
            RemoteImage(
                url = "$REVOLT_FILES/avatars/${author.avatar.id!!}/user.png",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                description = "Avatar for ${author.username}"
            )
        } else {
            RemoteImage(
                url = "$REVOLT_BASE/users/${author.id}/default_avatar",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                description = "Avatar for ${author.username}"
            )
        }

        Column(modifier = Modifier.padding(start = 10.dp)) {
            author.username?.let {
                Text(
                    text = it,
                    fontWeight = FontWeight.Bold
                )
            }
            message.content?.let {
                Text(
                    text = it
                )
            }

            message.attachments?.let {
                if (message.attachments.isNotEmpty()) {
                    message.attachments.forEach { attachment ->
                        if (attachment.metadata?.type == "Image") {
                            RemoteImage(
                                url = "$REVOLT_FILES/attachments/${attachment.id}/image.png",
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .clickable {
                                        viewAttachmentInBrowser(context, attachment)
                                    },
                                width = attachment.metadata.width?.toInt() ?: 0,
                                height = attachment.metadata.height?.toInt() ?: 0,
                                contentScale = ContentScale.Fit,
                                description = "Attached image ${attachment.filename}"
                            )
                        } else {
                            Text(
                                text = attachment.filename ?: "Attachment",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        viewAttachmentInBrowser(context, attachment)
                                    }
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}