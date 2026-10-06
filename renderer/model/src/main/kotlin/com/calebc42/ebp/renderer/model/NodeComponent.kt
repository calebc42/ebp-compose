// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.model

import com.calebc42.ebp.renderer.model.vocabulary.NodeMemberSet

/**
 * What every per-node component declares, whatever toolkit presents it: the
 * node type it owns and the generated member set it reads through.
 *
 * Toolkit modules add the presentation entry point in their own subtypes (a
 * Compose UI composable on a `Modifier`, a Glance composable on a
 * `GlanceModifier`); this base is what a [NodeComponentRegistry] and a
 * catalog export see.
 */
interface NodeComponent {
    /** The node discriminator (`t`) this component presents. */
    val nodeType: String

    /** The generated member set this component reads through; `members.nodeType == nodeType`. */
    val members: NodeMemberSet
}

/**
 * Exact node-type dispatch for one tier of components.
 *
 * Ownership is singular: two components cannot claim one node type, a
 * component's member set must name the type it presents, and a type is
 * either owned or absent — callers degrade an absent type per SPEC §16.2
 * rather than guessing.
 */
open class NodeComponentRegistry<T : NodeComponent>(components: Iterable<T>) {
    private val byType: Map<String, T> = buildMap {
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
    val components: Collection<T> get() = byType.values

    /** The component owning [nodeType], or null when this tier has none. */
    fun componentFor(nodeType: String): T? = byType[nodeType]
}
