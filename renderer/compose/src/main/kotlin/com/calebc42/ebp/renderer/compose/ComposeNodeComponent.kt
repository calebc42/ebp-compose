// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.calebc42.ebp.renderer.model.vocabulary.NodeMemberSet
import kotlinx.serialization.json.JsonObject

/**
 * One node type's Compose presentation: the type it owns, the typed members it
 * reads through, and one composable that presents an accepted node.
 *
 * This is the renderer-side shape the A2UI catalogs use (one object per
 * component, assembled into a registry) applied to EBP: the component consumes
 * the accepted JSON node directly through [members] — there is no parallel
 * tree — and reaches the host only through [C]. The context type parameter is
 * contravariant so a component written against the base
 * [ComposeNodeRenderContext] installs into a registry whose context is richer
 * (a Material context, say), while a component that needs the richer context
 * cannot be installed where only the base one is available.
 */
interface ComposeNodeComponent<in C : ComposeNodeRenderContext> {
    /** The node discriminator (`t`) this component presents. */
    val nodeType: String

    /** The generated member set this component reads through; `members.nodeType == nodeType`. */
    val members: NodeMemberSet

    /**
     * Whether this component owns the accepted shape of [node]; deterministic
     * and side-effect free. A registry caller that receives false falls through
     * to the next tier exactly as [ComposeCanonicalNodeOverride.appliesTo] does.
     */
    fun appliesTo(node: JsonObject): Boolean = true

    /** Present one admitted [node] on the interaction-owning [modifier]. */
    @Composable
    fun render(node: JsonObject, context: C, modifier: Modifier)
}

/**
 * Exact node-type dispatch for one tier of components.
 *
 * Ownership is singular: two components cannot claim one node type, a
 * component's member set must name the type it presents, and a type is
 * either owned or absent — callers degrade an absent type per SPEC §16.2
 * rather than guessing.
 */
@Stable
class ComposeComponentRegistry<C : ComposeNodeRenderContext>(
    components: Iterable<ComposeNodeComponent<C>>,
) {
    private val byType: Map<String, ComposeNodeComponent<C>> = buildMap {
        for (component in components) {
            require(component.nodeType.isNotBlank()) { "component node type is blank" }
            require(component.members.nodeType == component.nodeType) {
                "component ${component.nodeType} reads through members of " +
                    component.members.nodeType
            }
            require(put(component.nodeType, component) == null) {
                "two components own node type ${component.nodeType}"
            }
        }
    }

    /** Every owned node type. */
    val nodeTypes: Set<String> get() = byType.keys

    /** Every installed component, in installation order. */
    val components: Collection<ComposeNodeComponent<C>> get() = byType.values

    /** The component owning [nodeType], or null when this tier has none. */
    fun componentFor(nodeType: String): ComposeNodeComponent<C>? = byType[nodeType]

    companion object {
        /** A registry owning nothing. */
        fun <C : ComposeNodeRenderContext> empty(): ComposeComponentRegistry<C> =
            ComposeComponentRegistry(emptyList())
    }
}

/**
 * Present [node] through [registry] in [context], degrading an unowned type
 * to its `children` as a neutral vertical sequence (SPEC §16.2) and rendering
 * nothing when it has none.
 */
@Composable
fun <C : ComposeNodeRenderContext> RenderComponent(
    registry: ComposeComponentRegistry<C>,
    node: JsonObject,
    context: C,
    modifier: Modifier = Modifier,
) {
    val type = (node["t"] as? kotlinx.serialization.json.JsonPrimitive)?.content
    val component = type?.let(registry::componentFor)?.takeIf { it.appliesTo(node) }
    if (component != null) {
        component.render(node, context, modifier)
    } else {
        RenderChildrenFallback(node, context, modifier)
    }
}

/** The SPEC §16.2 degradation: children as a neutral column, or nothing. */
@Composable
fun RenderChildrenFallback(
    node: JsonObject,
    context: ComposeNodeRenderContext,
    modifier: Modifier = Modifier,
) {
    val children = (node["children"] as? kotlinx.serialization.json.JsonArray) ?: return
    androidx.compose.foundation.layout.Column(modifier) {
        children.forEachIndexed { index, child ->
            (child as? JsonObject)?.let { context.renderChild(it, index) }
        }
    }
}

/**
 * The component view of one [ComposeNodeExtension]: one component per owned
 * node type, each delegating to the extension's `render`. [members] supplies
 * the typed member set per node type; a type without one is projected
 * untyped, as a format-1 manifest would be.
 */
fun ComposeNodeExtension.asComponents(
    members: Map<String, NodeMemberSet> = emptyMap(),
): List<ComposeNodeComponent<ComposeExtensionRenderContext>> = nodeTypes.sorted().map { type ->
    val extension = this
    object : ComposeNodeComponent<ComposeExtensionRenderContext> {
        override val nodeType: String = type
        override val members: NodeMemberSet =
            members[type] ?: NodeMemberSet.untyped(type, emptySet(), emptySet())

        @Composable
        override fun render(
            node: JsonObject,
            context: ComposeExtensionRenderContext,
            modifier: Modifier,
        ) = extension.render(node, context, modifier)
    }
}

/**
 * The component view of one [ComposeCanonicalNodeOverride] for [nodeType],
 * which must be one of the override's `nodeTypes`; `appliesTo` is preserved.
 */
fun ComposeCanonicalNodeOverride.asComponent(
    nodeType: String,
    members: NodeMemberSet,
): ComposeNodeComponent<ComposeNodeRenderContext> {
    require(nodeType in nodeTypes) { "$id does not own $nodeType" }
    val override = this
    return object : ComposeNodeComponent<ComposeNodeRenderContext> {
        override val nodeType: String = nodeType
        override val members: NodeMemberSet = members
        override fun appliesTo(node: JsonObject): Boolean = override.appliesTo(node)

        @Composable
        override fun render(node: JsonObject, context: ComposeNodeRenderContext, modifier: Modifier) =
            override.render(node, context, modifier)
    }
}
