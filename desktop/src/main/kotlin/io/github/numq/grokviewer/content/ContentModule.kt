package io.github.numq.grokviewer.content

import kotlinx.serialization.json.Json
import org.apache.tika.Tika
import org.koin.dsl.bind
import org.koin.dsl.module

val contentModule = module {
    single { ContentService.BinaryContentService(json = Json, tika = Tika()) } bind ContentService::class
}