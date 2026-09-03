// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Resolves a wire `icon` identifier to a drawable vector.
 *
 * SPEC 17.1 keeps icon names as identifiers and SPEC 17.2 makes an unknown
 * name harmless, so the glyph set itself is a receiver asset rather than
 * protocol vocabulary. The composition root installs one resolver and every
 * downstream renderer reads it, which lets a Material-free design renderer
 * draw the same named glyphs as the Material renderer without depending on
 * the Material icon artifact.
 */
fun interface ComposeIconResolver {
    /** Return the vector for [name], or null when the receiver has no glyph. */
    fun resolve(name: String): ImageVector?
}

/** The installed icon resolver; null means the receiver draws no named glyphs. */
val LocalComposeIconResolver = staticCompositionLocalOf<ComposeIconResolver?> { null }
