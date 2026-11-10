package com.example.browserpicker.model

/**
 * State of the installed-browser list.
 *
 * [isLoading] is kept separate from the list itself so the UI can tell
 * "still querying" apart from "there really is no browser installed" — without
 * it the picker flashed the empty state for a fraction of a second on every
 * cold start.
 *
 * [candidateCount] is the number of apps the platform returned for the query,
 * *before* this app was filtered out. It is only used as a diagnostic in the
 * empty state: it tells "the platform returned nothing" apart from "the
 * platform returned only us".
 */
data class BrowserListUiState(
    val isLoading: Boolean = true,
    val browsers: List<BrowserInfo> = emptyList(),
    val candidateCount: Int = 0
)