package io.github.numq.grokviewer.archive

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

val archiveModule = module {
    single { ArchiveRepository.InMemoryArchiveRepository(service = get()) } bind ArchiveRepository::class onClose { it?.close() }

    single { AddArchiveContentFilter(get()) }

    single { AddArchives(get()) }

    single { ClearArchives(get()) }

    single { GetArchiveContentFilters(get()) }

    single { GetArchives(get()) }

    single { RemoveArchive(get()) }

    single { RemoveArchiveContentFilter(get()) }
}