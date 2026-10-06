// SPDX-License-Identifier: GPL-3.0-or-later
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.calebc42.ebp.renderer.compose

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.inputText
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calebc42.ebp.wire.textInputScalarToUtf16
import com.calebc42.ebp.renderer.model.ActionHandoff
import com.calebc42.ebp.renderer.model.vocabulary.NodeProperties
import com.calebc42.ebp.renderer.model.vocabulary.generated.TextInputMembers
import com.calebc42.ebp.renderer.model.vocabulary.generated.TextInputVariant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Accepted, toolkit-neutral presentation members of one EBP `text_input`. */
data class TextInputPresentation(
    val id: String,
    val label: String?,
    val hint: String?,
    val supportingText: String?,
    val prefix: String?,
    val suffix: String?,
    val leadingIcon: String?,
    val trailingIcon: String?,
    val password: Boolean,
    val singleLine: Boolean,
    val monospace: Boolean,
    val syntax: String?,
    val autofocus: Boolean,
    val enabled: Boolean,
    val isError: Boolean,
    val variant: String,
    val mask: String?,
    val contentPadding: Dp?,
    val hideKeyboardOnSubmit: Boolean,
    val keyboardOptions: KeyboardOptions,
    val lineLimits: TextFieldLineLimits,
)

/**
 * Shared behavioral binding consumed by alternate `text_input` presentations.
 *
 * Renderers may decorate [presentation], [controller], and [focusRequester];
 * they must not reparse JSON or bypass the controller for editing actions.
 */
@Stable
class TextInputBinding internal constructor(
    val presentation: TextInputPresentation,
    val controller: TextInputController,
    val focusRequester: FocusRequester,
) {
    /** Enabled state including the contract-required pending-secret lock. */
    val enabled: Boolean
        get() = presentation.enabled &&
            !(presentation.password && controller.passwordSubmissionPending)

    /** Submit the current logical value through the ordinary action host. */
    fun submit(): ActionHandoff = controller.submit()

    /**
     * Attach the shared focus owner and password-safe accessibility value.
     *
     * Compose 1.12's secure fields visually obfuscate `EditableText` but also
     * publish the raw value as `InputText`; Android's accessibility bridge
     * selects that raw property for `AccessibilityNodeInfo.text`. Password
     * fields override both properties with scalar-count bullets here while
     * leaving the field's native editing actions and autofill type intact.
     */
    fun fieldModifier(modifier: Modifier): Modifier {
        val focused = modifier.focusRequester(focusRequester)
        if (!presentation.password) return focused
        return focused.semantics {
            val text = controller.state.text
            val obfuscated = AnnotatedString(
                SECURE_ACCESSIBILITY_BULLET.toString().repeat(text.scalarCount()),
            )
            inputText = obfuscated
            editableText = obfuscated
            textSelectionRange = controller.state.selection.toScalarRange(text)
            password()
        }
    }
}

private const val SECURE_ACCESSIBILITY_BULLET = '\u2022'

private fun CharSequence.scalarCount(endExclusive: Int = length): Int {
    var count = 0
    var index = 0
    val end = endExclusive.coerceIn(0, length)
    while (index < end) {
        val first = this[index]
        index += if (
            Character.isHighSurrogate(first) &&
            index + 1 < end &&
            Character.isLowSurrogate(this[index + 1])
        ) {
            2
        } else {
            1
        }
        count += 1
    }
    return count
}

private fun TextRange.toScalarRange(text: CharSequence): TextRange = TextRange(
    text.scalarCount(start),
    text.scalarCount(end),
)

/** Initial state selected from authored content and an accepted retained value. */
internal data class TextInputSeed(
    val text: String,
    val selection: TextRange,
)

/**
 * Bind one admitted canonical node to shared state, reconciliation, and action
 * behavior. The containing render identity owns this composable invocation.
 */
@Composable
fun rememberTextInputBinding(
    node: JsonObject,
    context: ComposeNodeRenderContext,
): TextInputBinding {
    val properties = NodeProperties(node, TextInputMembers)
    val id = requireNotNull(properties[TextInputMembers.Id]) { "text_input without an id" }
    val password = properties.getOr(TextInputMembers.Password, false)
    val singleLine = properties.getOr(TextInputMembers.SingleLine, false)
    val stored = (context.storeValue(id) as? JsonPrimitive)
        ?.takeIf { it.isString }
        ?.content
    val seed = textInputSeed(node, stored)
    val onSubmit = properties[TextInputMembers.OnSubmit]
    val controller = rememberTextInputController(
        presentationEpoch = context.epochOf(id),
        initialText = seed.text,
        initialSelection = seed.selection,
        config = TextInputControllerConfig(
            id = id,
            password = password,
            singleLine = singleLine,
            // The controller resolves the filter against the contract's
            // character sets itself; it receives the authored spelling.
            filter = TextInputMembers.Filter.readRaw(node),
            maxLengthScalars = properties[TextInputMembers.MaxLength]?.toLong(),
            clearOnSubmit = properties.getOr(TextInputMembers.ClearOnSubmit, false),
            onChange = properties[TextInputMembers.OnChange],
            onSubmit = onSubmit,
            publishPasswordLocally = context.inDialog,
        ),
        maxFieldBytes = context.maxFieldBytes,
        publishState = { context.state(id, JsonPrimitive(it), password) },
        actionDispatcher = EditingActionDispatcher {
                descriptor, value, secret, sourceId, onOutcome ->
            context.dispatchAction(
                descriptor = descriptor,
                value = value,
                secret = secret,
                sourceId = sourceId,
                onOutcome = onOutcome,
            )
        },
    )
    DisposableEffect(
        controller,
        context.volatileSecretRegistryKey,
        id,
        password,
    ) {
        val unregister = if (password) {
            context.registerVolatileSecret(id, controller::eraseVolatileState)
        } else {
            {}
        }
        onDispose(unregister)
    }
    val presentation = textInputPresentationOf(node)
    val focusRequester = rememberPresentationFocusRequester(
        identity = context.path,
        autofocus = presentation.autofocus,
    )
    return remember(presentation, controller, focusRequester) {
        TextInputBinding(presentation, controller, focusRequester)
    }
}

/**
 * Pure contract projection shared by unit tests and both renderers.
 *
 * Reads go through the generated [TextInputMembers]: strict and total, so a
 * string-spelled boolean or number is not a boolean or number (the validator
 * already rejects it). The `variant` default and unknown-value fallback come
 * from the contract (`text_input_schema.variant_default`) through the member's
 * own default; the other defaults are SPEC §17.4 prose applied here.
 */
internal fun textInputPresentationOf(node: JsonObject): TextInputPresentation {
    val properties = NodeProperties(node, TextInputMembers)
    val password = properties.getOr(TextInputMembers.Password, false)
    val singleLine = properties.getOr(TextInputMembers.SingleLine, false)
    val onSubmit = properties[TextInputMembers.OnSubmit]
    fun nonEmpty(value: String?): String? = value?.takeIf { it.isNotEmpty() }
    return TextInputPresentation(
        id = requireNotNull(properties[TextInputMembers.Id]) { "text_input without an id" },
        label = nonEmpty(properties[TextInputMembers.Label]),
        hint = nonEmpty(properties[TextInputMembers.Hint]),
        supportingText = nonEmpty(properties[TextInputMembers.SupportingText]),
        prefix = nonEmpty(properties[TextInputMembers.Prefix]),
        suffix = nonEmpty(properties[TextInputMembers.Suffix]),
        leadingIcon = nonEmpty(properties[TextInputMembers.LeadingIcon]),
        trailingIcon = nonEmpty(properties[TextInputMembers.TrailingIcon]),
        password = password,
        singleLine = singleLine,
        monospace = properties.getOr(TextInputMembers.Monospace, false),
        syntax = nonEmpty(properties[TextInputMembers.Syntax]),
        autofocus = properties.getOr(TextInputMembers.Autofocus, false),
        enabled = properties.getOr(TextInputMembers.Enabled, true),
        isError = properties.getOr(TextInputMembers.IsError, false),
        variant = properties.getOr(TextInputMembers.Variant, TextInputVariant.Outlined).wire,
        mask = nonEmpty(properties[TextInputMembers.Mask]),
        contentPadding = properties[TextInputMembers.ContentPadding]?.dp,
        hideKeyboardOnSubmit = properties.getOr(TextInputMembers.HideKeyboardOnSubmit, false),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType(properties[TextInputMembers.Keyboard]?.wire, password),
            imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Default,
        ),
        lineLimits = properties.lineLimits(singleLine),
    )
}

/** Pure state seed selection implementing retained-draft and scalar rules. */
internal fun textInputSeed(node: JsonObject, stored: String?): TextInputSeed {
    val properties = NodeProperties(node, TextInputMembers)
    val password = properties.getOr(TextInputMembers.Password, false)
    val authored = properties[TextInputMembers.Value] ?: ""
    val initialText = if (password) "" else stored ?: authored
    val retainedDraftWins = !password && stored != null && stored != authored
    val authoredSelection = properties[TextInputMembers.Selection]
    val selection = if (retainedDraftWins || authoredSelection == null) {
        TextRange(initialText.length)
    } else {
        TextRange(
            textInputScalarToUtf16(initialText, authoredSelection.first.toInt()),
            textInputScalarToUtf16(initialText, authoredSelection.second.toInt()),
        )
    }
    return TextInputSeed(initialText, selection)
}

/** Request focus at most once for one accepted presentation identity. */
@Composable
fun rememberPresentationFocusRequester(
    identity: String,
    autofocus: Boolean,
): FocusRequester {
    val requester = remember(identity) { FocusRequester() }
    var requested by rememberSaveable(identity) { mutableStateOf(false) }
    LaunchedEffect(requester, autofocus) {
        if (autofocus && !requested) {
            requested = true
            requester.requestFocus()
        }
    }
    return requester
}

/** Standard IME callback preserving the renderer-local hide-on-handoff rule. */
fun TextInputBinding.keyboardAction(
    hideKeyboard: () -> Unit,
): KeyboardActionHandler = KeyboardActionHandler {
    if (submit() == ActionHandoff.HandedOff && presentation.hideKeyboardOnSubmit) {
        hideKeyboard()
    }
}

private fun NodeProperties.lineLimits(singleLine: Boolean): TextFieldLineLimits {
    if (singleLine) return TextFieldLineLimits.SingleLine
    val minimum = TextInputMembers.MinLines.readClamped(node) ?: 1
    val maximum = TextInputMembers.MaxLines.readClamped(node) ?: minimum
    return TextFieldLineLimits.MultiLine(
        minHeightInLines = minimum,
        maxHeightInLines = maximum,
    )
}

private fun keyboardType(name: String?, password: Boolean): KeyboardType {
    if (password) return KeyboardType.Password
    return when (name) {
        "number" -> KeyboardType.Number
        "decimal" -> KeyboardType.Decimal
        "email" -> KeyboardType.Email
        "phone" -> KeyboardType.Phone
        "uri" -> KeyboardType.Uri
        else -> KeyboardType.Text
    }
}

