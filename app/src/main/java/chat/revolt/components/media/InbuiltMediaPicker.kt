package chat.revolt.components.media

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.revolt.R
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

data class Media(
    val uri: Uri,
    val width: Int,
    val height: Int,
    val aspectRatio: Float = width.toFloat() / height.toFloat()
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalGlideComposeApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun InbuiltMediaPicker(
    onOpenDocumentsUi: () -> Unit,
    onClose: () -> Unit,
    onMediaSelected: (Media) -> Unit,
    pendingMedia: List<String>,
    disabled: Boolean = false,
) {
    val mediaPermissionState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    )

    val context = LocalContext.current

    val images = remember { mutableStateListOf<Media>() }

    BackHandler {
        onClose()
    }

    LaunchedEffect(mediaPermissionState.allPermissionsGranted) {
        if (mediaPermissionState.allPermissionsGranted) {
            val projection = arrayOf(
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.RESOLUTION
            )

            val selection: String? = null
            val selectionArgs: Array<String>? = null
            val sortOrder = MediaStore.Images.ImageColumns.DATE_ADDED + " DESC"

            val queryUri = MediaStore.Files.getContentUri("external")

            val cursor: Cursor? = context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    try {
                        val id =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID))
                        val resolution =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.RESOLUTION))
                        val contentUri =
                            ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )

                        if (resolution == null) continue

                        images.add(
                            Media(
                                uri = contentUri,
                                width = resolution.split("×")[0].toInt(),
                                height = resolution.split("×")[1].toInt()
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                cursor.close()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Crossfade(
            targetState = mediaPermissionState.allPermissionsGranted,
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            ),
            label = "Media picker permission dialog"
        ) { state ->
            if (!state) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ux_file_request),
                        modifier = Modifier
                            .width(128.dp)
                            .height(128.dp),
                        contentDescription = null // decorative
                    )
                    Text(
                        text = stringResource(id = R.string.file_picker_permission_request_header),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(id = R.string.file_picker_permission_request_body),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LocalContentColor.current.copy(
                                alpha = 0.5f
                            )
                        ),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = {
                        mediaPermissionState.launchMultiplePermissionRequest()
                    }) {
                        Text(text = stringResource(id = R.string.file_picker_permission_request_cta))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        AssistChip(
                            onClick = {
                                onOpenDocumentsUi()
                            },
                            label = {
                                Text(text = stringResource(id = R.string.file_picker_chip_documents))
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_paperclip_24dp),
                                    contentDescription = null // see label
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(100.dp),
                        verticalItemSpacing = 8.dp,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        content = {
                            items(images.size) { image ->
                                GlideImage(
                                    model = images[image].uri.toString(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.medium)
                                        .then(
                                            if (images[image].uri.lastPathSegment in pendingMedia) {
                                                Modifier.border(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = MaterialTheme.shapes.medium
                                                )
                                            } else Modifier
                                        )
                                        .then(
                                            if (disabled) {
                                                Modifier.alpha(0.5f)
                                            } else {
                                                Modifier.clickable {
                                                    onMediaSelected(images[image])
                                                }
                                            }
                                        )
                                        .width(100.dp)
                                        .aspectRatio(images[image].aspectRatio),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}