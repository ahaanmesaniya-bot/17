package com.r7studio.r7Playapp

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.delay

/** One shared image loader (memory + disk cache) used by EVERY game icon / banner in the app. */
object R7Images {
    @Volatile private var loader: ImageLoader? = null

    fun get(ctx: Context): ImageLoader {
        loader?.let { return it }
        synchronized(this) {
            loader?.let { return it }
            val app = ctx.applicationContext
            val l = ImageLoader.Builder(app)
                .crossfade(200)
                .respectCacheHeaders(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCache { MemoryCache.Builder(app).maxSizePercent(0.20).build() }
                .diskCache {
                    DiskCache.Builder()
                        .directory(app.cacheDir.resolve("r7_images"))
                        .maxSizePercent(0.02)
                        .build()
                }
                .build()
            loader = l
            return l
        }
    }
}

/** Makes an image address safe: trims, https only, spaces encoded. Empty if unusable. */
fun cleanImageUrl(raw: String): String {
    var u = raw.trim()
    if (u.isEmpty()) return ""
    if (u.startsWith("//")) u = "https:$u"
    if (u.startsWith("http://")) u = "https://" + u.removePrefix("http://")
    if (!u.startsWith("https://")) return ""
    return u.replace(" ", "%20")
}

/**
 * The single image composable for the whole app. If the picture is missing or fails, nothing is
 * drawn, so whatever is behind it (the gradient + first letter placeholder) stays visible.
 * A failed load is retried automatically a few times.
 */
@Composable
fun R7Image(url: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val clean = remember(url) { cleanImageUrl(url) }
    if (clean.isEmpty()) return
    val ctx = LocalContext.current
    var attempt by remember(clean) { mutableIntStateOf(0) }
    var failed by remember(clean) { mutableStateOf(false) }

    LaunchedEffect(clean, failed, attempt) {
        if (failed && attempt < 5) {
            delay(2000L * (attempt + 1))
            attempt += 1
            failed = false
        }
    }

    val request = remember(clean, attempt) {
        ImageRequest.Builder(ctx)
            .data(clean)
            .memoryCacheKey(clean)
            .diskCacheKey(clean)
            .build()
    }
    AsyncImage(
        model = request,
        imageLoader = R7Images.get(ctx),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
        onSuccess = { failed = false },
        onError = { failed = true }
    )
}
