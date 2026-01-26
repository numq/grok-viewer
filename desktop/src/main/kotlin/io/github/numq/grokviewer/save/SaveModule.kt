package io.github.numq.grokviewer.save

import org.koin.dsl.bind
import org.koin.dsl.module

val saveModule = module {
    single { SaveService.Default() } bind SaveService::class

    single { SaveArchive(service = get()) } bind SaveArchive::class

    single { SaveContent(service = get()) } bind SaveContent::class

    single { SaveContents(service = get()) } bind SaveContents::class
}