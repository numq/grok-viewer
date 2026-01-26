package io.github.numq.grokviewer.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.unit.dp
import io.github.numq.grokviewer.archive.Archive
import io.github.numq.grokviewer.content.ContentCard
import io.github.numq.grokviewer.save.SaveCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI
import java.nio.file.Path

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun OverviewView(feature: OverviewFeature = koinInject()) {
    val scope = rememberCoroutineScope()

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is OverviewEvent.Error -> println(event.message)
            }
        }
    }

    val target = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent) = when (val payload = event.dragData()) {
                is DragData.FilesList -> {
                    val paths = payload.readFiles().map { uri ->
                        Path.of(URI(uri)).toAbsolutePath().toString()
                    }

                    scope.launch {
                        feature.execute(OverviewCommand.UploadArchives(paths = paths))
                    }

                    true
                }

                else -> false
            }

            override fun onStarted(event: DragAndDropEvent) {
                scope.launch {
                    feature.execute(OverviewCommand.UpdateHovering(isHovered = true))
                }
            }

            override fun onEnded(event: DragAndDropEvent) {
                scope.launch {
                    feature.execute(OverviewCommand.UpdateHovering(isHovered = false))
                }
            }

            override fun onExited(event: DragAndDropEvent) {
                scope.launch {
                    feature.execute(OverviewCommand.UpdateHovering(isHovered = false))
                }
            }
        }
    }

    LaunchedEffect(state.saveCandidate) {
        val candidate = state.saveCandidate ?: return@LaunchedEffect

        val file = withContext(Dispatchers.IO) {
            val fileName = when (candidate) {
                is SaveCandidate.Archive -> "${candidate.archive.name}-${System.currentTimeMillis()}.zip"

                is SaveCandidate.Content -> "${candidate.content.id}.${candidate.content.extension}"

                is SaveCandidate.Contents -> "${System.currentTimeMillis()}.zip"
            }

            val dialog = FileDialog(null as Frame?, "Save Content", FileDialog.SAVE).apply {
                file = fileName

                directory = state.lastDirectoryPath

                isVisible = true
            }

            if (dialog.file != null) File(dialog.directory, dialog.file) else null
        }

        when (file) {
            null -> feature.execute(OverviewCommand.SaveCancellation)

            else -> feature.execute(OverviewCommand.SaveConfirmation(path = file.parent, name = file.name))
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize(), floatingActionButton = {
        AnimatedVisibility(
            visible = (state as? OverviewState.Selection)?.contents?.isNotEmpty() == true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            when (val selectionState = state) {
                is OverviewState.Selection -> Card(shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(
                            space = 4.dp, alignment = Alignment.CenterHorizontally
                        ), verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            scope.launch {
                                feature.execute(OverviewCommand.SaveContents(contents = selectionState.contents.toList()))
                            }
                        }, enabled = selectionState.contents.isNotEmpty()) {
                            Icon(imageVector = Icons.Default.FolderZip, contentDescription = null)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                feature.execute(OverviewCommand.ClearSelection)
                            }
                        }, enabled = selectionState.contents.isNotEmpty()) {
                            Icon(Icons.Filled.SelectAll, contentDescription = null)
                        }
                    }
                }

                else -> Unit
            }
        }
    }, floatingActionButtonPosition = FabPosition.Center, content = { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().background(
                if (state.isHovered) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background
            ).dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    state is OverviewState.Default && event.dragData() is DragData.FilesList
                }, target = target
            ).padding(paddingValues), contentAlignment = Alignment.Center
        ) {
            when {
                state.archives.isEmpty() -> Text(
                    text = "Drag and drop files here", color = MaterialTheme.colorScheme.onBackground
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp), modifier = Modifier.fillMaxSize()
                ) {
                    state.archives.forEach { archive ->
                        stickyHeader(key = "header_${archive.path}", contentType = "header") {
                            val hasSelectedContent by remember(state, archive) {
                                derivedStateOf {
                                    state is OverviewState.Selection && archive is Archive.Processed && archive.contents.any { content ->
                                        content.id in (state as OverviewState.Selection).contentIds
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = Path.of(archive.name).fileName.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TooltipArea(tooltip = {
                                            Card {
                                                Text(
                                                    if (state is OverviewState.Selection) "Clear selected files" else "Select files",
                                                    modifier = Modifier.padding(4.dp)
                                                )
                                            }
                                        }, content = {
                                            IconButton(
                                                onClick = {
                                                    when {
                                                        hasSelectedContent -> scope.launch {
                                                            feature.execute(OverviewCommand.ClearSelection)
                                                        }

                                                        archive is Archive.Processed -> scope.launch {
                                                            feature.execute(OverviewCommand.AddToSelection(contents = archive.contents))
                                                        }
                                                    }
                                                }, enabled = archive is Archive.Processed
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SelectAll, contentDescription = null
                                                )
                                            }
                                        })
                                        TooltipArea(tooltip = {
                                            Card {
                                                Text("Save as ZIP", modifier = Modifier.padding(4.dp))
                                            }
                                        }, content = {
                                            IconButton(
                                                onClick = {
                                                    if (archive is Archive.Processed) {
                                                        scope.launch {
                                                            feature.execute(OverviewCommand.SaveArchive(archive = archive))
                                                        }
                                                    }
                                                },
                                                enabled = state is OverviewState.Default && archive is Archive.Processed
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FolderZip, contentDescription = null
                                                )
                                            }
                                        })
                                        TooltipArea(tooltip = {
                                            Card {
                                                Text("Remove from uploaded", modifier = Modifier.padding(4.dp))
                                            }
                                        }, content = {
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        feature.execute(OverviewCommand.RemoveArchive(archive = archive))
                                                    }
                                                },
                                                enabled = state is OverviewState.Default && archive is Archive.Processed
                                            ) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                                            }
                                        })
                                    }
                                }
                            }
                        }

                        when (archive) {
                            is Archive.Processing -> item(
                                key = "processing_${archive.path}", span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }

                            is Archive.Failure -> item(
                                key = "failure_${archive.path}", span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    "Failed to upload: ${archive.throwable.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            is Archive.Processed -> items(
                                items = archive.contents,
                                key = { content -> "${archive.path}_${content.id}" },
                                contentType = { it::class }) { content ->
                                val isContentSelected by remember(state, content.id) {
                                    derivedStateOf {
                                        (state as? OverviewState.Selection)?.contentIds?.contains(content.id) ?: false
                                    }
                                }

                                ContentCard(content = content, isSelected = isContentSelected, click = {
                                    when (state) {
                                        is OverviewState.Default -> scope.launch {
                                            feature.execute(OverviewCommand.SaveContent(content = content))
                                        }

                                        is OverviewState.Selection -> scope.launch {
                                            val command = when {
                                                isContentSelected -> OverviewCommand.RemoveFromSelection(
                                                    contents = listOf(content)
                                                )

                                                else -> OverviewCommand.AddToSelection(contents = listOf(content))
                                            }

                                            feature.execute(command)
                                        }
                                    }
                                }, longClick = {
                                    if (state is OverviewState.Default) {
                                        scope.launch {
                                            feature.execute(
                                                OverviewCommand.AddToSelection(contents = listOf(content))
                                            )
                                        }
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    })
}