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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlin.time.Duration.Companion.milliseconds

data class Media(
    val uri: Uri,
    val width: Int,
    val height: Int,
    val duration: Long?,
    val aspectRatio: Float = width.toFloat() / height.toFloat()
)

private fun Long.formatAsLengthDuration(): String {
    val asDuration = this.milliseconds

    val components = asDuration.toComponents { days, hours, minutes, seconds, _ ->
        listOfNotNull(
            if (days > 0) "$days:" else null,
            if (hours > 0) "$hours".padStart(2, '0') + ":" else null,
            "$minutes".padStart(2, '0') + ":",
            "$seconds".padStart(2, '0'),
        )
    }

    return components.joinToString(separator = "")
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalGlideComposeApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun InbuiltMediaPicker(
    onOpenDocumentsUi: () -> Unit,
    onOpenCamera: () -> Unit,
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
                MediaStore.Images.ImageColumns.RESOLUTION,
                MediaStore.Images.ImageColumns.MIME_TYPE,
                MediaStore.Video.VideoColumns.DURATION
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

                        val isVideo =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.MIME_TYPE))
                                .startsWith("video")

                        val durationColumn =
                            cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                        val videoDuration = if (isVideo && durationColumn != -1) {
                            cursor.getLong(durationColumn)
                        } else {
                            null
                        }

                        val contentUri =
                            ContentUris.withAppendedId(
                                MediaStore.Files.getContentUri("external"),
                                id
                            )

                        if (resolution == null) continue

                        images.add(
                            Media(
                                uri = contentUri,
                                width = resolution.split("×")[0].toInt(),
                                height = resolution.split("×")[1].toInt(),
                                duration = videoDuration
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                    contentDescription = null, // see label
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(2.dp)
                                )
                            }
                        )
                        AssistChip(
                            onClick = {
                                onOpenCamera()
                            },
                            label = {
                                Text(text = stringResource(id = R.string.file_picker_chip_camera))
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_camera_24dp),
                                    contentDescription = null, // see label
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(2.dp)
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
                                val imageIsSelected by derivedStateOf { images[image].uri.lastPathSegment in pendingMedia }

                                val borderSize by animateDpAsState(
                                    targetValue = if (imageIsSelected) 2.dp else 0.dp,
                                    animationSpec = tween(),
                                    label = "Media picker image border size #$image"
                                )

                                Box(
                                    modifier = Modifier
                                        .border(
                                            width = borderSize,
                                            color = if (borderSize > 0.dp) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = MaterialTheme.shapes.medium
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
                                ) {
                                    GlideImage(
                                        model = images[image].uri.toString(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.medium)
                                            .fillMaxSize(),
                                    )

                                    if (images[image].duration != null) {
                                        Text(
                                            text = "▶ ${images[image].duration!!.formatAsLengthDuration()}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    MaterialTheme.shapes.small
                                                )
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}