// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.model

import com.calebc42.ebp.renderer.model.vocabulary.NodeMemberSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class NodeComponentRegistryTest {
    private class Component(
        override val nodeType: String,
        override val members: NodeMemberSet,
    ) : NodeComponent

    private fun component(type: String, membersOf: String = type) =
        Component(type, NodeMemberSet.untyped(membersOf, emptySet(), emptySet()))

    @Test
    fun resolvesOneExactOwnerPerNodeType() {
        val text = component("text")
        val button = component("button")
        val registry = NodeComponentRegistry(listOf(text, button))

        assertSame(text, registry.componentFor("text"))
        assertSame(button, registry.componentFor("button"))
        assertNull(registry.componentFor("row"))
        assertEquals(setOf("text", "button"), registry.nodeTypes)
        assertEquals(listOf(text, button), registry.components.toList())
    }

    @Test
    fun rejectsBlankTypesDuplicateOwnershipAndMismatchedMembers() {
        assertThrows(IllegalArgumentException::class.java) {
            NodeComponentRegistry(listOf(component("")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NodeComponentRegistry(listOf(component("text"), component("text")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NodeComponentRegistry(listOf(component("text", membersOf = "button")))
        }
    }
}
