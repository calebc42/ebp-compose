// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose.testing

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * The EBP conformance corpus (the `.golden` files under `ebp/goldens`),
 * located through the `ebp.dir` system property the including build sets.
 * Lines are `NN {json}`; indexes are strings because the corpus is numbered,
 * not contiguous.
 */
object EbpGoldens {
    /** One corpus line. */
    data class GoldenLine(val index: String, val raw: String, val json: JsonElement) {
        /** A bare ActionDescriptor line (no `t`), which renders only inside a node. */
        val isDescriptor: Boolean
            get() = json is JsonObject && !json.containsKey("t") &&
                (json.containsKey("action") || json.containsKey("builtin"))

        /** The line as a node; throws for a descriptor or array line. */
        val node: JsonObject
            get() = json as? JsonObject ?: error("golden $index is not an object")
    }

    /** The `goldens` directory of the sibling `ebp` checkout. */
    val directory: File
        get() = File(
            System.getProperty("ebp.dir") ?: error("ebp.dir system property not set"),
            "goldens",
        )

    /** Every non-blank line of [name], in file order. */
    fun lines(name: String): List<GoldenLine> =
        File(directory, name).readLines().filter { it.isNotBlank() }.map { line ->
            val raw = line.substringAfter(' ')
            GoldenLine(line.substringBefore(' '), raw, Json.parseToJsonElement(raw))
        }

    /** `widgets.golden`: every node type plus bare ActionDescriptors. */
    fun widgets(): List<GoldenLine> = lines("widgets.golden")

    /** `hypertext.golden`: document node arrays. */
    fun hypertext(): List<GoldenLine> = lines("hypertext.golden")

    /** The `widgets.golden` line numbered [index]. */
    fun widget(index: String): GoldenLine =
        widgets().firstOrNull { it.index == index } ?: error("no widgets golden $index")

    /**
     * A document that exercises a bare ActionDescriptor: a column holding a
     * `text_input` for every captured field (capture resolution is
     * document-wide) and a button whose `on_tap` is [action]. The same wrap
     * the validator's replay uses.
     */
    fun wrapDescriptor(action: JsonObject): JsonObject = buildJsonObject {
        put("t", "column")
        putJsonArray("children") {
            (action["capture_fields"] as? JsonArray)?.forEach { field ->
                addJsonObject {
                    put("t", "text_input")
                    put("id", field.jsonPrimitive.content)
                }
            }
            addJsonObject {
                put("t", "button")
                put("label", "x")
                put("on_tap", action)
            }
        }
    }
}
