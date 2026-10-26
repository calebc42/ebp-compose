// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/** Logical direction used by EBP's swipe_start and swipe_end members. */
enum class RevealSwipeDirection { Start, End }

/**
 * Toolkit-neutral behavior for one admitted swipe side.
 *
 * Rich sides reveal [actionCount] cells on an ordinary swipe. A rich side may
 * opt into a deliberately deeper commit of its first action. The legacy
 * single-action shape preserves its historical release-to-commit behavior.
 */
data class RevealSwipeSide(
    val actionCount: Int,
    val commitFirstOnDeepSwipe: Boolean = false,
    val legacyCommitOnRelease: Boolean = false,
) {
    init {
        require(actionCount in 1..4) { "swipe actionCount must be in 1..4" }
        require(!legacyCommitOnRelease || actionCount == 1) {
            "a legacy commit side has exactly one action"
        }
    }
}

/** Pure release result, shared by the gesture implementation and JVM tests. */
sealed interface RevealSwipeRelease {
    data object Close : RevealSwipeRelease
    data class Reveal(val direction: RevealSwipeDirection) : RevealSwipeRelease
    data class Commit(
        val direction: RevealSwipeDirection,
        val actionIndex: Int = 0,
    ) : RevealSwipeRelease
}

/**
 * Resolve a completed gesture without toolkit state or protocol knowledge.
 * Positive logical travel addresses start; negative logical travel addresses
 * end. Callers with physical pointer deltas must mirror them in RTL.
 */
fun resolveRevealSwipeRelease(
    rawOffsetPx: Float,
    openThresholdPx: Float,
    startDeepThresholdPx: Float,
    endDeepThresholdPx: Float,
    start: RevealSwipeSide?,
    end: RevealSwipeSide?,
): RevealSwipeRelease {
    val direction = when {
        rawOffsetPx >= openThresholdPx && start != null -> RevealSwipeDirection.Start
        rawOffsetPx <= -openThresholdPx && end != null -> RevealSwipeDirection.End
        else -> return RevealSwipeRelease.Close
    }
    val side = if (direction == RevealSwipeDirection.Start) start!! else end!!
    val deepThreshold = if (direction == RevealSwipeDirection.Start) {
        startDeepThresholdPx
    } else {
        endDeepThresholdPx
    }
    return when {
        side.legacyCommitOnRelease -> RevealSwipeRelease.Commit(direction)
        side.commitFirstOnDeepSwipe && abs(rawOffsetPx) >= deepThreshold ->
            RevealSwipeRelease.Commit(direction)
        else -> RevealSwipeRelease.Reveal(direction)
    }
}

/** Convert physical horizontal travel to EBP's logical start/end axis. */
internal fun logicalRevealSwipeOffset(physicalOffsetPx: Float, isRtl: Boolean): Float =
    if (isRtl) -physicalOffsetPx else physicalOffsetPx

@Stable
private class RevealSwipeCoordinator {
    var openKey by mutableStateOf<Any?>(null)
}

private val LocalRevealSwipeCoordinator =
    staticCompositionLocalOf<RevealSwipeCoordinator?> { null }

/** Own one-open-row coordination for a renderer surface or dialog root. */
@Composable
fun ProvideRevealSwipeCoordinator(content: @Composable () -> Unit) {
    val coordinator = remember { RevealSwipeCoordinator() }
    CompositionLocalProvider(LocalRevealSwipeCoordinator provides coordinator) {
        content()
    }
}

/**
 * The width of one revealed action cell.
 *
 * A side's open width is this times its action count, so a renderer drawing
 * the strip must lay its cells out at exactly this width or the drawn strip
 * and the travel disagree. Public for that reason: one definition beats two
 * constants and a test asserting they match.
 */
val RevealSwipeCellWidth = 46.dp
private val SwipeOpenThreshold = 66.dp
private val SwipeLegacyCap = 96.dp
private val SwipeDeepThreshold = 150.dp
private val SwipeDeepHeadroom = 58.dp
private val SwipeDeepCapHeadroom = 20.dp
private const val SwipeRubberBandFactor = 0.18f
private const val SwipeSettleMillis = 340
private val SwipeSettleEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Reusable Foundation-only reveal/commit mechanics.
 *
 * The caller owns all visual chrome and all action semantics. [underlay]
 * receives a close-and-dispatch callback for each cell; [foreground] receives
 * whether the row is open and a close callback so a tap on an open row cannot
 * accidentally invoke the row's ordinary action.
 */
@Composable
fun RevealSwipeRow(
    identity: Any,
    start: RevealSwipeSide?,
    end: RevealSwipeSide?,
    onAction: (
        direction: RevealSwipeDirection,
        actionIndex: Int,
        fromGestureCommit: Boolean,
    ) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    underlay: @Composable BoxScope.(
        direction: RevealSwipeDirection,
        invokeAction: (Int) -> Unit,
    ) -> Unit,
    foreground: @Composable BoxScope.(isOpen: Boolean, close: () -> Unit) -> Unit,
) {
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val cellPx = with(density) { RevealSwipeCellWidth.toPx() }
    val thresholdPx = with(density) { SwipeOpenThreshold.toPx() }
    val legacyCapPx = with(density) { SwipeLegacyCap.toPx() }
    val fixedDeepThresholdPx = with(density) { SwipeDeepThreshold.toPx() }
    val deepHeadroomPx = with(density) { SwipeDeepHeadroom.toPx() }
    val deepCapHeadroomPx = with(density) { SwipeDeepCapHeadroom.toPx() }
    val offset = remember(identity, isRtl) { Animatable(0f) }
    val raw = remember(identity, isRtl) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val coordinator = LocalRevealSwipeCoordinator.current
    val isOpen = offset.value != 0f

    fun openWidth(side: RevealSwipeSide): Float = cellPx * side.actionCount
    fun deepThreshold(side: RevealSwipeSide): Float =
        max(fixedDeepThresholdPx, openWidth(side) + deepHeadroomPx)
    fun dragCap(side: RevealSwipeSide): Float = when {
        side.legacyCommitOnRelease -> legacyCapPx
        side.commitFirstOnDeepSwipe -> deepThreshold(side) + deepCapHeadroomPx
        else -> openWidth(side)
    }
    fun logical(value: Float): Float = logicalRevealSwipeOffset(value, isRtl)
    fun sideFor(value: Float): RevealSwipeSide? = if (logical(value) >= 0f) start else end
    fun rubberBand(value: Float): Float {
        val side = sideFor(value) ?: return 0f
        val cap = dragCap(side)
        val magnitude = abs(value)
        if (magnitude <= cap) return value
        val sign = if (value < 0f) -1f else 1f
        return sign * (cap + (magnitude - cap) * SwipeRubberBandFactor)
    }
    fun animateTo(target: Float) {
        scope.launch {
            if (target != 0f) coordinator?.openKey = identity
            offset.animateTo(
                target,
                tween(SwipeSettleMillis, easing = SwipeSettleEasing),
            )
            raw.floatValue = target
            if (target == 0f && coordinator?.openKey == identity) {
                coordinator.openKey = null
            }
        }
    }
    fun close() = animateTo(0f)
    fun invoke(
        direction: RevealSwipeDirection,
        index: Int,
        fromGestureCommit: Boolean,
    ) {
        close()
        onAction(direction, index, fromGestureCommit)
    }
    fun release() {
        when (val decision = resolveRevealSwipeRelease(
            rawOffsetPx = logical(raw.floatValue),
            openThresholdPx = thresholdPx,
            startDeepThresholdPx = start?.let(::deepThreshold) ?: Float.POSITIVE_INFINITY,
            endDeepThresholdPx = end?.let(::deepThreshold) ?: Float.POSITIVE_INFINITY,
            start = start,
            end = end,
        )) {
            RevealSwipeRelease.Close -> close()
            is RevealSwipeRelease.Reveal -> {
                val side = if (decision.direction == RevealSwipeDirection.Start) start!! else end!!
                val logicalSign =
                    if (decision.direction == RevealSwipeDirection.Start) 1f else -1f
                val physicalSign = if (isRtl) -logicalSign else logicalSign
                animateTo(physicalSign * openWidth(side))
            }
            is RevealSwipeRelease.Commit -> invoke(
                decision.direction,
                decision.actionIndex,
                fromGestureCommit = true,
            )
        }
    }

    LaunchedEffect(coordinator?.openKey, identity) {
        if (coordinator?.openKey != null && coordinator.openKey != identity && offset.value != 0f) {
            offset.animateTo(
                0f,
                tween(SwipeSettleMillis, easing = SwipeSettleEasing),
            )
            raw.floatValue = 0f
        }
    }

    Box(modifier.clipToBounds()) {
        when {
            logical(offset.value) > 0f && start != null -> underlay(RevealSwipeDirection.Start) { index ->
                invoke(RevealSwipeDirection.Start, index, fromGestureCommit = false)
            }
            logical(offset.value) < 0f && end != null -> underlay(RevealSwipeDirection.End) { index ->
                invoke(RevealSwipeDirection.End, index, fromGestureCommit = false)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offset.value }
                .draggable(
                    state = rememberDraggableState { delta ->
                        val next = raw.floatValue + delta
                        raw.floatValue = when {
                            logical(next) > 0f && start == null -> 0f
                            logical(next) < 0f && end == null -> 0f
                            else -> next
                        }
                        scope.launch { offset.snapTo(rubberBand(raw.floatValue)) }
                    },
                    orientation = Orientation.Horizontal,
                    enabled = enabled && (start != null || end != null),
                    onDragStarted = { raw.floatValue = offset.value },
                    onDragStopped = { release() },
                ),
        ) {
            foreground(isOpen, ::close)
        }
    }
}

// ------------------------------------------------------------------------
// Amendment #207: the popup presentation. The item never displaces; a
// horizontal gesture past the ordinary threshold opens the side's actions
// as a floating row beside the gesture. The gesture and the placement are
// shared here; renderers draw the popup and its buttons.

/** Where a popup side opens: the gesture's end along the row, above the touch. */
data class SwipePopupAnchor(val direction: RevealSwipeDirection, val x: Int, val y: Int)

/**
 * Resolve a completed horizontal gesture on a popup row. [dxPx] is the
 * logical travel (start positive, as [resolveRevealSwipeRelease] reads it);
 * a side the row does not author never opens.
 */
fun resolveSwipePopupDirection(
    dxPx: Float,
    openThresholdPx: Float,
    hasStart: Boolean,
    hasEnd: Boolean,
): RevealSwipeDirection? = when {
    dxPx >= openThresholdPx && hasStart -> RevealSwipeDirection.Start
    dxPx <= -openThresholdPx && hasEnd -> RevealSwipeDirection.End
    else -> null
}

/**
 * The popup's offset inside the row: the gesture's end [endXPx] along the
 * row and [fingerPx] above where the touch began, never above the row's
 * top (SPEC 17.3 popup placement, Revived's NotePopup).
 */
fun swipePopupOffset(endXPx: Int, startYPx: Int, fingerPx: Int): Pair<Int, Int> =
    endXPx.coerceAtLeast(0) to (startYPx - fingerPx).coerceAtLeast(0)

/** Espresso's finger: 16mm, the clearance Revived keeps above the touch. */
val SwipePopupFingerClearance = 16f // millimetres

/**
 * The popup gesture over [content]: the row stays put, and a horizontal drag
 * past the ordinary reveal threshold toward an authored side calls [onOpen]
 * with the direction and the popup's offset inside the row.
 */
@Composable
fun SwipePopupRow(
    hasStart: Boolean,
    hasEnd: Boolean,
    onOpen: (SwipePopupAnchor) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val thresholdPx = with(density) { SwipeOpenThreshold.toPx() }
    // 1 in = 25.4 mm = 160 dp, so one millimetre is 160/25.4 dp.
    val fingerPx = with(density) { (SwipePopupFingerClearance * 160f / 25.4f).dp.roundToPx() }
    Box(
        modifier.then(
            if (enabled && (hasStart || hasEnd)) {
                Modifier.pointerInput(hasStart, hasEnd, isRtl) {
                    var startX = 0f
                    var startY = 0f
                    var endX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { startX = it.x; startY = it.y; endX = it.x },
                        onHorizontalDrag = { change, _ -> endX = change.position.x },
                        onDragEnd = {
                            val direction = resolveSwipePopupDirection(
                                logicalRevealSwipeOffset(endX - startX, isRtl),
                                thresholdPx, hasStart, hasEnd,
                            )
                            if (direction != null) {
                                val (x, y) = swipePopupOffset(endX.toInt(), startY.toInt(), fingerPx)
                                onOpen(SwipePopupAnchor(direction, x, y))
                            }
                        },
                    )
                }
            } else {
                Modifier
            },
        ),
    ) {
        content()
    }
}
