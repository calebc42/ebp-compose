// SPDX-License-Identifier: GPL-3.0-or-later
// Typed member descriptors over accepted EBP JSON nodes. The per-node member
// sets and enum domains in `generated/` are produced from ebp/contract.json by
// tools/gen-node-members.py; this file is the hand-written algebra they use.
package com.calebc42.ebp.renderer.model.vocabulary

import com.calebc42.ebp.renderer.model.arrOrNull
import com.calebc42.ebp.renderer.model.boolOrNull
import com.calebc42.ebp.renderer.model.numOrNull
import com.calebc42.ebp.renderer.model.objOrNull
import com.calebc42.ebp.renderer.model.strOrNull
import com.calebc42.ebp.renderer.model.stringOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.floor

/** An enum constant that knows its exact contract spelling. */
interface WireEnum {
    /** The value as it appears on the wire (`contract.json` `enums`). */
    val wire: String
}

/**
 * The closed value domain of one enum-typed member, generated from the
 * contract's `enums` registry (or a format-2 extension manifest).
 *
 * [fromWire] returns null for a spelling outside the domain: SPEC §12 rule 6
 * forbids guessing, so the renderer applies its documented fallback itself.
 */
interface EnumDomain<E> where E : Enum<E>, E : WireEnum {
    /** The `enums` key this domain was generated from, e.g. `text.style`. */
    val contractKey: String

    /** Every constant, in contract order. */
    val values: List<E>

    /** The constant spelled [value], or null when the value is outside the domain. */
    fun fromWire(value: String): E? = values.firstOrNull { it.wire == value }
}

/**
 * One typed member of an EBP node: the contract's key, requiredness and value
 * tag, plus a total, strict reader over the accepted JSON node.
 *
 * Readers never throw and never coerce. An absent member, a member of another
 * JSON kind, a number outside the tag's range, or an enum value outside its
 * domain all read as null — SPEC §17.1 forbids coercion the contract omits.
 * Defaults are the renderer's business and are applied at the call site
 * (`NodeProperties.getOr`); [default] is non-null only where the contract
 * itself projects one, such as `text_input_schema.variant_default`.
 */
sealed interface NodeMember<out T : Any> {
    /** The member name on the wire. */
    val key: String

    /** Whether the contract lists the member under `required`. */
    val required: Boolean

    /** The contract field-type tag (or manifest type) this member was derived from. */
    val tag: String

    /** A contract-projected default, or null when the contract states none. */
    val default: T? get() = null

    /** The member's value in [node], or null when absent or not of this type. */
    fun read(node: JsonObject): T?
}

/** Shared identity and rendering for the concrete member classes. */
abstract class AbstractNodeMember<T : Any>(
    override val key: String,
    override val required: Boolean,
    override val tag: String,
    override val default: T? = null,
) : NodeMember<T> {
    final override fun toString(): String =
        "${javaClass.simpleName}($key${if (required) ", required" else ""})"
}

/** A plain JSON string, optionally in a wire format such as `yyyy-mm-dd`. */
class StringMember(
    key: String,
    required: Boolean,
    tag: String = "string",
    /** The contract format tag when the string is date-like (`yyyy-mm`, `yyyy-mm-dd`, `hh-mm`). */
    val format: String? = null,
    default: String? = null,
) : AbstractNodeMember<String>(key, required, tag, default) {
    override fun read(node: JsonObject): String? = node.stringOrNull(key)
}

/** A SPEC §4.4 identifier, read as its raw string. */
class IdentifierMember(
    key: String,
    required: Boolean,
    tag: String = "identifier",
) : AbstractNodeMember<String>(key, required, tag) {
    override fun read(node: JsonObject): String? = node.stringOrNull(key)
}

/** A SPEC §16.6 Color (theme role or hex), read as its raw string; parsing stays renderer-side. */
class ColorMember(
    key: String,
    required: Boolean,
    tag: String = "color",
) : AbstractNodeMember<String>(key, required, tag) {
    override fun read(node: JsonObject): String? = node.stringOrNull(key)
}

/** A JSON boolean; a string spelling is not a boolean. */
class BooleanMember(
    key: String,
    required: Boolean,
    tag: String = "boolean",
    default: Boolean? = null,
) : AbstractNodeMember<Boolean>(key, required, tag, default) {
    override fun read(node: JsonObject): Boolean? = node.boolOrNull(key)
}

/** The three states of a `boolean-or-auto` member. */
enum class BooleanOrAuto { True, False, Auto }

/** A JSON boolean or the string `"auto"` (e.g. `button.expanded`). */
class BooleanOrAutoMember(
    key: String,
    required: Boolean,
    tag: String = "boolean-or-auto",
) : AbstractNodeMember<BooleanOrAuto>(key, required, tag) {
    override fun read(node: JsonObject): BooleanOrAuto? = when {
        node.boolOrNull(key) == true -> BooleanOrAuto.True
        node.boolOrNull(key) == false -> BooleanOrAuto.False
        node.stringOrNull(key) == "auto" -> BooleanOrAuto.Auto
        else -> null
    }
}

/** A finite JSON number. */
class NumberMember(
    key: String,
    required: Boolean,
    tag: String = "number",
    default: Double? = null,
) : AbstractNodeMember<Double>(key, required, tag, default) {
    override fun read(node: JsonObject): Double? =
        node[key]?.numOrNull()?.takeIf { it.isFinite() }
}

/** A finite, non-negative density-independent length (SPEC §16.5 `dp`). */
class DpMember(
    key: String,
    required: Boolean,
    tag: String = "dp",
) : AbstractNodeMember<Float>(key, required, tag) {
    override fun read(node: JsonObject): Float? =
        node[key]?.numOrNull()?.takeIf { it.isFinite() && it >= 0.0 }?.toFloat()
            ?.takeIf { it.isFinite() } // a binary64 value beyond Float range is not a dp
}

/**
 * An integer validated BY VALUE (`2.0` is integral, as the validator accepts)
 * and bounded by the tag: `positive-integer`, `non-negative-integer`,
 * `integer-1-12`, `integer-1-31`, or a manifest `integer` range.
 */
class IntMember(
    key: String,
    required: Boolean,
    tag: String,
    /** The smallest accepted value, inclusive. */
    val minimum: Long,
    /** The largest accepted value, inclusive. */
    val maximum: Long,
    default: Int? = null,
) : AbstractNodeMember<Int>(key, required, tag, default) {
    override fun read(node: JsonObject): Int? {
        val value = node[key]?.numOrNull() ?: return null
        if (value != floor(value) || value < minimum || value > maximum) return null
        if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) return null
        return value.toInt()
    }

    /**
     * Like [read], but a contract-legal value beyond the platform `Int` range
     * saturates instead of reading as null — for line counts and similar
     * platform parameters where "as large as the platform allows" is the
     * intended meaning of an enormous authored number.
     */
    fun readClamped(node: JsonObject): Int? {
        val value = node[key]?.numOrNull() ?: return null
        if (value != floor(value) || value < minimum || value > maximum) return null
        return value.coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()).toInt()
    }
}

/** A member whose string value must belong to a generated [EnumDomain]. */
class EnumMember<E>(
    key: String,
    required: Boolean,
    /** The closed domain the value is resolved against. */
    val domain: EnumDomain<E>,
    default: E? = null,
    tag: String = "enum",
) : AbstractNodeMember<E>(key, required, tag, default) where E : Enum<E>, E : WireEnum {
    override fun read(node: JsonObject): E? = node.stringOrNull(key)?.let(domain::fromWire)

    /** The authored spelling, whether or not it is in the domain. */
    fun readRaw(node: JsonObject): String? = node.stringOrNull(key)
}

/** A SPEC §14 ActionDescriptor, handed to the host untouched. */
class ActionMember(
    key: String,
    required: Boolean,
    tag: String = "action-descriptor",
) : AbstractNodeMember<JsonObject>(key, required, tag) {
    override fun read(node: JsonObject): JsonObject? = node.objOrNull(key)
}

/** A single child Node slot (`node`). */
class ChildNodeMember(
    key: String,
    required: Boolean,
    tag: String = "node",
) : AbstractNodeMember<JsonObject>(key, required, tag) {
    override fun read(node: JsonObject): JsonObject? = node.objOrNull(key)
}

/** An array of child Nodes (`node-array`); null when any element is not an object. */
class NodeArrayMember(
    key: String,
    required: Boolean,
    tag: String = "node-array",
) : AbstractNodeMember<List<JsonObject>>(key, required, tag) {
    override fun read(node: JsonObject): List<JsonObject>? {
        val array = node.arrOrNull(key) ?: return null
        return array.map { it as? JsonObject ?: return null }
    }
}

/** A structured object member (`*-object` tags) kept as JSON for its own reader. */
class ObjectMember(
    key: String,
    required: Boolean,
    tag: String,
) : AbstractNodeMember<JsonObject>(key, required, tag) {
    override fun read(node: JsonObject): JsonObject? = node.objOrNull(key)
}

/** A structured array member (`*-array` tags) kept as JSON for its own reader. */
class ArrayMember(
    key: String,
    required: Boolean,
    tag: String,
) : AbstractNodeMember<JsonArray>(key, required, tag) {
    override fun read(node: JsonObject): JsonArray? = node.arrOrNull(key)
}

/** A member of more than one JSON kind (e.g. `number-or-corner-object`), kept as JSON. */
class AnyMember(
    key: String,
    required: Boolean,
    tag: String,
) : AbstractNodeMember<JsonElement>(key, required, tag) {
    override fun read(node: JsonObject): JsonElement? = node[key]?.takeUnless { it is JsonNull }
}

/** A scalar of any primitive kind (e.g. an `option-value`), kept as a primitive. */
class ScalarMember(
    key: String,
    required: Boolean,
    tag: String,
) : AbstractNodeMember<JsonPrimitive>(key, required, tag) {
    override fun read(node: JsonObject): JsonPrimitive? =
        (node[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }
}

/** The value of a `string-or-number` member such as `font_weight`. */
sealed interface StringOrNumber {
    /** The string alternative. */
    data class Text(val value: String) : StringOrNumber

    /** The finite numeric alternative. */
    data class Numeric(val value: Double) : StringOrNumber
}

/** A member that is either a string or a finite number. */
class StringOrNumberMember(
    key: String,
    required: Boolean,
    tag: String,
) : AbstractNodeMember<StringOrNumber>(key, required, tag) {
    override fun read(node: JsonObject): StringOrNumber? {
        val element = node[key] ?: return null
        element.strOrNull()?.let { return StringOrNumber.Text(it) }
        return element.numOrNull()?.takeIf { it.isFinite() }?.let(StringOrNumber::Numeric)
    }
}

/** A `two-number-array` such as `text_input.selection`. */
class TwoNumberArrayMember(
    key: String,
    required: Boolean,
    tag: String = "two-number-array",
) : AbstractNodeMember<Pair<Double, Double>>(key, required, tag) {
    override fun read(node: JsonObject): Pair<Double, Double>? {
        val array = node.arrOrNull(key)?.takeIf { it.size == 2 } ?: return null
        val first = array[0].numOrNull()?.takeIf { it.isFinite() } ?: return null
        val second = array[1].numOrNull()?.takeIf { it.isFinite() } ?: return null
        return first to second
    }
}

/** A `number-array`; null when any element is not a finite number. */
class NumberArrayMember(
    key: String,
    required: Boolean,
    tag: String = "number-array",
) : AbstractNodeMember<List<Double>>(key, required, tag) {
    override fun read(node: JsonObject): List<Double>? {
        val array = node.arrOrNull(key) ?: return null
        return array.map { it.numOrNull()?.takeIf { n -> n.isFinite() } ?: return null }
    }
}

/**
 * The typed members of one node type, in the contract's required/optional
 * partition. Generated objects (`TextMembers`, …) subclass this; a renderer
 * component declares the set it reads through.
 */
abstract class NodeMemberSet(
    /** The node type (`t`) these members belong to; `*` for the universal attributes. */
    val nodeType: String,
) {
    /** Members the contract requires on every node of this type. */
    abstract val required: List<NodeMember<*>>

    /** Members the contract allows on this type. */
    abstract val optional: List<NodeMember<*>>

    /** Every member, required first, in contract order. */
    val all: List<NodeMember<*>> by lazy { required + optional }

    private val byKey: Map<String, NodeMember<*>> by lazy {
        all.associateBy { it.key }.also { indexed ->
            require(indexed.size == all.size) { "$nodeType repeats a member key" }
            require(required.all { it.required } && optional.none { it.required }) {
                "$nodeType member requiredness disagrees with its partition"
            }
        }
    }

    /** The member named [key], or null when this type has none. */
    fun member(key: String): NodeMember<*>? = byKey[key]

    /** Every member key. */
    val keys: Set<String> get() = byKey.keys

    final override fun toString(): String = "NodeMemberSet($nodeType)"

    companion object {
        /**
         * A set whose members are all untyped [AnyMember]s — the projection of
         * a format-1 extension manifest, which carries member names only.
         */
        fun untyped(nodeType: String, required: Set<String>, optional: Set<String>): NodeMemberSet =
            object : NodeMemberSet(nodeType) {
                override val required: List<NodeMember<*>> =
                    required.sorted().map { AnyMember(it, required = true, tag = "untyped") }
                override val optional: List<NodeMember<*>> =
                    optional.sorted().map { AnyMember(it, required = false, tag = "untyped") }
            }
    }
}

/**
 * Typed access to one accepted node through its member set.
 *
 * This is a view over the same JSON the validator admitted — no copy and no
 * parallel model. Every read is total; [getOr] supplies the renderer's
 * documented default where the contract states none.
 */
class NodeProperties(
    /** The accepted node. */
    val node: JsonObject,
    /** The member set the reader declares; universal members may be read on any node. */
    val members: NodeMemberSet,
) {
    /** [member]'s value, or null when absent or not of its type. */
    operator fun <T : Any> get(member: NodeMember<T>): T? = member.read(node)

    /** [member]'s value, the contract default if it has one, else [default]. */
    fun <T : Any> getOr(member: NodeMember<T>, default: T): T =
        member.read(node) ?: member.default ?: default

    /** Whether the node carries [member] at all, whatever its JSON kind. */
    fun has(member: NodeMember<*>): Boolean = node.containsKey(member.key)

    /** The raw JSON under [member]'s key, for readers of structured members. */
    fun raw(member: NodeMember<*>): JsonElement? = node[member.key]
}
