// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class RevealSwipeTest {
    private val rich = RevealSwipeSide(actionCount = 2)
    private val committing = rich.copy(commitFirstOnDeepSwipe = true)
    private val legacy = RevealSwipeSide(
        actionCount = 1,
        legacyCommitOnRelease = true,
    )

    @Test
    fun ordinaryRichSwipeRevealsWithoutDispatching() {
        assertEquals(
            RevealSwipeRelease.Reveal(RevealSwipeDirection.End),
            release(offset = -80f, end = rich),
        )
    }

    @Test
    fun legacyShapeRetainsReleaseToCommit() {
        assertEquals(
            RevealSwipeRelease.Commit(RevealSwipeDirection.Start),
            release(offset = 66f, start = legacy),
        )
    }

    @Test
    fun richCommitRequiresDeliberateDeeperTravel() {
        assertEquals(
            RevealSwipeRelease.Reveal(RevealSwipeDirection.Start),
            release(offset = 149f, start = committing),
        )
        assertEquals(
            RevealSwipeRelease.Commit(RevealSwipeDirection.Start),
            release(offset = 150f, start = committing),
        )
    }

    @Test
    fun unavailableDirectionAlwaysCloses() {
        assertEquals(
            RevealSwipeRelease.Close,
            release(offset = -200f, start = rich),
        )
    }

    @Test
    fun popupOpensPastTheThresholdTowardAnAuthoredSide() {
        assertEquals(
            RevealSwipeDirection.Start,
            resolveSwipePopupDirection(70f, 66f, hasStart = true, hasEnd = true),
        )
        assertEquals(
            RevealSwipeDirection.End,
            resolveSwipePopupDirection(-70f, 66f, hasStart = true, hasEnd = true),
        )
        assertEquals(null, resolveSwipePopupDirection(-70f, 66f, hasStart = true, hasEnd = false))
        assertEquals(null, resolveSwipePopupDirection(40f, 66f, hasStart = true, hasEnd = true))
    }

    @Test
    fun popupSitsAtTheGestureEndAboveTheTouchAndNeverAboveTheRow() {
        assertEquals(300 to 20, swipePopupOffset(endXPx = 300, startYPx = 120, fingerPx = 100))
        assertEquals(300 to 0, swipePopupOffset(endXPx = 300, startYPx = 40, fingerPx = 100))
        assertEquals(0 to 0, swipePopupOffset(endXPx = -5, startYPx = 40, fingerPx = 100))
    }

    @Test
    fun physicalTravelMirrorsOntoLogicalStartInRtl() {
        assertEquals(80f, logicalRevealSwipeOffset(80f, isRtl = false))
        assertEquals(80f, logicalRevealSwipeOffset(-80f, isRtl = true))
        assertEquals(-80f, logicalRevealSwipeOffset(80f, isRtl = true))
    }

    private fun release(
        offset: Float,
        start: RevealSwipeSide? = null,
        end: RevealSwipeSide? = null,
    ): RevealSwipeRelease = resolveRevealSwipeRelease(
        rawOffsetPx = offset,
        openThresholdPx = 66f,
        startDeepThresholdPx = 150f,
        endDeepThresholdPx = 150f,
        start = start,
        end = end,
    )
}
