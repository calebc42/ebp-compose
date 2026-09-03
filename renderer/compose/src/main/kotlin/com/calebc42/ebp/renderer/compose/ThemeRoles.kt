// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The 13 neutral EBP theme roles (SPEC 18.4) as resolved colors.
 *
 * A receiver derives every toolkit-specific palette from these roles. A
 * downstream design scope may re-declare them for its own subtree, and the
 * toolkit renderers beneath it re-derive their palettes from this value, so
 * chrome such as a scaffold, top bar, or tab indicator follows the authored
 * profile instead of the ambient Emacs theme. Nothing here is a wire object.
 */
@Immutable
data class ComposeThemeRoles(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val error: Color,
    val onError: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val outline: Color,
    val success: Color,
    val warning: Color,
) {
    /** Look up a role by its wire name; null for an unknown name. */
    fun byWireName(name: String): Color? = when (name) {
        "primary" -> primary
        "on_primary" -> onPrimary
        "secondary" -> secondary
        "on_secondary" -> onSecondary
        "error" -> error
        "on_error" -> onError
        "background" -> background
        "on_background" -> onBackground
        "surface" -> surface
        "on_surface" -> onSurface
        "outline" -> outline
        "success" -> success
        "warning" -> warning
        else -> null
    }
}

/**
 * Roles a design scope has re-declared for its subtree, or null outside any
 * such scope. Toolkit renderers read this at their dispatch boundary and
 * re-theme when it changes; the ambient receiver theme remains the fallback.
 */
val LocalComposeThemeRoles = staticCompositionLocalOf<ComposeThemeRoles?> { null }
