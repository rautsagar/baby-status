package com.sagar.babystatus.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.core.net.toUri
import com.sagar.babystatus.core.common.BabyStatusDispatchers
import com.sagar.babystatus.core.common.Dispatcher
import com.sagar.babystatus.core.common.IdGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

private const val PHOTO_DIR = "photos"
private const val MAX_EDGE_PX = 1024
private const val JPEG_QUALITY = 85

internal class DefaultPhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(BabyStatusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val idGenerator: IdGenerator,
) : PhotoStore {

    override suspend fun importPhoto(uri: String): String = withContext(ioDispatcher) {
        val source = ImageDecoder.createSource(context.contentResolver, uri.toUri())
        // ImageDecoder applies EXIF rotation, so the saved JPEG is upright.
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longEdge = max(info.size.width, info.size.height)
            if (longEdge > MAX_EDGE_PX) {
                val scale = MAX_EDGE_PX.toFloat() / longEdge
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt(),
                    (info.size.height * scale).roundToInt(),
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val relativePath = "$PHOTO_DIR/${idGenerator.newId()}.jpg"
        val file = File(context.filesDir, relativePath).apply { parentFile?.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        relativePath
    }

    override suspend fun delete(path: String) {
        withContext(ioDispatcher) {
            // Only ever delete inside our own photo folder.
            if (path.startsWith("$PHOTO_DIR/") && ".." !in path) File(context.filesDir, path).delete()
        }
    }
}
