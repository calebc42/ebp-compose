// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.calebc42.ebp.wire.CORE_NODE_SET
import com.calebc42.ebp.wire.CompletionNarrowing
import com.calebc42.ebp.wire.CompletionOfferView
import com.calebc42.ebp.wire.ScalarPos
import com.calebc42.ebp.renderer.model.ActionHandoff
import com.calebc42.ebp.renderer.model.CandidateDocument
import com.calebc42.ebp.renderer.model.CompletionOffer
import com.calebc42.ebp.renderer.model.EditorAnnotationState
import com.calebc42.ebp.renderer.model.EditorConnectionPhase
import com.calebc42.ebp.renderer.model.EditorEditOutcome
import com.calebc42.ebp.renderer.model.EditorMirror
import com.calebc42.ebp.renderer.model.RendererActionOutcome
import com.calebc42.ebp.renderer.model.RendererContribution
import com.calebc42.ebp.renderer.model.RendererEditorHost
import com.calebc42.ebp.renderer.model.RendererVolatileSecret
import com.calebc42.ebp.renderer.model.vocabulary.NodeMemberSet
import com.calebc42.ebp.renderer.model.vocabulary.NodeProperties
import com.calebc42.ebp.renderer.model.vocabulary.generated.BoxMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.ButtonMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.ColumnMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.DividerMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.RowMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.SpacerMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.TextInputMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.TextMembers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The renderer-neutral Compose contribution required by every app target. */
val CoreComposeContribution = RendererContribution(
    id = "compose.core",
    nodeTypes = CORE_NODE_SET,
)

/** Effects owned by the host; core Compose only presents accepted EBP data. */
fun interface CoreComposeActionSink {
    fun dispatch(descriptor: JsonObject)
}

/**
 * The Core Node Set (SPEC §16.2) as Foundation components: one object per
 * node type, each reading the accepted JSON through its generated member set
 * and reaching the host only through the render context.
 *
 * They import neither Material nor a design-system token type. Product
 * renderers install these objects as their canonical tier, delegate to them,
 * or replace them while keeping the same protocol semantics.
 */
object CoreTextComponent : ComposeNodeComponent<ComposeNodeRenderContext> {
    override val nodeType: String = "text"
    override val members: NodeMemberSet = TextMembers

    @Composable
    override fun render(node: JsonObject, context: ComposeNodeRenderContext, modifier: Modifier) {
        val properties = NodeProperties(node, TextMembers)
        BasicText(properties.getOr(TextMembers.Text, ""), modifier.coreSemantics(node, context))
    }
}

/** `row`: children in a Foundation [Row]. */
object CoreRowComponent : ComposeNodeComponent<ComposeNodeRenderContext> {
    override val nodeType: String = "row"
    override val members: NodeMemberSet = RowMembers

    @Composable
    override fun render(node: JsonObject, context: ComposeNodeRenderContext, modifier: Modifier) {
        Row(modifier.coreSemantics(node, context)) {
            CoreChildren(NodeProperties(node, RowMembers)[RowMembers.Children], context)
        }
    }
}

/** `column`: children in a Foundation [Column]. */
object CoreColumnComponent : ComposeNodeComponent<ComposeNodeRenderContext> {
    override val nodeType: String = "column"
    override val members: NodeMemberSet = ColumnMembers

    @Composable
    override fun render(node: JsonObject, context: ComposeNodeRenderContext, modifier: Modifier) {
        Column(modifier.coreSemantics(node, context)) {
            CoreChildren(NodeProperties(node, ColumnMembers)[ColumnMembers.Children], context)
        }
    }
}

/** `box`: children stacked in a Foundation [Box]. */
object CoreBoxComponent : ComposeNodeComponent<ComposeNodeRenderContext> {
    override val nodeType: String = "box"
    override val members: NodeMemberSet = BoxMembers

    @Composable
    override fun render(node: JsonObject, context: ComposeNodeRenderContext, modifier: Modifier) {
        Box(modifier.coreSemantics(node, context)) {
            CoreChildren(NodeProperties(node, BoxMembers)[BoxMembers.Children], context)
        }
    }
}

/** `spacer`: a fixed 8dp gap. */
object CoreSpacerComponent : ComposeNodeComponent<ComposeNodeRenderContext> {
    override val nodeType: String = "spacer"
    override val members: NodeMemberSet = SpacerMembers

    @Composable
    override fun render(node: JsonObject, context: ComposeNodeRenderContext, modifier: Modifier) {
        Spacer(modifier.coreSemantics(node, context).size(8.dp))
    }
}

/** `divider`: a one-pixel neutral rule. */
object CoreDividerComponent : ComposeNodeComponent<ComposeNodeRenderContext> {
    override val nodeType: String = "divider"
    override val members: NodeMemberSet = DividerMembers

    @Composable
    override fun render(node: JsonObject, context: ComposeNodeRenderContext, modifier: Modifier) {
        Spacer(modifier.coreSemantics(node, context).background(Color.Gray).size(1.dp))
    }
}

/** `button`: a clickable label that hands `on_tap` to the host. */
object CoreButtonComponent : ComposeNodeComponent<ComposeNodeRenderContext> {
    override val nodeType: String = "button"
    override val members: NodeMemberSet = ButtonMembers

    @Composable
    override fun render(node: JsonObject, context: ComposeNodeRenderContext, modifier: Modifier) {
        val properties = NodeProperties(node, ButtonMembers)
        val descriptor = properties[ButtonMembers.OnTap]
        Box(
            modifier
                .coreSemantics(node, context)
                .clickable(enabled = descriptor != null) {
                    descriptor?.let { context.action(it) }
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            BasicText(properties.getOr(ButtonMembers.Label, ""))
        }
    }
}

/**
 * `text_input`: a plain field seeded from the authored value. Each edit is
 * published through the host's state path (SPEC §14.6 allows publishing
 * without delay); the host's own draft reconciliation and limits are the
 * shared controllers' business, which a product renderer adopts through
 * [rememberTextInputBinding].
 */
object CoreTextInputComponent : ComposeNodeComponent<ComposeNodeRenderContext> {
    override val nodeType: String = "text_input"
    override val members: NodeMemberSet = TextInputMembers

    @Composable
    override fun render(node: JsonObject, context: ComposeNodeRenderContext, modifier: Modifier) {
        val properties = NodeProperties(node, TextInputMembers)
        val id = properties[TextInputMembers.Id]
        var value by remember(node) {
            mutableStateOf(properties.getOr(TextInputMembers.Value, ""))
        }
        BasicTextField(
            value = value,
            onValueChange = {
                value = it
                if (id != null) context.state(id, JsonPrimitive(it))
            },
            modifier = modifier.coreSemantics(node, context),
        )
    }
}

/** The core tier as a registry; a product renderer may install or wrap it. */
val CoreComponents: ComposeComponentRegistry<ComposeNodeRenderContext> = ComposeComponentRegistry(
    listOf(
        CoreTextComponent,
        CoreRowComponent,
        CoreColumnComponent,
        CoreBoxComponent,
        CoreSpacerComponent,
        CoreDividerComponent,
        CoreButtonComponent,
        CoreTextInputComponent,
    ),
)

/** Present [node] through [CoreComponents], degrading other types per SPEC §16.2. */
@Composable
fun RenderCoreComponent(
    node: JsonObject,
    context: ComposeNodeRenderContext,
    modifier: Modifier = Modifier,
) = RenderComponent(CoreComponents, node, context, modifier)

/**
 * The sink-only entry point: a host that owns nothing but action delivery.
 * State publication and stored drafts are no-ops here; callers that need
 * them supply a real [ComposeNodeRenderContext] to [RenderCoreComponent].
 */
@Composable
fun RenderCoreComposeNode(
    node: JsonObject,
    actions: CoreComposeActionSink,
    modifier: Modifier = Modifier,
) {
    val context = remember(actions) { CoreSinkRenderContext(actions) }
    RenderCoreComponent(node, context, modifier)
}

private fun Modifier.coreSemantics(node: JsonObject, context: ComposeNodeRenderContext): Modifier =
    ebpSemantics(node = node, onAction = { context.action(it) })

@Composable
private fun CoreChildren(children: List<JsonObject>?, context: ComposeNodeRenderContext) {
    children?.forEachIndexed { index, child -> context.renderChild(child, index) }
}

/** A context whose only host capability is delivering descriptors to [actions]. */
private class CoreSinkRenderContext(
    private val actions: CoreComposeActionSink,
) : ComposeNodeRenderContext {
    override val surface: String = "app:core"
    override val path: String = ""
    override val inDialog: Boolean = false
    override val maxFieldBytes: Int = 65_536
    override val editorHost: RendererEditorHost = InertEditorHost

    override fun dispatchAction(
        descriptor: JsonObject?,
        value: JsonElement?,
        secret: RendererVolatileSecret?,
        sourceId: String?,
        onOutcome: (RendererActionOutcome) -> Unit,
    ): ActionHandoff {
        descriptor ?: return ActionHandoff.Ignored
        actions.dispatch(descriptor)
        return ActionHandoff.HandedOff
    }

    override fun swipeAction(descriptor: JsonObject?, direction: RevealSwipeDirection) {
        descriptor?.let(actions::dispatch)
    }

    override fun state(id: String, value: JsonElement?, volatileSecret: Boolean) = Unit
    override fun storeValue(id: String): JsonElement? = null
    override fun epochOf(id: String): Long = 0L

    @Composable
    override fun renderChild(child: JsonObject, index: Int, modifier: Modifier) {
        RenderCoreComponent(child, this, modifier)
    }
}

/** An editor host that is always ready and does nothing; for sink-only rendering. */
internal object InertEditorHost : RendererEditorHost {
    override val maxEditorBytes: Int = 262_144
    override val editorConnectionPhase = MutableStateFlow(EditorConnectionPhase.READY)
    override val editorMirrors = MutableStateFlow<Map<Pair<String, String>, EditorMirror>>(emptyMap())
    override val editorAnnotations =
        MutableStateFlow<Map<Pair<String, String>, EditorAnnotationState>>(emptyMap())
    override val completionOffers =
        MutableStateFlow<Map<Pair<String, String>, CompletionOffer>>(emptyMap())
    override val completionOfferViews =
        MutableStateFlow<Map<Pair<String, String>, CompletionOfferView>>(emptyMap())
    override val candidateDocuments =
        MutableStateFlow<Map<Pair<String, String>, CandidateDocument>>(emptyMap())
    override var completionNarrowing: CompletionNarrowing = CompletionNarrowing.STRICT
    override fun requestEditorCompletion(document: String, editorId: String) = Unit
    override fun selectEditorCompletion(document: String, editorId: String, label: String, insert: String) = Unit
    override fun requestCandidateDocument(document: String, editorId: String, index: Int, epoch: Long) = Unit
    override fun publishEditorEdit(
        document: String,
        editorId: String,
        start: ScalarPos,
        deletedScalars: Int,
        inserted: String,
        base: String,
        onOutcome: (EditorEditOutcome) -> Unit,
    ) = Unit
    override fun publishEditorCaret(
        document: String,
        editorId: String,
        cursorUtf16: Int,
        selectionStartUtf16: Int,
        selectionEndUtf16: Int,
    ) = Unit
    override fun dispatchEditorCommand(
        surface: String,
        document: String,
        editorId: String,
        command: String,
        cursorUtf16: Int,
        selectionStartUtf16: Int,
        selectionEndUtf16: Int,
    ) = Unit
}
