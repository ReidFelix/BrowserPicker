package com.example.browserpicker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkRequestStateTest {

    @Test
    fun `a fresh state has no request`() {
        assertNull(LinkRequestState().current.value)
    }

    @Test
    fun `a new request carries the url`() {
        val state = LinkRequestState()
        val request = state.newRequest("https://example.com")

        assertEquals("https://example.com", request.url)
        assertEquals(request, state.current.value)
    }

    @Test
    fun `every request gets its own id`() {
        val state = LinkRequestState()

        val first = state.newRequest("https://example.com/1")
        val second = state.newRequest("https://example.com/2")

        assertTrue(first.id != second.id)
        assertEquals(second, state.current.value)
    }

    @Test
    fun `the same url twice still produces a new request`() {
        val state = LinkRequestState()

        val first = state.newRequest("https://example.com")
        val second = state.newRequest("https://example.com")

        assertTrue(first.id != second.id)
    }

    @Test
    fun `dismissing the current dialog finishes the activity`() {
        val state = LinkRequestState()
        val request = state.newRequest("https://example.com")

        assertTrue(state.shouldFinishOnDismiss(request.id))
    }

    @Test
    fun `dismissing after a hand-over does not finish the activity`() {
        // The system dismisses the dialog when the Activity moves to the
        // background — closing the Activity there would cancel the browser
        // that is about to ask the user for permission.
        val state = LinkRequestState()
        val request = state.newRequest("https://example.com")
        state.markForwarded()

        assertFalse(state.shouldFinishOnDismiss(request.id))
    }

    @Test
    fun `dismissing a superseded dialog does not finish the activity`() {
        // A second link replaces the first one. The old dialog is disposed,
        // which must not close the Activity that shows the new dialog.
        val state = LinkRequestState()
        val first = state.newRequest("https://example.com/1")
        state.newRequest("https://example.com/2")

        assertFalse(state.shouldFinishOnDismiss(first.id))
    }

    @Test
    fun `resuming after a hand-over finishes the activity`() {
        val state = LinkRequestState()
        state.newRequest("https://example.com")

        assertFalse(state.shouldFinishOnResume())

        state.markForwarded()
        assertTrue(state.shouldFinishOnResume())
    }

    @Test
    fun `resuming without a hand-over keeps the activity alive`() {
        val state = LinkRequestState()
        state.newRequest("https://example.com")

        assertFalse(state.shouldFinishOnResume())
    }

    @Test
    fun `a failed hand-over keeps the picker open`() {
        val state = LinkRequestState()
        val request = state.newRequest("https://example.com")
        state.markForwarded()
        state.markForwardFailed()

        assertFalse(state.shouldFinishOnResume())
        assertTrue(state.shouldFinishOnDismiss(request.id))
    }

    @Test
    fun `a new link resets the hand-over flag`() {
        // Otherwise `onResume` would close the Activity and the second link
        // would be lost.
        val state = LinkRequestState()
        state.newRequest("https://example.com/1")
        state.markForwarded()

        state.newRequest("https://example.com/2")

        assertFalse(state.shouldFinishOnResume())
        assertFalse(state.hasLaunchedBrowser)
    }
}