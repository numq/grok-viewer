package io.github.numq.grokviewer.overview

import io.github.numq.grokviewer.archive.Archive
import io.github.numq.grokviewer.content.Content
import io.github.numq.grokviewer.save.SaveCandidate

sealed interface OverviewState {
    val archives: List<Archive>

    val lastDirectoryPath: String?

    val saveCandidate: SaveCandidate?

    val isHovered: Boolean

    data class Default(
        override val archives: List<Archive> = emptyList(),
        override val lastDirectoryPath: String? = null,
        override val saveCandidate: SaveCandidate? = null,
        override val isHovered: Boolean = false,
    ) : OverviewState

    data class Selection(
        override val archives: List<Archive> = emptyList(),
        override val lastDirectoryPath: String? = null,
        override val saveCandidate: SaveCandidate? = null,
        override val isHovered: Boolean = false,
        val contents: Set<Content>,
        val contentIds: List<String>
    ) : OverviewState
}