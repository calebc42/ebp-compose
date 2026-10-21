// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Exact optional target support when a renderer cannot honor whole schemas. */
data class RendererMemberSupport(
    val universal: Set<String> = emptySet(),
    val nodes: Map<String, Set<String>>,
    val semantics: Set<String> = emptySet(),
    val surface: Set<String> = emptySet(),
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("universal", strings(universal))
        put("nodes", buildJsonObject {
            nodes.toSortedMap().forEach { (node, members) ->
                put(node, strings(members))
            }
        })
        put("semantics", strings(semantics))
        put("surface", strings(surface))
    }

    private fun strings(values: Set<String>) =
        JsonArray(values.sorted().map(::JsonPrimitive))
}

/** Target-local renderer budgets advertised beside its vocabulary. */
data class RendererTargetLimits(
    val maxNodes: Int,
    val maxLazyItems: Int,
    val maxNodeDepth: Int,
    val maxSizeVariants: Int,
    val maxRemoteViewsBytes: Int,
) {
    init {
        require(
            listOf(
                maxNodes,
                maxLazyItems,
                maxNodeDepth,
                maxSizeVariants,
                maxRemoteViewsBytes,
            ).all { it > 0 },
        ) { "Renderer target limits must be positive" }
    }

    fun toJson(): JsonObject = buildJsonObject {
        put("max_nodes", maxNodes)
        put("max_lazy_items", maxLazyItems)
        put("max_node_depth", maxNodeDepth)
        put("max_size_variants", maxSizeVariants)
        put("max_remote_views_bytes", maxRemoteViewsBytes)
    }
}

/**
 * One independently owned renderer slice.
 *
 * The contribution is implementation metadata, not another UI tree: renderers
 * still consume the accepted EBP JSON document directly. A contribution names
 * only behavior implemented by that slice, which lets a receiver derive its
 * welcome profile from installed code instead of maintaining a parallel list.
 */
data class RendererContribution(
    val id: String,
    val nodeTypes: Set<String> = emptySet(),
    val builtins: Set<String> = emptySet(),
    val features: Set<String> = emptySet(),
    val extensions: Set<String> = emptySet(),
    val memberSupport: RendererMemberSupport? = null,
    val limits: RendererTargetLimits? = null,
)

/** A complete receiver target profile assembled from renderer contributions. */
data class RendererProfile(
    val nodeTypes: Set<String>,
    val builtins: Set<String>,
    val features: Set<String>,
    val extensions: Set<String>,
    val memberSupport: RendererMemberSupport? = null,
    val limits: RendererTargetLimits? = null,
) {
    /** EBP 3 §10.2 wire projection with stable lexical ordering. */
    fun toJson(): JsonObject = buildJsonObject {
        put("node_types", strings(nodeTypes))
        put("builtins", strings(builtins))
        put("features", strings(features))
        put("extensions", strings(extensions))
        memberSupport?.let { put("members", it.toJson()) }
        limits?.let { put("limits", it.toJson()) }
    }

    private fun strings(values: Set<String>) =
        JsonArray(values.sorted().map(::JsonPrimitive))
}

/**
 * Validates and combines the renderer code actually installed in a target.
 *
 * Extension nodes are dual-gated: a target may implement a subset of an
 * extension, but an extension node can never be advertised without its owner.
 * This is the receiver-side proof behind the EBP 3 profile rather than a
 * handwritten capability claim.
 */
class RendererRegistry(
    contributions: Iterable<RendererContribution>,
    /** Renderer-owned node ownership, injected by the selected implementation. */
    private val extensionOwners: Map<String, Set<String>> = emptyMap(),
) {
    private val contributionList = contributions.toList()
    private val contributions = contributionList.associateBy { contribution ->
        require(contribution.id.isNotBlank()) { "Renderer contribution id is blank" }
        contribution.id
    }.also { indexed ->
        require(indexed.size == contributionList.size) {
            "Renderer contribution ids must be unique"
        }
    }

    fun profile(vararg contributionIds: String): RendererProfile {
        val selected = contributionIds.map { id ->
            requireNotNull(contributions[id]) { "Unknown renderer contribution: $id" }
        }
        val profile = RendererProfile(
            nodeTypes = selected.flatMap { it.nodeTypes }.toSet(),
            builtins = selected.flatMap { it.builtins }.toSet(),
            features = selected.flatMap { it.features }.toSet(),
            extensions = selected.flatMap { it.extensions }.toSet(),
            memberSupport = selected.map { it.memberSupport }
                .takeIf { supports -> supports.all { it != null } }
                ?.filterNotNull()
                ?.let(::combineMemberSupport),
            limits = selected.mapNotNull { it.limits }
                .takeIf { it.isNotEmpty() }
                ?.let(::combineLimits),
        )
        validateExtensions(profile)
        validateMemberSupport(profile)
        return profile
    }

    private fun combineMemberSupport(
        supports: List<RendererMemberSupport>,
    ): RendererMemberSupport = RendererMemberSupport(
        universal = supports.flatMap { it.universal }.toSet(),
        nodes = supports.flatMap { it.nodes.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> values.flatten().toSet() },
        semantics = supports.flatMap { it.semantics }.toSet(),
        surface = supports.flatMap { it.surface }.toSet(),
    )

    private fun combineLimits(limits: List<RendererTargetLimits>) =
        RendererTargetLimits(
            maxNodes = limits.minOf { it.maxNodes },
            maxLazyItems = limits.minOf { it.maxLazyItems },
            maxNodeDepth = limits.minOf { it.maxNodeDepth },
            maxSizeVariants = limits.minOf { it.maxSizeVariants },
            maxRemoteViewsBytes = limits.minOf { it.maxRemoteViewsBytes },
        )

    private fun validateMemberSupport(profile: RendererProfile) {
        val support = profile.memberSupport ?: return
        require(support.nodes.keys == profile.nodeTypes) {
            "Exact member support must name every and only advertised node type"
        }
        require("semantics" !in support.universal || support.semantics.isNotEmpty()) {
            "Advertising universal semantics requires at least one Semantics member"
        }
    }

    private fun validateExtensions(profile: RendererProfile) {
        val owners = extensionOwners.entries.flatMap { (extension, nodes) ->
            nodes.map { it to extension }
        }.toMap()
        profile.nodeTypes.forEach { node ->
            owners[node]?.let { owner ->
                require(owner in profile.extensions) {
                    "Extension node $node requires $owner"
                }
            }
        }
        profile.extensions.forEach { extension ->
            requireNotNull(extensionOwners[extension]) {
                "Unknown renderer extension: $extension"
            }
        }
    }
}
