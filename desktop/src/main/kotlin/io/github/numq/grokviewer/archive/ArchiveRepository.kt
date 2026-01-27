package io.github.numq.grokviewer.archive

import io.github.numq.grokviewer.content.ContentService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

interface ArchiveRepository : AutoCloseable {
    val contentFilters: StateFlow<Set<ArchiveContentFilter>>

    val archives: StateFlow<List<Archive>>

    suspend fun addContentFilter(filter: ArchiveContentFilter): Result<Unit>

    suspend fun removeContentFilter(filter: ArchiveContentFilter): Result<Unit>

    suspend fun addArchives(paths: List<String>): Result<Unit>

    suspend fun removeArchive(path: String): Result<Unit>

    suspend fun clearArchives(): Result<Unit>

    class InMemoryArchiveRepository(private val service: ContentService) : ArchiveRepository {
        private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        private val activeJobs = ConcurrentHashMap<String, Job>()

        private val _processingQueue = Channel<Archive.Processing>()

        init {
            scope.launch {
                for (processing in _processingQueue) {
                    _archives.update { archives ->
                        when {
                            archives.none { archive -> archive.path == processing.path } -> archives + Archive.Processing(
                                path = processing.path, name = processing.name
                            )

                            else -> archives
                        }
                    }

                    val job = launch {
                        try {
                            service.parse(path = processing.path).fold(onSuccess = { contents ->
                                _archives.update { current ->
                                    current.filterNot { archive ->
                                        archive is Archive.Processing && archive.path == processing.path
                                    } + Archive.Processed(
                                        path = processing.path, name = processing.name, contents = contents
                                    )
                                }
                            }, onFailure = { throwable ->
                                _archives.update { current ->
                                    current.map { archive ->
                                        when {
                                            archive is Archive.Processing && archive.path == processing.path -> Archive.Failure(
                                                path = archive.path, name = archive.name, throwable = throwable
                                            )

                                            else -> archive
                                        }
                                    }
                                }
                            })
                        } finally {
                            activeJobs.remove(processing.path)
                        }
                    }

                    activeJobs[processing.path] = job
                }
            }
        }

        private val _contentFilters = MutableStateFlow<Set<ArchiveContentFilter>>(emptySet())

        override val contentFilters = _contentFilters.asStateFlow()

        private val _archives = MutableStateFlow<List<Archive>>(emptyList())

        override val archives = combine(_contentFilters, _archives) { filters, archives ->
            when {
                filters.isEmpty() -> archives

                else -> archives.map { archive ->
                    when (archive) {
                        is Archive.Processed -> archive.copy(contents = archive.contents.filter { content ->
                            val mime = content.mimeType

                            filters.any { filter ->
                                when (filter) {
                                    ArchiveContentFilter.IMAGE -> mime.startsWith("image")

                                    ArchiveContentFilter.TEXT -> mime.startsWith("text")
                                }
                            }
                        })

                        else -> archive
                    }
                }
            }
        }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

        override suspend fun addContentFilter(filter: ArchiveContentFilter) = runCatching {
            _contentFilters.update { filters ->
                filters + filter
            }
        }

        override suspend fun removeContentFilter(filter: ArchiveContentFilter) = runCatching {
            _contentFilters.update { filters ->
                filters - filter
            }
        }

        override suspend fun addArchives(paths: List<String>) = runCatching {
            paths.forEach { path ->
                val file = File(path)

                val name = file.name

                if (file.isFile && name.endsWith("zip")) {
                    _processingQueue.send(Archive.Processing(path = path, name = name))
                }
            }
        }

        override suspend fun removeArchive(path: String) = runCatching {
            activeJobs[path]?.cancel()

            activeJobs.remove(path)

            _archives.update { current ->
                current.filterNot { archive -> archive.path == path }
            }
        }

        override suspend fun clearArchives() = runCatching {
            _archives.update { emptyList() }
        }

        override fun close() {
            scope.cancel()

            _processingQueue.close()
        }
    }
}