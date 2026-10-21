// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle

/**
 * Resolved styling for the chrome a toolkit renderer draws on its own:
 * tab strips, navigation rails, drawer sheets, snackbars.
 *
 * Content nodes under a design scope are drawn by the scope's own
 * renderer, but the scaffold chrome around them stays the toolkit's, and
 * without this value it keeps the toolkit's typography and shapes while
 * everything inside follows the profile. A design scope resolves its
 * chrome slots into this neutral form; a toolkit renderer reads each field
 * where it draws the matching piece and keeps its own default for a null.
 * A text style carries only the properties the profile set, so the
 * toolkit merges it over its own base style. Nothing here is a wire object.
 */
@Immutable
data class ComposeChromeStyles(
    val tabLabel: TextStyle? = null,
    val tabSelectedLabelColor: Color? = null,
    val tabIndicator: Color? = null,
    val railLabel: TextStyle? = null,
    val railIndicator: Color? = null,
    val drawerContainer: Color? = null,
    val drawerShape: Shape? = null,
    val snackbarContainer: Color? = null,
    val snackbarContent: Color? = null,
    val snackbarShape: Shape? = null,
) {
    /** True when no field is set, so a provider can decline to shadow a parent. */
    val isEmpty: Boolean
        get() = this == EMPTY

    companion object {
        val EMPTY = ComposeChromeStyles()
    }
}

/**
 * Chrome styles the nearest design scope resolved, or null outside any
 * such scope. Toolkit renderers read this where they draw chrome; the
 * toolkit's own defaults remain the fallback for every null field.
 */
val LocalComposeChromeStyles = staticCompositionLocalOf<ComposeChromeStyles?> { null }
