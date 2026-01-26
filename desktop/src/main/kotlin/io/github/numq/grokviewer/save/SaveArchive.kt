package io.github.numq.grokviewer.save

import io.github.numq.grokviewer.archive.Archive
import io.github.numq.grokviewer.usecase.UseCase

class SaveArchive(private val service: SaveService) : UseCase<SaveArchive.Input, Unit> {
    data class Input(val archive: Archive.Processed, val path: String, val name: String)

    override suspend fun execute(input: Input) = with(input) {
        service.saveArchive(archive = archive, path = path, name = name)
    }
}