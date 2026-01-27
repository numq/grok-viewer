package io.github.numq.grokviewer.overview

import io.github.numq.grokviewer.archive.Archive

sealed interface OverviewArchive {
    val archive: Archive

    data class Expanded(override val archive: Archive) : OverviewArchive

    data class Collapsed(override val archive: Archive) : OverviewArchive
}