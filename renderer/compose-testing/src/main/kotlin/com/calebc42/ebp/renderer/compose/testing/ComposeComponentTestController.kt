// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.calebc42.ebp.renderer.compose.ComposeChromeStyles
import com.calebc42.ebp.renderer.compose.ComposeComponentRegistry
import com.calebc42.ebp.renderer.compose.ComposeExtensionRenderContext
import com.calebc42.ebp.renderer.compose.ComposeIconResolver
import com.calebc42.ebp.renderer.compose.ComposeNodeRenderContext
import com.calebc42.ebp.renderer.compose.ComposeThemeRoles
import com.calebc42.ebp.renderer.compose.RenderComponent
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Mounts one accepted node in a Compose test rule and records what it asks of
 * its host — the A2UI test-controller shape for EBP. [start] composes the
 * node once; [update] replaces it inside the same composition, which is how
 * a test exercises identity-keyed retained state and draft reconciliation.
 * Everything the component dispatched or published is on [context].
 */
class ComposeComponentTestController(
    private val rule: ComposeContentTestRule,
    render: @Composable (JsonObject, RecordingRenderContext, Modifier) -> Unit,
    /** The editor host the context exposes; a [RecordingEditorHost] by default. */
    val editorHost: RecordingEditorHost = RecordingEditorHost(),
    surface: String = "app:test",
    inDialog: Boolean = false,
    extensionId: String = "test.extension",
    private val iconResolver: ComposeIconResolver? = null,
    private val themeRoles: ComposeThemeRoles? = null,
    private val chromeStyles: ComposeChromeStyles? = null,
) {
    /** The recording host the mounted node renders against. */
    val context = RecordingRenderContext(
        surface = surface,
        inDialog = inDialog,
        extensionId = extensionId,
        editorHost = editorHost,
        renderer = render,
    )

    private val mounted = mutableStateOf<JsonObject?>(null)
    private var started = false

    /** Every dispatched action, in order. */
    val dispatched: List<RecordingRenderContext.Dispatch> get() = context.dispatched

    /** Every published state, in order. */
    val states: List<RecordingRenderContext.State> get() = context.states

    /** Every host call in order (`action:`, `state:`, `swipe:`). */
    val events: List<String> get() = context.events

    /** Compose [node] under [EbpTestSurface] and wait for idle. Call once per test. */
    fun start(node: JsonObject) {
        check(!started) { "start() was already called; use update()" }
        started = true
        mounted.value = node
        rule.setContent {
            EbpTestSurface(iconResolver, themeRoles, chromeStyles) {
                mounted.value?.let { context.renderChild(it, 0) }
            }
        }
        rule.waitForIdle()
    }

    /** Replace the mounted node in the same composition and wait for idle. */
    fun update(node: JsonObject) {
        check(started) { "call start() first" }
        mounted.value = node
        rule.waitForIdle()
    }

    /** Wait for composition, layout and effects to settle. */
    fun waitForIdle() = rule.waitForIdle()

    /** Seed the stored draft (and presentation epoch) a stateful node reads on first composition. */
    fun seedStore(id: String, value: JsonElement, epoch: Long = 0L) {
        context.store[id] = value
        context.epochs[id] = epoch
    }

    /** Forget recorded dispatches, states, swipes and events. */
    fun clearRecords() = context.clear()

    companion object {
        /** A controller rendering through a registry of base-context components. */
        fun forRegistry(
            rule: ComposeContentTestRule,
            registry: ComposeComponentRegistry<ComposeNodeRenderContext>,
            editorHost: RecordingEditorHost = RecordingEditorHost(),
            iconResolver: ComposeIconResolver? = null,
        ): ComposeComponentTestController = ComposeComponentTestController(
            rule = rule,
            render = { node, context, modifier -> RenderComponent(registry, node, context, modifier) },
            editorHost = editorHost,
            iconResolver = iconResolver,
        )

        /** A controller rendering through a registry of extension-context components. */
        fun forExtensionRegistry(
            rule: ComposeContentTestRule,
            registry: ComposeComponentRegistry<ComposeExtensionRenderContext>,
            extensionId: String,
            editorHost: RecordingEditorHost = RecordingEditorHost(),
            iconResolver: ComposeIconResolver? = null,
        ): ComposeComponentTestController = ComposeComponentTestController(
            rule = rule,
            render = { node, context, modifier -> RenderComponent(registry, node, context, modifier) },
            editorHost = editorHost,
            extensionId = extensionId,
            iconResolver = iconResolver,
        )
    }
}
