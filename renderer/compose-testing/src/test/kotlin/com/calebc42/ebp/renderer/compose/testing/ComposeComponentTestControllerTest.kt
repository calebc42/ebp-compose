// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.compose.testing

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.calebc42.ebp.renderer.compose.CoreComponents
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComposeComponentTestControllerTest {
    @get:Rule
    val compose = createAndroidComposeRule(ComponentActivity::class.java)

    private fun node(json: String): JsonObject = Json.parseToJsonElement(json) as JsonObject

    @Test
    fun recordsPublishedStateAndDispatchedActionsInOrder() {
        val controller = ComposeComponentTestController.forRegistry(compose, CoreComponents)
        controller.start(
            node(
                """{"t":"column","children":[
                    {"t":"text_input","id":"f","value":"a"},
                    {"t":"button","label":"Go","on_tap":{"action":"demo.go"}}]}""",
            ),
        )

        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("b")
        compose.onNodeWithText("Go").performClick()

        compose.runOnIdle {
            // Every edit publishes state; the tap follows every state event.
            assertTrue(controller.events.count { it == "state:f" } >= 2)
            assertEquals("action:demo.go", controller.events.last())
            assertEquals(JsonPrimitive("b"), controller.states.last().value)
            assertEquals("demo.go", controller.dispatched.single().name)
        }
        controller.clearRecords()
        assertTrue(controller.events.isEmpty())
    }

    @Test
    fun updateReplacesTheMountedNodeInTheSameComposition() {
        val controller = ComposeComponentTestController.forRegistry(compose, CoreComponents)
        controller.start(node("""{"t":"text","text":"first"}"""))
        compose.onNodeWithText("first").assertIsDisplayed()

        controller.update(node("""{"t":"text","text":"second"}"""))

        compose.onNodeWithText("second").assertIsDisplayed()
    }

    @Test
    fun seededStoreIsVisibleToTheContext() {
        val controller = ComposeComponentTestController.forRegistry(compose, CoreComponents)
        controller.seedStore("f", JsonPrimitive("draft"), epoch = 3L)

        assertEquals(JsonPrimitive("draft"), controller.context.storeValue("f"))
        assertEquals(3L, controller.context.epochOf("f"))
        assertEquals(0L, controller.context.epochOf("absent"))
    }
}
