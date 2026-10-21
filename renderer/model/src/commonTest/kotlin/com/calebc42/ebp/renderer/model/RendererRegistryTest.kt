// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RendererRegistryTest {
    private val extensions = mapOf(
        "example.design" to setOf("example.chip", "example.split"),
    )

    @Test
    fun derivesSortedProfileFromInstalledContributions() {
        val registry = RendererRegistry(
            listOf(
                RendererContribution("core", nodeTypes = setOf("text", "row")),
                RendererContribution(
                    "design",
                    nodeTypes = extensions.getValue("example.design"),
                    extensions = setOf("example.design"),
                ),
            ),
            extensionOwners = extensions,
        )
        val profile = registry.profile("core", "design")
        assertEquals(setOf("example.design"), profile.extensions)
        assertEquals(
            listOf("example.design"),
            profile.toJson().getValue("extensions").let { array ->
                (array as kotlinx.serialization.json.JsonArray).map {
                    (it as kotlinx.serialization.json.JsonPrimitive).content
                }
            },
        )
    }

    @Test
    fun rejectsExtensionNodeWithoutOwnerAdvertisement() {
        val node = extensions.getValue("example.design").first()
        val registry = RendererRegistry(
            listOf(RendererContribution("broken", nodeTypes = setOf(node))),
            extensionOwners = extensions,
        )
        assertThrows(IllegalArgumentException::class.java) { registry.profile("broken") }
    }

    @Test
    fun permitsTargetSpecificExtensionSubset() {
        val registry = RendererRegistry(
            listOf(
                RendererContribution(
                    "broken",
                    nodeTypes = setOf("example.chip"),
                    extensions = setOf("example.design"),
                ),
            ),
            extensionOwners = extensions,
        )
        assertEquals(
            setOf("example.chip"),
            registry.profile("broken").nodeTypes,
        )
    }

    @Test
    fun restrictedContributionProjectsExactMembersAndLimits() {
        val contribution = RendererContribution(
            id = "glance",
            nodeTypes = setOf("text", "box"),
            memberSupport = RendererMemberSupport(
                universal = setOf("padding"),
                nodes = mapOf(
                    "text" to setOf("text", "max_lines"),
                    "box" to setOf("children", "on_tap"),
                ),
                surface = setOf("title", "body", "empty"),
            ),
            limits = RendererTargetLimits(128, 40, 12, 8, 716_800),
        )

        val profile = RendererRegistry(listOf(contribution)).profile("glance")
        assertNotNull(profile.memberSupport)
        assertNotNull(profile.toJson()["members"])
        assertEquals(716_800, profile.limits?.maxRemoteViewsBytes)
    }

    @Test
    fun unrestrictedContributionKeepsLegacyWholeSchemaProfile() {
        val profile = RendererRegistry(
            listOf(RendererContribution("core", nodeTypes = setOf("text"))),
        ).profile("core")

        assertNull(profile.memberSupport)
        assertNull(profile.toJson()["members"])
    }

    @Test
    fun rejectsIncompleteExactNodeMap() {
        val registry = RendererRegistry(
            listOf(
                RendererContribution(
                    id = "broken",
                    nodeTypes = setOf("text", "box"),
                    memberSupport = RendererMemberSupport(
                        nodes = mapOf("text" to setOf("text")),
                    ),
                ),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            registry.profile("broken")
        }
    }
}
