package chat.revolt.activities.media

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltHttp
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.api.settings.GlobalState
import chat.revolt.components.generic.PageHeader
import chat.revolt.provider.getAttachmentContentUri
import chat.revolt.ui.theme.RevoltTheme
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.launch

class VideoViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val autumnResource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("autumnResource", AutumnResource::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("autumnResource")
        }

        if (autumnResource?.id == null) {
            Log.e("VideoViewActivity", "No AutumnResource provided")
            finish()
            return
        }

        setContent {
            VideoViewScreen(resource = autumnResource, onClose = { finish() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoViewScreen(
    resource: AutumnResource,
    onClose: () -> Unit = {}
) {
    val resourceUrl = "$REVOLT_FILES/attachments/${resource.id}/${resource.filename}"

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    val shareSubmenuIsOpen = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun shareUrl() {
        shareSubmenuIsOpen.value = false

        val intent =
            Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(
            Intent.EXTRA_TEXT,
            resourceUrl
        )

        val shareIntent = Intent.createChooser(intent, null)
        activityLauncher.launch(shareIntent)
    }

    fun shareVideo() {
        shareSubmenuIsOpen.value = false

        coroutineScope.launch {
            val contentUri = getAttachmentContentUri(
                context,
                resourceUrl,
                resource.id!!,
                resource.filename ?: "video"
            )

            val intent =
                Intent(Intent.ACTION_SEND)
            intent.type = resource.contentType ?: "video/*"
            intent.putExtra(
                Intent.EXTRA_STREAM,
                contentUri
            )

            val shareIntent = Intent.createChooser(intent, null)
            activityLauncher.launch(shareIntent)
        }
    }

    fun saveToGallery() {
        coroutineScope.launch {
            context.applicationContext.let {
                it.contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, resource.filename)
                        put(MediaStore.Video.Media.MIME_TYPE, resource.contentType)
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Revolt")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                )
            }?.let { uri ->
                context.contentResolver.openOutputStream(uri).use { stream ->
                    val video = RevoltHttp.get(resourceUrl).readBytes()
                    stream?.write(video)

                    context.applicationContext.let {
                        it.contentResolver.update(
                            uri,
                            ContentValues().apply {
                                put(MediaStore.Video.Media.IS_PENDING, 0)
                            },
                            null,
                            null
                        )
                    }

                    val snackbar = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.video_viewer_saved),
                        actionLabel = context.getString(R.string.video_viewer_open),
                        duration = SnackbarDuration.Short
                    )

                    if (snackbar == SnackbarResult.ActionPerformed) {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(uri, resource.contentType)
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        activityLauncher.launch(intent)
                    }
                }
            }
        }
    }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(resourceUrl))
            prepare()
            play()
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    RevoltTheme(requestedTheme = GlobalState.theme) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { pv ->
            Surface(
                modifier = Modifier
                    .padding(pv)
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxSize()
            ) {
                Column {
                    PageHeader(text = stringResource(
                        id = R.string.video_viewer_title, resource.filename ?: resource.id!!
                    ),
                        showBackButton = true,
                        onBackButtonClicked = onClose,
                        maxLines = 1,
                        additionalButtons = {
                            Row {
                                IconButton(onClick = {
                                    shareSubmenuIsOpen.value = true
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_share_24dp),
                                        contentDescription = stringResource(id = R.string.share)
                                    )
                                }

                                DropdownMenu(
                                    expanded = shareSubmenuIsOpen.value,
                                    onDismissRequest = {
                                        shareSubmenuIsOpen.value = false
                                    }) {
                                    DropdownMenuItem(
                                        onClick = {
                                            shareUrl()
                                        },
                                        text = {
                                            Text(stringResource(id = R.string.video_viewer_share_url))
                                        }
                                    )
                                    DropdownMenuItem(
                                        onClick = {
                                            shareVideo()
                                        },
                                        text = {
                                            Text(stringResource(id = R.string.video_viewer_share_video))
                                        }
                                    )
                                }

                                IconButton(onClick = {
                                    saveToGallery()
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_download_24dp),
                                        contentDescription = stringResource(id = R.string.video_viewer_save)
                                    )
                                }
                            }
                        })

                    Box(
                        modifier = Modifier
                            .clip(RectangleShape)
                            .fillMaxSize()
                    ) {
                        AndroidView(
                            factory = { context ->
                                PlayerView(context).apply {
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                                }
                            },
                            update = {
                                it.player = player
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                        )
                    }
                }
            }
        }
    }
}