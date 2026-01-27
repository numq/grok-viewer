package io.github.numq.grokviewer.overview

import org.koin.dsl.module
import org.koin.dsl.onClose

val overviewModule = module {
    single {
        OverviewReducer(
            getArchiveContentFilters = get(),
            addArchiveContentFilter = get(),
            removeArchiveContentFilter = get(),
            addArchives = get(),
            clearArchives = get(),
            getArchives = get(),
            removeArchive = get(),
            saveArchive = get(),
            saveContent = get(),
            saveContents = get()
        )
    }

    single { OverviewFeature(reducer = get()) } onClose { it?.close() }
}