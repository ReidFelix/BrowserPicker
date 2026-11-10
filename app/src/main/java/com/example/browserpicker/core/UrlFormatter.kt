package com.example.browserpicker.core

/**
 * Formatting helpers for the URL that is about to be opened.
 */
object UrlFormatter {

    private val SCHEMES = listOf("https://", "http://")

    /**
     * Renders [raw] in a compact, human friendly way: the scheme and a trailing
     * slash are dropped so the interesting part of the URL is visible.
     *
     * This is purely cosmetic — the value that gets copied to the clipboard is
     * always the untouched original URL.
     */
    fun display(raw: String): String {
        val trimmed = raw.trim()
        var text = trimmed
        for (scheme in SCHEMES) {
            if (text.startsWith(scheme, ignoreCase = true)) {
                text = text.substring(scheme.length)
                break
            }
        }
        while (text.length > 1 && text.endsWith("/")) {
            text = text.dropLast(1)
        }
        return text.ifBlank { trimmed }
    }
}