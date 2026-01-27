package io.github.numq.grokviewer.image

import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.numq.grokviewer.content.Content
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.zip.ZipFile

object ImageProvider : AutoCloseable {
    private const val MAX_ENTRIES = 128

    private const val ZIP_POOL_CAPACITY = 10

    private val imageCache =
        Collections.synchronizedMap(object : LinkedHashMap<String, CachedImage>(MAX_ENTRIES, .75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedImage>?) = size > MAX_ENTRIES
        })

    private val zipPool =
        Collections.synchronizedMap(object : LinkedHashMap<String, ZipFile>(ZIP_POOL_CAPACITY, .75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ZipFile>?): Boolean {
                if (size > ZIP_POOL_CAPACITY) {
                    eldest?.value?.close()

                    return true
                }

                return false
            }
        })

    private fun getZipFile(path: String) = synchronized(zipPool) {
        zipPool.getOrPut(path) { ZipFile(File(path)) }
    }

    private fun getBytes(content: Content): ByteArray {
        val zip = getZipFile(content.zipFilePath)

        val entry =
            zip.getEntry(content.entryName) ?: throw NoSuchElementException("Entry ${content.entryName} not found")

        return zip.getInputStream(entry).use(InputStream::readAllBytes)
    }

    private fun decodeSampledBitmap(bytes: ByteArray, dstWidth: Float, dstHeight: Float) =
        Image.makeFromEncoded(bytes).use { skiaImage ->
            Surface.makeRasterN32Premul(dstWidth.toInt(), dstHeight.toInt()).use { surface ->
                val canvas = surface.canvas

                val srcWidth = skiaImage.width.toFloat()

                val srcHeight = skiaImage.height.toFloat()

                val scale = maxOf(dstWidth / srcWidth, dstHeight / srcHeight)

                val scaledWidth = srcWidth * scale

                val scaledHeight = srcHeight * scale

                val dx = (dstWidth - scaledWidth) / 2f

                val dy = (dstHeight - scaledHeight) / 2f

                canvas.drawImageRect(
                    skiaImage,
                    Rect.makeXYWH(0f, 0f, srcWidth, srcHeight),
                    Rect.makeXYWH(dx, dy, scaledWidth, scaledHeight),
                    SamplingMode.LINEAR,
                    null,
                    true
                )

                val resultBitmap = surface.makeImageSnapshot().toComposeImageBitmap()

                CachedImage(bitmap = resultBitmap, x = dx.toInt(), y = dy.toInt())
            }
        }

    fun getOrCreate(content: Content, dstWidth: Float, dstHeight: Float) = content.runCatching {
        imageCache[id] ?: synchronized(id.intern()) {
            imageCache[id] ?: decodeSampledBitmap(
                bytes = getBytes(this), dstWidth = dstWidth, dstHeight = dstHeight
            ).also { imageCache[id] = it }
        }
    }.getOrNull()

    override fun close() {
        imageCache.clear()

        synchronized(zipPool) {
            zipPool.values.forEach(ZipFile::close)

            zipPool.clear()
        }
    }
}