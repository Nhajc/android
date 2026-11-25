package com.nhajc.android.ui.admin

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nhajc.android.R
import com.nhajc.android.data.model.AdminSong
import kotlin.uuid.Uuid
import kotlinx.coroutines.delay

internal class SongDragState(private val list: LazyListState) {
    var draggedId by mutableStateOf<Uuid?>(null)
        private set
    private var distance by mutableFloatStateOf(0f)
    private var initialTop = 0
    private var height = 0
    private var lastTarget: String? = null
    var move: (Uuid, Uuid) -> Unit = { _, _ -> }

    fun start(id: Uuid) {
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "song:$id" } ?: return
        draggedId = id
        initialTop = item.offset
        height = item.size
        distance = 0f
        lastTarget = null
    }

    fun drag(delta: Float) {
        distance += delta
        moveIfNeeded()
    }

    fun stop() {
        draggedId = null
        distance = 0f
        lastTarget = null
    }

    fun translation(id: Uuid): Float {
        if (id != draggedId) return 0f
        val top = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "song:$id" }?.offset ?: initialTop
        return initialTop + distance - top
    }

    private fun moveIfNeeded() {
        val id = draggedId ?: return
        val center = initialTop + distance + height / 2f
        val target = list.layoutInfo.visibleItemsInfo.firstOrNull {
            val key = it.key as? String
            key?.startsWith("song:") == true && key != "song:$id" && center >= it.offset &&
                center <= it.offset + it.size
        }?.key as? String ?: return
        if (lastTarget == target) return
        lastTarget = target
        move(id, Uuid.parse(target.removePrefix("song:")))
    }

    suspend fun scrollAtEdge() {
        if (draggedId == null) return
        val info = list.layoutInfo
        val center = initialTop + distance + height / 2f
        val edge = height.coerceAtLeast(80).toFloat()
        val scroll = when {
            center < info.viewportStartOffset + edge -> -18f
            center > info.viewportEndOffset - edge -> 18f
            else -> 0f
        }
        if (scroll != 0f) {
            list.scrollBy(scroll)
            moveIfNeeded()
        }
    }
}

@Composable
internal fun rememberSongDragState(list: LazyListState, onMove: (Uuid, Uuid) -> Unit): SongDragState {
    val state = remember(list) { SongDragState(list) }
    SideEffect { state.move = onMove }
    LaunchedEffect(state.draggedId) {
        if (state.draggedId != null) {
            while (true) {
                state.scrollAtEdge()
                delay(16)
            }
        }
    }
    return state
}

@Composable
internal fun ReorderSongRow(
    song: AdminSong,
    position: Int,
    previous: Uuid?,
    next: Uuid?,
    drag: SongDragState,
    enabled: Boolean,
    onMove: (Uuid, Uuid) -> Unit
) {
    val up = stringResource(R.string.admin_move_song_up)
    val down = stringResource(R.string.admin_move_song_down)
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("release-song:${song.id}")
            .zIndex(if (drag.draggedId == song.id) 1f else 0f)
            .graphicsLayer { translationY = drag.translation(song.id) },
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (drag.draggedId == song.id) 8.dp else 1.dp
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                position.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(song.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Box(
                Modifier.size(48.dp).testTag("drag-song:${song.id}")
                    .semantics {
                        customActions = if (!enabled) {
                            emptyList()
                        } else {
                            listOfNotNull(
                                previous?.let {
                                    CustomAccessibilityAction(up) {
                                        onMove(song.id, it)
                                        true
                                    }
                                },
                                next?.let {
                                    CustomAccessibilityAction(down) {
                                        onMove(song.id, it)
                                        true
                                    }
                                }
                            )
                        }
                    }
                    .pointerInput(song.id, enabled) {
                        if (enabled) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { drag.start(song.id) },
                                onDragEnd = drag::stop,
                                onDragCancel = drag::stop,
                                onDrag = { change, amount ->
                                    change.consume()
                                    drag.drag(amount.y)
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DragHandle,
                    stringResource(R.string.admin_drag_song),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
