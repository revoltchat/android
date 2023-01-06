package chat.revolt.components.generic

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import chat.revolt.BuildConfig
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.memory.MemoryCache
import coil.request.ImageRequest

@Composable
fun RemoteImage(
    url: String,
    description: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    width: Int = 0,
    height: Int = 0,
    crossfade: Boolean = true,
) {
    val context = LocalContext.current

    fun imageRequest() = run {
        val builder = ImageRequest.Builder(context)
            .crossfade(crossfade)
            .data(url)

        if (width != 0 && height != 0) {
            builder.size(width, height)
        }

        builder.build()
    }

    AsyncImage(
        model = imageRequest(),
        imageLoader = ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(.25)
                    .build()
            }
            .build(),
        contentDescription = description,
        contentScale = contentScale,
        modifier = modifier,
    )
}

fun drawableResource(id: Int): String {
    return "android.resource://" + BuildConfig.APPLICATION_ID + "/" + id
}