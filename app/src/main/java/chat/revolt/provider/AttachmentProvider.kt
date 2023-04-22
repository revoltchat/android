package chat.revolt.provider

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import chat.revolt.R
import chat.revolt.api.RevoltHttp
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import java.io.File

class AttachmentProvider : FileProvider(R.xml.file_paths) {
}

suspend fun getAttachmentContentUri(
    context: Context,
    resourceUrl: String,
    id: String,
    filename: String
): Uri {
    val attachmentsDir = File(context.cacheDir, "attachments")
    if (!attachmentsDir.exists()) {
        attachmentsDir.mkdir()
    }

    val response = RevoltHttp.get(resourceUrl)
    val file = File(attachmentsDir, "$id-$filename")
    file.writeBytes(response.readBytes())

    return FileProvider.getUriForFile(
        context,
        "chat.revolt.fileprovider",
        file
    )
}