package io.github.numq.grokviewer.content

sealed interface Content {
    val id: String

    val path: String

    val mimeType: String

    val extension: String

    val entryName: String

    val zipFilePath: String

    data class Unknown(
        override val id: String,
        override val path: String,
        override val mimeType: String,
        override val entryName: String,
        override val zipFilePath: String
    ) : Content {
        override val extension = "bin"
    }

    data class Resolved(
        override val id: String,
        override val path: String,
        override val mimeType: String,
        override val extension: String,
        override val entryName: String,
        override val zipFilePath: String
    ) : Content
}