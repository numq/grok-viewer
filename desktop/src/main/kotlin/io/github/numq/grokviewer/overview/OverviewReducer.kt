package io.github.numq.grokviewer.overview

import io.github.numq.grokviewer.archive.AddArchives
import io.github.numq.grokviewer.archive.ClearArchives
import io.github.numq.grokviewer.archive.GetArchives
import io.github.numq.grokviewer.archive.RemoveArchive
import io.github.numq.grokviewer.content.Content
import io.github.numq.grokviewer.feature.*
import io.github.numq.grokviewer.save.SaveArchive
import io.github.numq.grokviewer.save.SaveCandidate
import io.github.numq.grokviewer.save.SaveContent
import io.github.numq.grokviewer.save.SaveContents
import kotlinx.coroutines.flow.map

class OverviewReducer(
    private val addArchives: AddArchives,
    private val clearArchives: ClearArchives,
    private val getArchives: GetArchives,
    private val removeArchive: RemoveArchive,
    private val saveArchive: SaveArchive,
    private val saveContent: SaveContent,
    private val saveContents: SaveContents,
) : Reducer<OverviewState, OverviewCommand, OverviewEvent> {
    override fun reduce(state: OverviewState, command: OverviewCommand) = when (command) {
        is OverviewCommand.HandleFailure -> transition(state).event(
            OverviewEvent.Error(message = command.throwable.message ?: "Unknown error")
        )

        is OverviewCommand.Initialize -> transition(state).effect(
            action(key = command.key, block = {
                getArchives.execute(input = Unit).fold(
                    onSuccess = OverviewCommand::InitializeSuccess, onFailure = OverviewCommand::HandleFailure
                )
            })
        )

        is OverviewCommand.InitializeSuccess -> transition(state).effect(
            stream(key = command.key, flow = command.flow.map(OverviewCommand::HandleArchives))
        )

        is OverviewCommand.HandleArchives -> when (state) {
            is OverviewState.Default -> transition(state.copy(archives = command.archives))

            is OverviewState.Selection -> transition(state.copy(archives = command.archives))
        }

        is OverviewCommand.UploadArchives -> transition(state).effect(action(key = command.key, block = {
            addArchives.execute(input = AddArchives.Input(paths = command.paths)).fold(
                onSuccess = { OverviewCommand.UploadArchivesSuccess }, onFailure = OverviewCommand::HandleFailure
            )
        }))

        is OverviewCommand.UploadArchivesSuccess -> transition(state)

        is OverviewCommand.RemoveArchive -> transition(state).effect(action(key = command.key, block = {
            removeArchive.execute(input = RemoveArchive.Input(path = command.archive.path)).fold(
                onSuccess = { OverviewCommand.RemoveArchiveSuccess }, onFailure = OverviewCommand::HandleFailure
            )
        }))

        is OverviewCommand.RemoveArchiveSuccess -> transition(state)

        is OverviewCommand.ClearArchives -> transition(state).effect(action(key = command.key, block = {
            clearArchives.execute(input = Unit).fold(
                onSuccess = { OverviewCommand.ClearArchivesSuccess }, onFailure = OverviewCommand::HandleFailure
            )
        }))

        is OverviewCommand.ClearArchivesSuccess -> transition(state)

        is OverviewCommand.AddToSelection -> when (state) {
            is OverviewState.Default -> with(state) {
                transition(
                    OverviewState.Selection(
                        archives = archives,
                        lastDirectoryPath = lastDirectoryPath,
                        saveCandidate = saveCandidate,
                        isHovered = isHovered,
                        contents = command.contents.toSet(),
                        contentIds = command.contents.map(Content::id),
                    )
                )
            }

            is OverviewState.Selection -> {
                val contents = state.contents + command.contents

                transition(state.copy(contents = contents, contentIds = contents.map(Content::id)))
            }
        }

        is OverviewCommand.RemoveFromSelection -> when (state) {
            is OverviewState.Default -> transition(state)

            is OverviewState.Selection -> with(state) {
                val contents = state.contents - command.contents

                when {
                    contents.isEmpty() -> transition(
                        OverviewState.Default(
                            archives = archives,
                            lastDirectoryPath = lastDirectoryPath,
                            saveCandidate = saveCandidate,
                            isHovered = isHovered,
                        )
                    )

                    else -> transition(state.copy(contents = contents, contentIds = contents.map(Content::id)))
                }
            }
        }

        is OverviewCommand.ClearSelection -> when (state) {
            is OverviewState.Default -> transition(state)

            is OverviewState.Selection -> with(state) {
                transition(
                    OverviewState.Default(
                        archives = archives,
                        lastDirectoryPath = lastDirectoryPath,
                        saveCandidate = saveCandidate,
                        isHovered = isHovered,
                    )
                )
            }
        }

        is OverviewCommand.SaveArchive -> when (state) {
            is OverviewState.Default -> transition(state.copy(saveCandidate = SaveCandidate.Archive(archive = command.archive)))

            is OverviewState.Selection -> transition(state.copy(saveCandidate = SaveCandidate.Archive(archive = command.archive)))
        }

        is OverviewCommand.SaveContent -> when (state) {
            is OverviewState.Default -> transition(state.copy(saveCandidate = SaveCandidate.Content(content = command.content)))

            is OverviewState.Selection -> transition(state.copy(saveCandidate = SaveCandidate.Content(content = command.content)))
        }

        is OverviewCommand.SaveContents -> when (state) {
            is OverviewState.Default -> transition(state.copy(saveCandidate = SaveCandidate.Contents(contents = command.contents)))

            is OverviewState.Selection -> transition(state.copy(saveCandidate = SaveCandidate.Contents(contents = command.contents)))
        }

        is OverviewCommand.SaveConfirmation -> {
            val saveEffect = when (val candidate = state.saveCandidate) {
                is SaveCandidate.Archive -> action(key = command.key, block = {
                    saveArchive.execute(
                        input = SaveArchive.Input(
                            archive = candidate.archive, path = command.path, name = command.name
                        )
                    ).fold(
                        onSuccess = { OverviewCommand.SaveSuccess }, onFailure = OverviewCommand::HandleFailure
                    )
                })

                is SaveCandidate.Content -> action(key = command.key, block = {
                    saveContent.execute(
                        input = SaveContent.Input(
                            content = candidate.content, path = command.path, name = command.name
                        )
                    ).fold(
                        onSuccess = { OverviewCommand.SaveSuccess }, onFailure = OverviewCommand::HandleFailure
                    )
                })

                is SaveCandidate.Contents -> action(key = command.key, block = {
                    saveContents.execute(
                        input = SaveContents.Input(
                            contents = candidate.contents, path = command.path, name = command.name
                        )
                    ).fold(
                        onSuccess = { OverviewCommand.SaveSuccess }, onFailure = OverviewCommand::HandleFailure
                    )
                })

                else -> null
            }

            when (saveEffect) {
                null -> transition(state)

                else -> when (state) {
                    is OverviewState.Default -> transition(state.copy(lastDirectoryPath = command.path)).effect(
                        saveEffect
                    )

                    is OverviewState.Selection -> transition(state.copy(lastDirectoryPath = command.path)).effect(
                        saveEffect
                    )
                }
            }
        }

        is OverviewCommand.SaveCancellation -> when (state) {
            is OverviewState.Default -> transition(state.copy(saveCandidate = null))

            is OverviewState.Selection -> transition(state.copy(saveCandidate = null))
        }

        is OverviewCommand.SaveSuccess -> when (state) {
            is OverviewState.Default -> transition(state.copy(saveCandidate = null))

            is OverviewState.Selection -> transition(state.copy(saveCandidate = null))
        }

        is OverviewCommand.UpdateHovering -> when (state) {
            is OverviewState.Default -> transition(state.copy(isHovered = command.isHovered))

            is OverviewState.Selection -> transition(state.copy(isHovered = command.isHovered))
        }
    }
}