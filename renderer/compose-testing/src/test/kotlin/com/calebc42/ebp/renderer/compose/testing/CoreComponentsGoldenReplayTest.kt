// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose.testing

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.calebc42.ebp.renderer.compose.CoreComponents
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * BUILDING-COMPANION.md: "every widgets.golden line parses and renders (or
 * degrades) without crashing". The core tier owns eight types; every other
 * line must degrade to its children or to nothing, never throw.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoreComponentsGoldenReplayTest {
    @get:Rule
    val compose = createAndroidComposeRule(ComponentActivity::class.java)

    private fun node(json: String): JsonObject = Json.parseToJsonElement(json) as JsonObject

    @Test
    fun everyWidgetsGoldenLineRendersOrDegrades() {
        val lines = EbpGoldens.widgets()
        assertTrue("the corpus must still carry every node type", lines.size >= 72)
        val controller = ComposeComponentTestController.forRegistry(compose, CoreComponents)
        controller.start(node("""{"t":"text","text":"start"}"""))
        for (line in lines) {
            val document = if (line.isDescriptor) EbpGoldens.wrapDescriptor(line.node) else line.node
            try {
                controller.update(document)
            } catch (failure: Throwable) {
                throw AssertionError("widgets golden ${line.index} failed to render: ${line.raw}", failure)
            }
        }
    }

    @Test
    fun aDescriptorLineReachesTheHostExactlyOnce() {
        val line = EbpGoldens.widgets().first { it.isDescriptor }
        val controller = ComposeComponentTestController.forRegistry(compose, CoreComponents)
        controller.start(EbpGoldens.wrapDescriptor(line.node))

        compose.onNodeWithText("x").performClick()

        compose.runOnIdle {
            assertEquals(1, controller.dispatched.size)
            assertEquals(line.node, controller.dispatched.single().descriptor)
        }
    }

    @Test
    fun aTypeOutsideTheCoreTierDegradesToItsChildren() {
        val controller = ComposeComponentTestController.forRegistry(compose, CoreComponents)
        controller.start(node("""{"t":"card","children":[{"t":"text","text":"inner"}]}"""))

        compose.onNodeWithText("inner").assertIsDisplayed()
    }
}
