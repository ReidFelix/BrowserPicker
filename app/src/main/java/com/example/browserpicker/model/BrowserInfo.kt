package com.example.browserpicker.model

import android.graphics.drawable.Drawable

/**
 * Represents a browser app installed on the device.
 *
 * @property packageName The unique Android package name of the browser
 * @property label The human-readable app name (e.g., "Chrome", "Firefox")
 * @property icon The app icon drawable for display in the list
 */
data class BrowserInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)
