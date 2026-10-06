// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.calebc42.ebp.renderer.model.vocabulary.NodeMemberSet
import com.calebc42.ebp.renderer.model.vocabulary.generated.ButtonMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.TextMembers
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeComponentRegistryTest {
    private class Stub(
        override val nodeType: String,
        override val members: NodeMemberSet,
        private val applies: (JsonObject) -> Boolean = { true },
    ) : ComposeNodeComponent<ComposeNodeRenderContext> {
        override fun appliesTo(node: JsonObject): Boolean = applies(node)

        @Composable
        override fun render(node: JsonObject, context: ComposeNodeRenderContext, modifier: Modifier) = Unit
    }

    @Test
    fun resolvesOneExactOwnerPerNodeType() {
        val text = Stub("text", TextMembers)
        val button = Stub("button", ButtonMembers)
        val registry = ComposeComponentRegistry(listOf(text, button))

        assertSame(text, registry.componentFor("text"))
        assertSame(button, registry.componentFor("button"))
        assertNull(registry.componentFor("row"))
        assertEquals(setOf("text", "button"), registry.nodeTypes)
        assertEquals(listOf(text, button), registry.components.toList())
        assertTrue(ComposeComponentRegistry.empty<ComposeNodeRenderContext>().nodeTypes.isEmpty())
    }

    @Test
    fun rejectsDuplicateOwnershipAndMismatchedMembers() {
        assertThrows(IllegalArgumentException::class.java) {
            ComposeComponentRegistry(listOf(Stub("text", TextMembers), Stub("text", TextMembers)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ComposeComponentRegistry(listOf(Stub("text", ButtonMembers)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ComposeComponentRegistry(listOf(Stub(" ", NodeMemberSet.untyped(" ", emptySet(), emptySet()))))
        }
    }

    @Test
    fun extensionAndOverrideAdaptersKeepOwnershipAndApplicability() {
        val extension = object : ComposeNodeExtension {
            override val id = "example.compose"
            override val extensionId = "example.design"
            override val nodeTypes = setOf("example.two", "example.one")

            @Composable
            override fun render(node: JsonObject, context: ComposeExtensionRenderContext, modifier: Modifier) = Unit
        }
        val components = extension.asComponents(
            members = mapOf("example.one" to NodeMemberSet.untyped("example.one", setOf("label"), emptySet())),
        )
        assertEquals(listOf("example.one", "example.two"), components.map { it.nodeType })
        assertEquals(listOf("label"), components[0].members.all.map { it.key })
        assertTrue(components[1].members.all.isEmpty())
        val registry = ComposeComponentRegistry(components)
        assertEquals(setOf("example.one", "example.two"), registry.nodeTypes)

        val override = object : ComposeCanonicalNodeOverride {
            override val id = "example.text"
            override val designScope = "example.design"
            override val nodeTypes = setOf("text")
            override fun appliesTo(node: JsonObject): Boolean = !node.containsKey("syntax")

            @Composable
            override fun render(node: JsonObject, context: ComposeNodeRenderContext, modifier: Modifier) = Unit
        }
        val component = override.asComponent("text", TextMembers)
        assertEquals("text", component.nodeType)
        assertTrue(component.appliesTo(buildJsonObject { put("t", "text") }))
        assertFalse(component.appliesTo(buildJsonObject { put("t", "text"); put("syntax", "elisp") }))
        assertThrows(IllegalArgumentException::class.java) { override.asComponent("button", ButtonMembers) }
    }
}
