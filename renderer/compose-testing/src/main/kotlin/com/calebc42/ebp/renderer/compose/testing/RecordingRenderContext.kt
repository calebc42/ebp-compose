// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.calebc42.ebp.renderer.compose.ComposeExtensionRenderContext
import com.calebc42.ebp.renderer.compose.RevealSwipeDirection
import com.calebc42.ebp.renderer.model.ActionHandoff
import com.calebc42.ebp.renderer.model.RendererActionOutcome
import com.calebc42.ebp.renderer.model.RendererEditorHost
import com.calebc42.ebp.renderer.model.RendererVolatileSecret
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A render context that records what a component asked of its host instead
 * of doing it: dispatched actions, swipe actions, published state, volatile
 * secret registrations, and scoped children. Stored drafts and epochs are
 * maps the test seeds. Children render through [renderer], which the test
 * controller points at a component registry.
 *
 * It implements the extension context too, so extension components can be
 * exercised with the same recorder.
 */
class RecordingRenderContext(
    override val surface: String = "app:test",
    override val path: String = "root",
    override val inDialog: Boolean = false,
    override val extensionId: String = "test.extension",
    override val maxFieldBytes: Int = 65_536,
    override val editorHost: RendererEditorHost = RecordingEditorHost(),
    override val volatileSecretRegistryKey: Any? = null,
    private val renderer: @Composable (JsonObject, RecordingRenderContext, Modifier) -> Unit,
) : ComposeExtensionRenderContext {
    /** One `dispatchAction` call, with the outcome callback the host would invoke. */
    data class Dispatch(
        val descriptor: JsonObject?,
        val value: JsonElement?,
        val secret: RendererVolatileSecret?,
        val sourceId: String?,
        val onOutcome: (RendererActionOutcome) -> Unit,
    ) {
        /** The descriptor's `action` name, or `builtin`, or an empty string. */
        val name: String
            get() = (descriptor?.get("action") as? JsonPrimitive)?.content
                ?: (descriptor?.get("builtin") as? JsonPrimitive)?.content
                ?: ""
    }

    /** One `state` call. */
    data class State(val id: String, val value: JsonElement?, val volatileSecret: Boolean)

    /** One `swipeAction` call. */
    data class Swipe(val descriptor: JsonObject?, val direction: RevealSwipeDirection)

    val dispatched = mutableListOf<Dispatch>()
    val states = mutableListOf<State>()
    val swipes = mutableListOf<Swipe>()

    /** Every host call in order, as `action:<name>`, `state:<id>` or `swipe:<name>`. */
    val events = mutableListOf<String>()

    /** Registered volatile-secret erasers, by stateful node id. */
    val volatileSecrets = mutableMapOf<String, () -> Unit>()

    /** Children rendered through `renderScopedChild`, with their index. */
    val scopedChildren = mutableListOf<Pair<JsonObject, Int>>()

    /** Stored drafts returned by [storeValue]; the test seeds them. */
    val store = mutableMapOf<String, JsonElement>()

    /** Presentation epochs returned by [epochOf]; absent ids read as 0. */
    val epochs = mutableMapOf<String, Long>()

    /** What [dispatchAction] reports back to the component. */
    var handoff: ActionHandoff = ActionHandoff.HandedOff

    override fun dispatchAction(
        descriptor: JsonObject?,
        value: JsonElement?,
        secret: RendererVolatileSecret?,
        sourceId: String?,
        onOutcome: (RendererActionOutcome) -> Unit,
    ): ActionHandoff {
        val dispatch = Dispatch(descriptor, value, secret, sourceId, onOutcome)
        dispatched += dispatch
        events += "action:${dispatch.name}"
        return handoff
    }

    override fun swipeAction(descriptor: JsonObject?, direction: RevealSwipeDirection) {
        swipes += Swipe(descriptor, direction)
        events += "swipe:${(descriptor?.get("action") as? JsonPrimitive)?.content.orEmpty()}"
    }

    override fun state(id: String, value: JsonElement?, volatileSecret: Boolean) {
        states += State(id, value, volatileSecret)
        events += "state:$id"
    }

    override fun registerVolatileSecret(id: String, erase: () -> Unit): () -> Unit {
        volatileSecrets[id] = erase
        return { volatileSecrets.remove(id) }
    }

    override fun storeValue(id: String): JsonElement? = store[id]

    override fun epochOf(id: String): Long = epochs[id] ?: 0L

    @Composable
    override fun renderChild(child: JsonObject, index: Int, modifier: Modifier) {
        renderer(child, this, modifier)
    }

    @Composable
    override fun renderScopedChild(child: JsonObject, index: Int, modifier: Modifier) {
        scopedChildren += child to index
        renderer(child, this, modifier)
    }

    /** Forget every recorded call; the store and epochs stay. */
    fun clear() {
        dispatched.clear()
        states.clear()
        swipes.clear()
        events.clear()
        scopedChildren.clear()
    }
}
