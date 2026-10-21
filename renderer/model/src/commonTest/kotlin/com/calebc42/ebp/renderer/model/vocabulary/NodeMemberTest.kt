// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.model.vocabulary

import com.calebc42.ebp.renderer.model.vocabulary.generated.ButtonMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.ButtonVariant
import com.calebc42.ebp.renderer.model.vocabulary.generated.CheckboxMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.TextInputMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.TextInputVariant
import com.calebc42.ebp.renderer.model.vocabulary.generated.TextMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.TextStyle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The strict, total reader contract of every member class. */
class NodeMemberTest {
    private fun node(json: String): JsonObject = Json.parseToJsonElement(json) as JsonObject

    @Test
    fun stringsAndBooleansAndNumbersAreNeverCoerced() {
        val n = node("""{"text":1,"selectable":"true","max_lines":"3","enabled":false}""")
        assertNull(TextMembers.Text.read(n))
        assertNull(TextMembers.Selectable.read(n))
        assertNull(TextMembers.MaxLines.read(n))
        assertEquals(false, ButtonMembers.Enabled.read(n))
        assertEquals("hi", TextMembers.Text.read(node("""{"text":"hi"}""")))
    }

    @Test
    fun integersAreAcceptedByValueAndBoundedByTheTag() {
        assertEquals(2, TextMembers.MaxLines.read(node("""{"max_lines":2.0}""")))
        assertNull(TextMembers.MaxLines.read(node("""{"max_lines":2.5}""")))
        assertNull(TextMembers.MaxLines.read(node("""{"max_lines":0}""")))
        val bounded = IntMember("n", required = false, tag = "integer-1-12", minimum = 1, maximum = 12)
        assertEquals(12, bounded.read(node("""{"n":12}""")))
        assertNull(bounded.read(node("""{"n":13}""")))
    }

    @Test
    fun enumValuesOutsideTheDomainReadAsNullButStayReadableRaw() {
        val n = node("""{"style":"bogus","variant":"tonal"}""")
        assertNull(TextMembers.Style.read(n))
        assertEquals("bogus", TextMembers.Style.readRaw(n))
        assertEquals(TextStyle.Body, TextMembers.Style.read(node("""{"style":"body"}""")))
        assertEquals(ButtonVariant.Tonal, ButtonMembers.Variant.read(n))
        assertEquals("text.style", TextStyle.contractKey)
        assertEquals(TextStyle.entries, TextStyle.values)
    }

    @Test
    fun booleanOrAutoReadsBothSpellings() {
        assertEquals(BooleanOrAuto.True, ButtonMembers.Expanded.read(node("""{"expanded":true}""")))
        assertEquals(BooleanOrAuto.Auto, ButtonMembers.Expanded.read(node("""{"expanded":"auto"}""")))
        assertNull(ButtonMembers.Expanded.read(node("""{"expanded":"yes"}""")))
    }

    @Test
    fun structuredMembersStayJsonAndAreTotal() {
        val n = node("""{"children":[{"t":"text","text":"a"},1],"on_tap":{"action":"x.y"},"selection":[1,2.5]}""")
        assertNull(TextInputMembers.OnChange.read(n))
        assertEquals("x.y", ButtonMembers.OnTap.read(n)?.get("action")?.let { (it as JsonPrimitive).content })
        assertNull(NodeArrayMember("children", required = true).read(n))
        assertEquals(1.0 to 2.5, TextInputMembers.Selection.read(n))
        assertNull(TextInputMembers.Selection.read(node("""{"selection":[1]}""")))
        assertEquals(
            StringOrNumber.Numeric(700.0),
            TextMembers.FontWeight.read(node("""{"font_weight":700}""")),
        )
        assertEquals(
            StringOrNumber.Text("bold"),
            TextMembers.FontWeight.read(node("""{"font_weight":"bold"}""")),
        )
    }

    @Test
    fun propertiesApplyContractDefaultsBeforeRendererDefaults() {
        val empty = NodeProperties(node("""{"t":"text_input","id":"f"}"""), TextInputMembers)
        assertEquals(TextInputVariant.Outlined, empty.getOr(TextInputMembers.Variant, TextInputVariant.Filled))
        assertEquals(false, NodeProperties(node("{}"), CheckboxMembers).getOr(CheckboxMembers.Checked, true))
        assertEquals(true, NodeProperties(node("{}"), ButtonMembers).getOr(ButtonMembers.Enabled, true))
        assertNull(empty[TextInputMembers.Value])
        assertFalse(empty.has(TextInputMembers.Value))
        assertTrue(empty.has(TextInputMembers.Id))
        assertEquals("f", empty.raw(TextInputMembers.Id)?.let { (it as JsonPrimitive).content })
    }

    @Test
    fun memberSetsPartitionAndIndexTheirMembers() {
        assertEquals(listOf("text"), TextMembers.required.map { it.key })
        assertEquals(TextMembers.Style, TextMembers.member("style"))
        assertNull(TextMembers.member("nope"))
        assertTrue(TextMembers.keys.containsAll(setOf("text", "style", "max_lines")))
        val untyped = NodeMemberSet.untyped("x.y", setOf("b", "a"), setOf("c"))
        assertEquals(listOf("a", "b", "c"), untyped.all.map { it.key })
        assertTrue(untyped.all.all { it is AnyMember && it.tag == "untyped" })
        val duplicate = object : NodeMemberSet("dup") {
            override val required = listOf<NodeMember<*>>(StringMember("k", required = true))
            override val optional = listOf<NodeMember<*>>(StringMember("k", required = false))
        }
        try {
            duplicate.member("k")
            fail("duplicate keys must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("dup"))
        }
    }
}
