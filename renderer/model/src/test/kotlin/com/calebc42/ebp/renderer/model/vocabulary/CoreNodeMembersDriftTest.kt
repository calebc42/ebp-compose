// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.model.vocabulary

import com.calebc42.ebp.renderer.model.vocabulary.generated.CORE_NODE_ENUMS
import com.calebc42.ebp.renderer.model.vocabulary.generated.CORE_NODE_MEMBERS
import com.calebc42.ebp.renderer.model.vocabulary.generated.CheckboxMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.TextInputMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.UniversalMembers
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Re-reads ebp/contract.json and pins the generated member sets and enum
 * domains to it, the way `VocabularyDriftTest` pins `Vocabulary.kt`: the
 * committed projection must be the one `tools/gen-node-members.py` derives.
 */
class CoreNodeMembersDriftTest {
    private val contract = Json.parseToJsonElement(
        File(System.getProperty("ebp.dir") ?: error("ebp.dir is not set"), "contract.json").readText(),
    ).jsonObject
    private val nodeSchema = contract.getValue("node_schema").jsonObject
    private val fieldTypes = contract.getValue("field_types").jsonObject
        .mapValues { it.value.jsonPrimitive.content }
    private val enums = contract.getValue("enums").jsonObject
        .mapValues { entry -> entry.value.jsonArray.map { it.jsonPrimitive.content } }
    private val hookKeys = contract.getValue("actions").jsonObject.getValue("hook_keys")
        .jsonArray.map { it.jsonPrimitive.content }.toSet()
    private val universal = contract.getValue("universal_node_attributes")
        .jsonArray.map { it.jsonPrimitive.content }

    private fun row(node: String, group: String): List<String> =
        nodeSchema.getValue(node).jsonObject.getValue(group).jsonArray.map { it.jsonPrimitive.content }

    /** The §17.1 derivation order (amendment #192); the generator's Python twin. */
    private fun expected(node: String?, member: String): Pair<String, String?> {
        val scoped = node?.let { "$it.$member" }
        var tag = scoped?.let(fieldTypes::get)
        if (tag == null || tag == "enum") {
            val domain = when {
                scoped != null && scoped in enums -> scoped
                member in enums -> member
                else -> null
            }
            if (domain != null) return "enum" to domain
            check(tag != "enum") { "$scoped is enum without a domain" }
        }
        if (tag == null) tag = fieldTypes[member]?.takeIf { it != "varies-per-node" }
        if (tag == null && (member in hookKeys || member.startsWith("on_"))) tag = "action-descriptor"
        if (tag == null && member == "children") tag = "node-array"
        return (tag ?: error("${scoped ?: member} is untyped")) to null
    }

    private fun assertMemberMatches(node: String?, member: NodeMember<*>) {
        val (tag, domain) = expected(node, member.key)
        val where = "${node ?: "*"}.${member.key}"
        assertEquals(where, tag, member.tag)
        if (member is EnumMember<*>) {
            assertEquals(where, domain, member.domain.contractKey)
            assertEquals(where, enums.getValue(domain!!), member.domain.values.map { it.wire })
        }
    }

    @Test
    fun everyNodeTypeHasAMemberSetWithTheContractRows() {
        assertEquals(nodeSchema.keys, CORE_NODE_MEMBERS.keys)
        for ((node, set) in CORE_NODE_MEMBERS) {
            assertEquals(node, set.nodeType)
            assertEquals("$node required", row(node, "required"), set.required.map { it.key })
            assertEquals("$node optional", row(node, "optional"), set.optional.map { it.key })
            set.all.forEach { assertMemberMatches(node, it) }
        }
    }

    @Test
    fun universalAttributesAreProjectedUntyped() {
        assertEquals(universal, UniversalMembers.optional.map { it.key })
        assertTrue(UniversalMembers.required.isEmpty())
        UniversalMembers.all.forEach { assertMemberMatches(null, it) }
    }

    @Test
    fun enumDomainsAreExactlyThoseNodeMembersResolveTo() {
        val used = buildSet {
            for (node in nodeSchema.keys) {
                (row(node, "required") + row(node, "optional")).forEach { member ->
                    expected(node, member).second?.let(::add)
                }
            }
            universal.forEach { member -> expected(null, member).second?.let(::add) }
        }
        assertEquals(used, CORE_NODE_ENUMS.keys)
        for ((key, domain) in CORE_NODE_ENUMS) {
            assertEquals(key, domain.contractKey)
            assertEquals(key, enums.getValue(key), domain.values.map { it.wire })
        }
    }

    @Test
    fun contractProjectedDefaultsAreCarried() {
        val variantDefault = contract.getValue("text_input_schema").jsonObject
            .getValue("variant_default").jsonPrimitive.content
        assertEquals(variantDefault, TextInputMembers.Variant.default?.wire)
        val checkbox = contract.getValue("semantics_schema").jsonObject
            .getValue("default_node_semantics").jsonObject.getValue("checkbox").jsonObject
        assertEquals(
            checkbox.getValue("checked_default").jsonPrimitive.content.toBooleanStrict(),
            CheckboxMembers.Checked.default,
        )
    }
}
