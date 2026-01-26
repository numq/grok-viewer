package io.github.numq.grokviewer.overview

import io.github.numq.grokviewer.archive.Archive
import io.github.numq.grokviewer.content.Content
import kotlinx.coroutines.flow.Flow

sealed interface OverviewCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS, UPLOAD_FILES, REMOVE_ARCHIVE, CLEAR_ARCHIVES, SAVE_CONTENT_CONFIRMATION
    }

    data class HandleFailure(val throwable: Throwable) : OverviewCommand

    data object Initialize : OverviewCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(val flow: Flow<List<Archive>>) : OverviewCommand {
        val key = Key.INITIALIZE_SUCCESS
    }

    data class HandleArchives(val archives: List<Archive>) : OverviewCommand

    data class UploadArchives(val paths: List<String>) : OverviewCommand {
        val key = Key.UPLOAD_FILES
    }

    data object UploadArchivesSuccess : OverviewCommand

    data class RemoveArchive(val archive: Archive) : OverviewCommand {
        val key = Key.REMOVE_ARCHIVE
    }

    data object RemoveArchiveSuccess : OverviewCommand

    data object ClearArchives : OverviewCommand {
        val key = Key.CLEAR_ARCHIVES
    }

    data object ClearArchivesSuccess : OverviewCommand

    data class AddToSelection(val contents: List<Content>) : OverviewCommand

    data class RemoveFromSelection(val contents: List<Content>) : OverviewCommand

    data object ClearSelection : OverviewCommand

    data class SaveArchive(val archive: Archive.Processed) : OverviewCommand

    data class SaveContent(val content: Content) : OverviewCommand

    data class SaveContents(val contents: List<Content>) : OverviewCommand

    data class SaveConfirmation(val path: String, val name: String) : OverviewCommand {
        val key = Key.SAVE_CONTENT_CONFIRMATION
    }

    data object SaveCancellation : OverviewCommand

    data object SaveSuccess : OverviewCommand

    data class UpdateHovering(val isHovered: Boolean) : OverviewCommand
}