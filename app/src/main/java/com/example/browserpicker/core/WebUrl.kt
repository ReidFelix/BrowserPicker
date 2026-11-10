package com.example.browserpicker.core

import java.util.Locale

/**
 * Helpers for the URLs this app is allowed to intercept.
 */
object WebUrl {

    /** The schemes the browser intent filters are registered for. */
    val SUPPORTED_SCHEMES = setOf("http", "https")

    /** Whether [scheme] is a web scheme this app handles. */
    fun isSupportedScheme(scheme: String?): Boolean {
        val normalized = scheme?.lowercase(Locale.ROOT) ?: return false
        return normalized in SUPPORTED_SCHEMES
    }
}