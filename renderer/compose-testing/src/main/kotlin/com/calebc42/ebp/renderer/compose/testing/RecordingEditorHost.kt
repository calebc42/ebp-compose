// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose.testing

import com.calebc42.ebp.renderer.model.CandidateDocument
import com.calebc42.ebp.renderer.model.CompletionOffer
import com.calebc42.ebp.renderer.model.EditorAnnotationState
import com.calebc42.ebp.renderer.model.EditorConnectionPhase
import com.calebc42.ebp.renderer.model.EditorEditOutcome
import com.calebc42.ebp.renderer.model.EditorMirror
import com.calebc42.ebp.renderer.model.RendererEditorHost
import com.calebc42.ebp.wire.CompletionNarrowing
import com.calebc42.ebp.wire.CompletionOfferView
import com.calebc42.ebp.wire.ScalarPos
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An editor host that records every call and lets a test drive the flows a
 * synchronized editor observes. Every flow is a [MutableStateFlow] the test
 * may set; every outbound call lands in a list the test may assert on.
 */
class RecordingEditorHost(
    override val maxEditorBytes: Int = 262_144,
) : RendererEditorHost {
    /** One `publishEditorEdit` call. */
    data class Edit(
        val document: String,
        val editorId: String,
        val start: ScalarPos,
        val deletedScalars: Int,
        val inserted: String,
        val base: String,
    )

    /** One `publishEditorCaret` call. */
    data class Caret(
        val document: String,
        val editorId: String,
        val cursorUtf16: Int,
        val selectionStartUtf16: Int,
        val selectionEndUtf16: Int,
    )

    /** One `dispatchEditorCommand` call. */
    data class Command(
        val surface: String,
        val document: String,
        val editorId: String,
        val command: String,
        val cursorUtf16: Int,
        val selectionStartUtf16: Int,
        val selectionEndUtf16: Int,
    )

    /** One `selectEditorCompletion` call. */
    data class CompletionSelection(
        val document: String,
        val editorId: String,
        val label: String,
        val insert: String,
    )

    /** One `requestCandidateDocument` call. */
    data class CandidateRequest(
        val document: String,
        val editorId: String,
        val index: Int,
        val epoch: Long,
    )

    override val editorConnectionPhase = MutableStateFlow(EditorConnectionPhase.READY)
    override val editorMirrors = MutableStateFlow<Map<Pair<String, String>, EditorMirror>>(emptyMap())
    override val editorAnnotations =
        MutableStateFlow<Map<Pair<String, String>, EditorAnnotationState>>(emptyMap())
    override val completionOffers =
        MutableStateFlow<Map<Pair<String, String>, CompletionOffer>>(emptyMap())
    override val completionOfferViews =
        MutableStateFlow<Map<Pair<String, String>, CompletionOfferView>>(emptyMap())
    override val candidateDocuments =
        MutableStateFlow<Map<Pair<String, String>, CandidateDocument>>(emptyMap())
    override var completionNarrowing: CompletionNarrowing = CompletionNarrowing.STRICT

    /** `requestEditorCompletion` calls as (document, editorId). */
    val completionRequests = mutableListOf<Pair<String, String>>()
    val completionSelections = mutableListOf<CompletionSelection>()
    val candidateRequests = mutableListOf<CandidateRequest>()
    val edits = mutableListOf<Edit>()
    /** `publishEditorComposition` calls as (document, editorId, active). */
    val compositions = mutableListOf<Triple<String, String, Boolean>>()
    val carets = mutableListOf<Caret>()
    val commands = mutableListOf<Command>()

    /** Delivered to every `publishEditorEdit` callback when non-null. */
    var nextEditOutcome: EditorEditOutcome? = null

    override fun requestEditorCompletion(document: String, editorId: String) {
        completionRequests += document to editorId
    }

    override fun selectEditorCompletion(document: String, editorId: String, label: String, insert: String) {
        completionSelections += CompletionSelection(document, editorId, label, insert)
    }

    override fun requestCandidateDocument(document: String, editorId: String, index: Int, epoch: Long) {
        candidateRequests += CandidateRequest(document, editorId, index, epoch)
    }

    override fun publishEditorEdit(
        document: String,
        editorId: String,
        start: ScalarPos,
        deletedScalars: Int,
        inserted: String,
        base: String,
        onOutcome: (EditorEditOutcome) -> Unit,
    ) {
        edits += Edit(document, editorId, start, deletedScalars, inserted, base)
        nextEditOutcome?.let(onOutcome)
    }

    override fun publishEditorComposition(document: String, editorId: String, active: Boolean) {
        compositions += Triple(document, editorId, active)
    }

    override fun publishEditorCaret(
        document: String,
        editorId: String,
        cursorUtf16: Int,
        selectionStartUtf16: Int,
        selectionEndUtf16: Int,
    ) {
        carets += Caret(document, editorId, cursorUtf16, selectionStartUtf16, selectionEndUtf16)
    }

    override fun dispatchEditorCommand(
        surface: String,
        document: String,
        editorId: String,
        command: String,
        cursorUtf16: Int,
        selectionStartUtf16: Int,
        selectionEndUtf16: Int,
    ) {
        commands += Command(surface, document, editorId, command, cursorUtf16, selectionStartUtf16, selectionEndUtf16)
    }

    /** Forget every recorded call; flows keep their current values. */
    fun clear() {
        completionRequests.clear()
        completionSelections.clear()
        candidateRequests.clear()
        edits.clear()
        compositions.clear()
        carets.clear()
        commands.clear()
    }
}
