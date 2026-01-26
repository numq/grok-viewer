package io.github.numq.grokviewer.save

import io.github.numq.grokviewer.content.Content
import io.github.numq.grokviewer.usecase.UseCase

class SaveContents(private val service: SaveService) : UseCase<SaveContents.Input, Unit> {
    data class Input(val contents: List<Content>, val path: String, val name: String)

    override suspend fun execute(input: Input) = with(input) {
        service.saveContents(contents = contents, path = path, name = name)
    }
}