package com.fittrack.app.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/**
 * Minimal long-press drag-and-drop reorder support for a LazyColumn whose
 * items map 1:1 to list indices. Attach [dragContainer] to the LazyColumn and
 * wrap each item's modifier with [DragDropState.itemModifier].
 */
class DragDropState(
    private val listState: LazyListState,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set
    var draggingItemOffset by mutableFloatStateOf(0f)
        private set

    private var draggingItemInitialOffset = 0

    private val currentDraggingItem: LazyListItemInfo?
        get() = draggingItemIndex?.let { index ->
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        }

    fun onDragStart(offset: Offset) {
        listState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.let { item ->
                draggingItemIndex = item.index
                draggingItemInitialOffset = item.offset
            }
    }

    fun onDrag(dragAmount: Offset) {
        draggingItemOffset += dragAmount.y
        val dragging = currentDraggingItem ?: return
        val startOffset = draggingItemInitialOffset + draggingItemOffset
        val endOffset = startOffset + dragging.size
        val middle = (startOffset + endOffset) / 2f

        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.index != dragging.index &&
                middle.toInt() in item.offset..(item.offset + item.size)
        } ?: return

        val from = dragging.index
        val to = target.index
        onMove(from, to)
        // Keep the visual offset continuous across the index swap.
        draggingItemInitialOffset += (target.offset - dragging.offset)
        draggingItemOffset -= (target.offset - dragging.offset)
        draggingItemIndex = to
    }

    fun onDragEnd() {
        draggingItemIndex = null
        draggingItemOffset = 0f
        draggingItemInitialOffset = 0
    }

    /** Modifier for each list item; lifts and translates the dragged row. */
    fun itemModifier(scope: LazyItemScope, index: Int): Modifier =
        if (index == draggingItemIndex) {
            Modifier
                .zIndex(1f)
                .graphicsLayer { translationY = draggingItemOffset }
        } else {
            with(scope) { Modifier.animateItem() }
        }
}

@Composable
fun rememberDragDropState(
    listState: LazyListState,
    onMove: (Int, Int) -> Unit,
): DragDropState = remember(listState) { DragDropState(listState, onMove) }

/** Attach to the LazyColumn to receive long-press drag gestures. */
fun Modifier.dragContainer(state: DragDropState): Modifier =
    pointerInput(state) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset -> state.onDragStart(offset) },
            onDrag = { change, dragAmount ->
                change.consume()
                state.onDrag(dragAmount)
            },
            onDragEnd = { state.onDragEnd() },
            onDragCancel = { state.onDragEnd() },
        )
    }
