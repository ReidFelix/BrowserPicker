package com.example.browserpicker.core

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Helpers around the "default browser" role.
 *
 * From Android 10 (API 29) onwards the system exposes a dedicated
 * `ROLE_BROWSER` role. Below that the only way to find out is to resolve the
 * `ACTION_VIEW` intent and compare the package name.
 *
 * Note: an app becomes a *candidate* for the browser role purely by declaring
 * an `ACTION_VIEW` + `BROWSABLE` intent filter for http/https — there is no
 * `android:role` manifest attribute for that.
 */
object DefaultBrowser {

    /** Whether this app is currently the system default browser. */
    fun isDefaultBrowser(context: Context): Boolean {
        // Resolving the intent is the most reliable answer: it is exactly what
        // the system uses to decide where a link opens.
        if (isDefaultBrowserViaResolve(context)) return true

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
                ?.isRoleHeld(RoleManager.ROLE_BROWSER) == true
        } else {
            false
        }
    }

    /**
     * Intent that opens the system screen where the user can hand the browser
     * role over to this app.
     *
     * * API 29+ — `RoleManager` knows the exact screen for requesting the role.
     * * API 31+ — the per-app "app links" screen, which also offers the
     *   "set as default" action.
     * * API 26..30 — the generic list of apps that can be set as default.
     */
    fun setDefaultIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null) {
                try {
                    return roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                } catch (e: Exception) {
                    // Role not available — fall through to the screens below.
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
        }
        return manageDefaultAppsIntent()
    }

    /** System screen listing all apps that can be set as default. */
    fun manageDefaultAppsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)

    @SuppressLint("QueryPermissionsNeeded")
    private fun isDefaultBrowserViaResolve(context: Context): Boolean {
        val resolved = context.packageManager.resolveActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(PROBE_URL)),
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolved?.activityInfo?.packageName == context.packageName
    }

    private const val PROBE_URL = "https://example.com"
}