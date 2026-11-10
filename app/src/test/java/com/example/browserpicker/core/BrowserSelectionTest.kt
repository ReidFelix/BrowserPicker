package com.example.browserpicker.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserSelectionTest {

    private fun candidate(pkg: String, label: String) = BrowserCandidate(pkg, label)

    @Test
    fun `own package is removed from the list`() {
        val result = BrowserSelection.select(
            candidates = listOf(
                candidate("com.example.browserpicker", "Browser Picker"),
                candidate("com.android.chrome", "Chrome")
            ),
            ownPackageName = "com.example.browserpicker"
        )

        assertEquals(listOf("com.android.chrome"), result.map { it.packageName })
    }

    @Test
    fun `duplicate package names are collapsed keeping the first entry`() {
        val result = BrowserSelection.select(
            candidates = listOf(
                candidate("com.android.chrome", "Chrome"),
                candidate("com.android.chrome", "Chrome Beta"),
                candidate("org.mozilla.firefox", "Firefox")
            ),
            ownPackageName = "com.example.browserpicker"
        )

        assertEquals(2, result.size)
        assertEquals("Chrome", result.first { it.packageName == "com.android.chrome" }.label)
    }

    @Test
    fun `list is sorted by label ignoring case`() {
        val result = BrowserSelection.select(
            candidates = listOf(
                candidate("b", "brave"),
                candidate("c", "Chrome"),
                candidate("f", "Firefox"),
                candidate("e", "Edge")
            ),
            ownPackageName = "com.example.browserpicker"
        )

        assertEquals(
            listOf("brave", "Chrome", "Edge", "Firefox"),
            result.map { it.label }
        )
    }

    @Test
    fun `an empty input yields an empty result`() {
        val result = BrowserSelection.select(
            candidates = emptyList(),
            ownPackageName = "com.example.browserpicker"
        )

        assertEquals(emptyList<BrowserCandidate>(), result)
    }

    @Test
    fun `a list that only contains our own package yields an empty result`() {
        val result = BrowserSelection.select(
            candidates = listOf(candidate("com.example.browserpicker", "Browser Picker")),
            ownPackageName = "com.example.browserpicker"
        )

        assertEquals(emptyList<BrowserCandidate>(), result)
    }
}