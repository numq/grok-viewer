package io.github.numq.grokviewer.save

import io.github.numq.grokviewer.archive.Archive
import io.github.numq.grokviewer.content.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

interface SaveService {
    suspend fun saveArchive(archive: Archive.Processed, path: String, name: String): Result<Unit>

    suspend fun saveContent(content: Content, path: String, name: String): Result<Unit>

    suspend fun saveContents(contents: List<Content>, path: String, name: String): Result<Unit>

    class Default : SaveService {
        private fun streamEntriesToZip(
            contents: List<Content>, destination: File
        ) = ZipOutputStream(FileOutputStream(destination).buffered()).use { out ->
            contents.filterIsInstance<Content.Resolved>().groupBy(Content.Resolved::zipFilePath)
                .forEach { (zipPath, items) ->
                    ZipFile(File(zipPath)).use { zipFile ->
                        items.forEach { content ->
                            val entry = zipFile.getEntry(content.entryName) ?: return@forEach

                            out.putNextEntry(ZipEntry("${content.id}.${content.extension}"))

                            zipFile.getInputStream(entry).use { inputStream ->
                                inputStream.copyTo(out)
                            }

                            out.closeEntry()
                        }
                    }
                }
        }

        override suspend fun saveArchive(archive: Archive.Processed, path: String, name: String) =
            saveContents(archive.contents, path, name)

        override suspend fun saveContent(content: Content, path: String, name: String) = runCatching {
            val resolved = content as? Content.Resolved ?: throw IllegalArgumentException("Unsupported content type")

            withContext(Dispatchers.IO) {
                val destination = File(path, name)

                destination.parentFile?.mkdirs()

                ZipFile(File(resolved.zipFilePath)).use { zipFile ->
                    val entry = zipFile.getEntry(resolved.entryName)

                    zipFile.getInputStream(entry).use { input ->
                        destination.outputStream().use(input::copyTo)
                    }

                    Unit
                }
            }
        }

        override suspend fun saveContents(contents: List<Content>, path: String, name: String) = runCatching {
            val destination = File(path, name)

            destination.parentFile?.mkdirs()

            withContext(Dispatchers.IO) {
                streamEntriesToZip(contents, destination)
            }
        }
    }
}