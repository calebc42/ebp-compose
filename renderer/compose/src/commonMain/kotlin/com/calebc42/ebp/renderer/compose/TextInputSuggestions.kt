// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose

import com.calebc42.ebp.renderer.model.vocabulary.generated.TextInputMembers
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * SPEC 17.4 `suggestions` (amendment #202): completing the word being typed
 * at the Companion's own cursor. The cursor stays local; only the edited
 * value is published.
 */
object TextInputSuggestions {
    /** The node's suggestions, or empty when it has none it may offer. */
    fun of(node: JsonObject): List<String> {
        if ((node["password"] as? JsonPrimitive)?.content == "true") return emptyList()
        if (node["mask"] != null) return emptyList()
        return (node[TextInputMembers.Suggestions.key] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content }
            ?.filter { word -> word.isNotEmpty() && word.none(Char::isWhitespace) }
            .orEmpty()
    }

    /**
     * The word being typed: the run of non-whitespace ending at [cursor], as
     * `start until cursor`; null when it is empty or a range is selected.
     */
    fun wordAt(text: CharSequence, selectionStart: Int, selectionEnd: Int): IntRange? {
        if (selectionStart != selectionEnd) return null
        val cursor = selectionEnd.coerceIn(0, text.length)
        var start = cursor
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        return if (start == cursor) null else start until cursor
    }

    /** The suggestions [word] begins, case-folded, in authored order. */
    fun matching(word: CharSequence, suggestions: List<String>): List<String> =
        suggestions.filter { it.startsWith(word, ignoreCase = true) }
}
