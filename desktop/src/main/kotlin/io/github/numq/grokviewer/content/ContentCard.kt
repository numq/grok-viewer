package io.github.numq.grokviewer.content

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.numq.grokviewer.image.CachedImage
import io.github.numq.grokviewer.image.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ContentCard(
    content: Content,
    size: Size,
    isHoverable: Boolean,
    isSelectionModeActive: Boolean,
    isSelected: Boolean,
    click: () -> Unit,
    longClick: () -> Unit
) {
    val hintContent = remember(isSelectionModeActive, isSelected) {
        @Composable {
            when {
                isSelectionModeActive -> Text(
                    text = if (isSelected) "Click to Deselect" else "Click to Add to selection",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium
                )

                else -> Column(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(space = 4.dp)
                ) {
                    Text(
                        text = "Click to Save", style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "Long click to Select", style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }

    val progress by animateFloatAsState(targetValue = if (isSelected) 1f else 0f, label = "content_progress")

    TooltipArea(tooltip = {
        if (isHoverable) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                content = hintContent
            )
        }
    }, delayMillis = 500, content = {
        Card(
            modifier = Modifier.padding(4.dp).aspectRatio(1f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().combinedClickable(onLongClick = longClick, onClick = click),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = 1f - (progress * .5f)
                }, contentAlignment = Alignment.Center) {
                    val imageBitmap by produceState<CachedImage?>(
                        initialValue = null, key1 = content.id, key2 = size
                    ) {
                        val width = size.width

                        val height = size.height

                        if (value == null && width > 0f && height > 0f) {
                            value = withContext(Dispatchers.IO) {
                                ImageProvider.getOrCreate(content = content, dstWidth = width, dstHeight = height)
                            }
                        }
                    }

                    when (val bitmap = imageBitmap?.bitmap) {
                        null -> with(content) {
                            Text(
                                text = extension.takeIf(String::isNotBlank) ?: mimeType.takeIf(String::isNotBlank)
                                ?: "?",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        else -> Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.Low
                        )
                    }
                }

                if (progress > 0f) {
                    Card(modifier = Modifier.graphicsLayer {
                        alpha = progress
                    }, shape = CircleShape) {
                        Icon(imageVector = Icons.Outlined.Done, contentDescription = null)
                    }
                }
            }
        }
    })
}