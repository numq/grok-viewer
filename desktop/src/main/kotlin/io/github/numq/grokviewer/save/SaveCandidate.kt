package io.github.numq.grokviewer.save

sealed interface SaveCandidate {
    data class Archive(val archive: io.github.numq.grokviewer.archive.Archive.Processed) : SaveCandidate

    data class Content(val content: io.github.numq.grokviewer.content.Content) : SaveCandidate

    data class Contents(val contents: List<io.github.numq.grokviewer.content.Content>) : SaveCandidate
}