package io.github.numq.grokviewer.archive

import io.github.numq.grokviewer.usecase.UseCase

class AddArchiveContentFilter(
    private val repository: ArchiveRepository
) : UseCase<AddArchiveContentFilter.Input, Unit> {
    data class Input(val contentFilter: ArchiveContentFilter)

    override suspend fun execute(input: Input) = repository.addContentFilter(filter = input.contentFilter)
}