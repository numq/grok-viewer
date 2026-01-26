package io.github.numq.grokviewer.save

import io.github.numq.grokviewer.content.Content
import io.github.numq.grokviewer.usecase.UseCase

class SaveContent(private val service: SaveService) : UseCase<SaveContent.Input, Unit> {
    data class Input(val content: Content, val path: String, val name: String)

    override suspend fun execute(input: Input) = with(input) {
        service.saveContent(content = content, path = path, name = name)
    }
}