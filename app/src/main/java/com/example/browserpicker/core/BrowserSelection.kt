package com.example.browserpicker.core

import java.util.Locale

/**
 * A browser candidate discovered through [android.content.pm.PackageManager],
 * before its icon has been loaded.
 *
 * Deliberately free of any Android framework type so the selection rules can be
 * unit tested on the JVM.
 */
data class BrowserCandidate(
    val packageName: String,
    val label: String
)

/**
 * Pure selection rules applied to the list of apps that can open a web URL.
 */
object BrowserSelection {

    /**
     * Removes our own package, keeps only the first entry per package name and
     * sorts the result by label (case-insensitive).
     *
     * De-duplicating *before* the icons are loaded matters: a single app often
     * registers several activities that can handle `ACTION_VIEW`, and loading an
     * icon is comparatively expensive.
     */
    fun select(
        candidates: List<BrowserCandidate>,
        ownPackageName: String
    ): List<BrowserCandidate> = candidates
        .asSequence()
        .filter { it.packageName != ownPackageName }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase(Locale.ROOT) }
        .toList()
}