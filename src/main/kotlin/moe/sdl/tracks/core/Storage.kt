package moe.sdl.tracks.core

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.MemoryPolicy
import com.dropbox.android.external.store4.StoreBuilder
import java.io.File
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration
import moe.sdl.tracks.consts.BILI_IMG_CACHE_DIR
import moe.sdl.tracks.ui.common.loadImageBitmap
import moe.sdl.tracks.util.encode.Md5
import moe.sdl.tracks.util.io.ensureCreate
import mu.KotlinLogging
import okio.buffer
import okio.sink
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skiko.toImage

private val logger by lazy { KotlinLogging.logger {} }


@Suppress("NOTHING_TO_INLINE", "FunctionName")
private inline fun BiliImgCacheFile(url: String) = File("$BILI_IMG_CACHE_DIR/${Md5.digestBase64(url)}")

@OptIn(ExperimentalTime::class)
internal val biliImgStore = StoreBuilder
    .from(
        fetcher = Fetcher.of { url: String ->
            logger.debug { "Fetching for $url" }
            return@of runCatching { loadImageBitmap(url) }
        },
//        sourceOfTruth = SourceOfTruth.of(
//            nonFlowReader = ImageDiskCache::read,
//            writer = ImageDiskCache::write,
//            delete = ImageDiskCache::delete,
//            deleteAll = ImageDiskCache::deleteAll,
//        ),
    )
    .cachePolicy(
        MemoryPolicy.builder<String, Result<ImageBitmap>>()
            .setWeigherAndMaxWeight(weigher = { _, data ->
                // Suppose compression ratio 500, aka, 1080P == 4 MiB, weighted 4096±
                data.onSuccess { return@setWeigherAndMaxWeight(it.height * it.width) / 1000 }
                0
            }, 1024 * 20)
            .setExpireAfterWrite(10.toDuration(DurationUnit.MINUTES))
            .build()
    )
    .build()

internal object ImageDiskCache {
    suspend fun read(url: String): ImageBitmap {
        val file = BiliImgCacheFile(url)
        logger.debug { "Loading img cache for $url from ${file.absolutePath}..." }
        if (!file.exists()) {
            logger.debug { "None cache found, getting from remote..." }
            return loadImageBitmap(url).also { write(url, it) }
        }
        return loadImageBitmap(file)
    }

    suspend fun write(url: String, img: ImageBitmap) {
        val bytes =
            img.toAwtImage().toImage().encodeToData(format = EncodedImageFormat.WEBP)?.bytes
        if (bytes == null) {
            logger.warn { "Failed to write img cache for $url, unable to encode img data to bytes." }
            return
        }
        val file = BiliImgCacheFile(url)
        file.ensureCreate()
        file.sink().use { it.buffer().write(bytes) }
    }

    fun delete(url: String) {
        val file = BiliImgCacheFile(url)
        logger.debug { "Deleting img cache for $url at ${file.absolutePath}" }
        if (!file.exists()) return
        file.delete()
    }

    fun deleteAll() {
        val file = File(BILI_IMG_CACHE_DIR)
        logger.debug { "Clearing img cache" }
        if (file.exists() && file.isDirectory) file.deleteRecursively()
    }
}
