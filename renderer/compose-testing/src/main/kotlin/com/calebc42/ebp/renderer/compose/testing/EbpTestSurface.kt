// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.calebc42.ebp.renderer.compose.ComposeChromeStyles
import com.calebc42.ebp.renderer.compose.ComposeIconResolver
import com.calebc42.ebp.renderer.compose.ComposeThemeRoles
import com.calebc42.ebp.renderer.compose.LocalComposeChromeStyles
import com.calebc42.ebp.renderer.compose.LocalComposeIconResolver
import com.calebc42.ebp.renderer.compose.LocalComposeThemeRoles
import com.calebc42.ebp.renderer.compose.ProvideRevealSwipeCoordinator

/**
 * The composition environment the Foundation renderer expects: the icon,
 * theme-role and chrome-style locals (null means "none installed", which the
 * renderers tolerate) and the reveal-swipe coordinator.
 */
@Composable
fun EbpTestSurface(
    iconResolver: ComposeIconResolver? = null,
    themeRoles: ComposeThemeRoles? = null,
    chromeStyles: ComposeChromeStyles? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalComposeIconResolver provides iconResolver,
        LocalComposeThemeRoles provides themeRoles,
        LocalComposeChromeStyles provides chromeStyles,
    ) {
        ProvideRevealSwipeCoordinator(content)
    }
}
