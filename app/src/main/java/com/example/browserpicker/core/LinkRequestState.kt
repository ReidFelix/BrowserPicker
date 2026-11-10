package com.example.browserpicker.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A URL that is waiting for the user to pick a browser.
 *
 * @property id monotonically increasing id. Every URL gets a new one, which is
 *   what allows the UI to tell "this is a different link" from "the same link
 *   again".
 */
data class LinkRequest(
    val id: Long,
    val url: String
)

/**
 * State machine for the link that is currently being handled.
 *
 * It contains the two decisions that are easy to get wrong:
 *
 *  * when a dialog may close the Activity, and
 *  * when the Activity has to close itself.
 *
 * Everything here is plain Kotlin (no Android framework types), so the
 * transitions are covered by unit tests.
 */
class LinkRequestState {

    private val _current = MutableStateFlow<LinkRequest?>(null)

    /** The request the UI is showing, or `null` when there is none. */
    val current: StateFlow<LinkRequest?> = _current.asStateFlow()

    /** Whether a browser has already been handed the URL. */
    var hasLaunchedBrowser: Boolean = false
        private set

    private var counter = 0L

    /**
     * Registers a new URL and returns it.
     *
     * A new URL resets [hasLaunchedBrowser]: the previous link is no longer
     * being handled, so coming back to the foreground must not close the
     * Activity — otherwise the new link would be lost.
     */
    fun newRequest(url: String): LinkRequest {
        counter += 1
        val request = LinkRequest(counter, url)
        _current.value = request
        hasLaunchedBrowser = false
        return request
    }

    /** Marks the current request as handed over to a browser. */
    fun markForwarded() {
        hasLaunchedBrowser = true
    }

    /**
     * Called when forwarding the URL failed — the picker stays open so the user
     * can try another browser.
     */
    fun markForwardFailed() {
        hasLaunchedBrowser = false
    }

    /**
     * Whether the Activity should be finished because the dialog was dismissed.
     *
     * That is only the case when the dismissed dialog still is the current one
     * and no browser was launched:
     *
     *  * after a successful hand-over the system dismisses the dialog while the
     *    Activity moves to the background, and closing the Activity there would
     *    cancel the browser that is about to ask for permission, and
     *  * when a new URL arrives the old dialog is disposed, and disposing it
     *    must not close the Activity that is about to show the new dialog.
     */
    fun shouldFinishOnDismiss(dismissedRequestId: Long): Boolean =
        _current.value?.id == dismissedRequestId && !hasLaunchedBrowser

    /** Whether the Activity should be finished when it comes back to the foreground. */
    fun shouldFinishOnResume(): Boolean = hasLaunchedBrowser
}