package io.github.numq.grokviewer.save

import io.github.numq.grokviewer.archive.Archive
import io.github.numq.grokviewer.content.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

interface SaveService {
    suspend fun saveArchive(archive: Archive.Processed, path: String, name: String): Result<Unit>

    suspend fun saveContent(content: Content, path: String, name: String): Result<Unit>

    suspend fun saveContents(contents: List<Content>, path: String, name: String): Result<Unit>

    class Default : SaveService {
        private fun pack(contents: List<Content>) = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zipOut ->
                contents.forEach { content ->
                    zipOut.putNextEntry(ZipEntry("${content.id}.${content.extension}"))

                    zipOut.write(content.rawBytes)

                    zipOut.closeEntry()
                }
            }

            baos.toByteArray()
        }

        private suspend fun writeToFile(bytes: ByteArray, path: String, name: String) = withContext(Dispatchers.IO) {
            val destination = File(path, name)

            destination.parentFile?.mkdirs()

            destination.writeBytes(bytes)
        }

        override suspend fun saveArchive(archive: Archive.Processed, path: String, name: String) =
            saveContents(archive.contents, path, name)

        override suspend fun saveContent(content: Content, path: String, name: String) = runCatching {
            writeToFile(content.rawBytes, path, name)
        }

        override suspend fun saveContents(contents: List<Content>, path: String, name: String) = runCatching {
            val archiveBytes = pack(contents)

            writeToFile(archiveBytes, path, name)
        }
    }
}