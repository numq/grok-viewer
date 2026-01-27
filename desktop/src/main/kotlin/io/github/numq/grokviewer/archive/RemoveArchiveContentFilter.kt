package io.github.numq.grokviewer.archive

import io.github.numq.grokviewer.usecase.UseCase

class RemoveArchiveContentFilter(
    private val repository: ArchiveRepository
) : UseCase<RemoveArchiveContentFilter.Input, Unit> {
    data class Input(val contentFilter: ArchiveContentFilter)

    override suspend fun execute(input: Input) = repository.removeContentFilter(filter = input.contentFilter)
}