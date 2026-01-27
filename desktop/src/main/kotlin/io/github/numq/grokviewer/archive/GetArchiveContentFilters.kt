package io.github.numq.grokviewer.archive

import io.github.numq.grokviewer.usecase.UseCase
import kotlinx.coroutines.flow.StateFlow

class GetArchiveContentFilters(
    private val repository: ArchiveRepository
) : UseCase<Unit, StateFlow<Set<ArchiveContentFilter>>> {
    override suspend fun execute(input: Unit) = runCatching { repository.contentFilters }
}