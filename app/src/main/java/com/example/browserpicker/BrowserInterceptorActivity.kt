package com.example.browserpicker

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.example.browserpicker.core.DefaultBrowser
import com.example.browserpicker.core.LinkRequestState
import com.example.browserpicker.core.WebUrl
import com.example.browserpicker.model.BrowserInfo
import com.example.browserpicker.ui.BrowserPickerDialog
import com.example.browserpicker.ui.MainScreen
import com.example.browserpicker.ui.theme.BrowserPickerColorScheme
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The single Activity of the app. It plays two roles:
 *
 *  1. **Link mode** — launched by the system with an `ACTION_VIEW` intent for an
 *     http/https URL. The Activity stays fully transparent and shows a Compose
 *     dialog with the list of installed browsers. Once the user picks one, the
 *     URL is forwarded with an explicit intent and this Activity steps aside.
 *
 *  2. **Launcher mode** — started from the launcher icon. Shows the list of
 *     installed browsers plus a shortcut to the system's default browser
 *     settings.
 */
class BrowserInterceptorActivity : ComponentActivity() {


    private val viewModel: BrowserListViewModel by viewModels()

    /**
     * The URL that currently awaits a decision, or `null` when the Activity was
     * started from the launcher.
     *
     * Holding the URL in a [LinkRequestState] instead of reading it from `intent`
     * in `onCreate` means a second link can be handled without recreating the
     * Activity (and without tearing down the Compose tree).
     */
    private val linkState = LinkRequestState()

    /** Whether the system default browser is currently this app. */
    private val defaultBrowserState = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        extractUrl(intent)?.let { linkState.newRequest(it) }
        defaultBrowserState.value = DefaultBrowser.isDefaultBrowser(this)

        setContent {
            MaterialTheme(colorScheme = BrowserPickerColorScheme) {
                val request by linkState.current.collectAsState()
                val uiState by viewModel.uiState.collectAsState()
                val isDefaultBrowser by defaultBrowserState.collectAsState()

                val current = request
                if (current != null) {
                    // `key` makes Compose discard the previous dialog and build a
                    // new one. That is required because the system dismisses the
                    // dialog when the Activity goes to the background and a
                    // dismissed Compose `Dialog` does not reappear on its own.
                    key(current.id) {
                        BrowserPickerDialog(
                            url = current.url,
                            uiState = uiState,
                            onDismiss = {
                                // The rules live in LinkRequestState and are
                                // covered by unit tests — see
                                // LinkRequestState.shouldFinishOnDismiss.
                                if (linkState.shouldFinishOnDismiss(current.id)) finish()
                            },
                            onBrowserSelected = { browser ->
                                onBrowserSelected(current.url, browser)
                            }
                        )
                    }
                } else {
                    MainScreen(
                        uiState = uiState,
                        isDefaultBrowser = isDefaultBrowser,
                        onRefresh = { viewModel.reloadBrowsers() },
                        onSetDefaultBrowser = { openDefaultBrowserSettings() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val url = extractUrl(intent)
        if (url != null) {
            // A new URL means a new decision is pending, so the "already
            // forwarded" guard is reset — otherwise `onResume` would finish the
            // Activity and the new link would be lost.
            linkState.newRequest(url)
        }
    }

    override fun onResume() {
        super.onResume()
        if (linkState.shouldFinishOnResume()) {
            // The user came back from the target browser — close down.
            finish()
            return
        }
        // The default browser may have changed while we were in the background.
        defaultBrowserState.value = DefaultBrowser.isDefaultBrowser(this)
    }

    private fun onBrowserSelected(url: String, browser: BrowserInfo) {
        linkState.markForwarded()
        openUrlInBrowser(url, browser)
    }

    /**
     * Forwards [url] to [browser] using an explicit intent.
     *
     * `setPackage()` is what prevents the loop: an explicit intent is delivered
     * straight to that package and therefore never matches this app's own
     * `ACTION_VIEW` intent filter again.
     */
    private fun openUrlInBrowser(url: String, browser: BrowserInfo) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(browser.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // The app exists but refuses this URL — keep the picker open so the
            // user can choose a different browser.
            linkState.markForwardFailed()
            Toast.makeText(this, R.string.open_failed_unsupported, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            linkState.markForwardFailed()
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the system screen where the browser role can be handed over to this
     * app. Several intents are tried in order because the most specific one is
     * not available on every Android version.
     */
    private fun openDefaultBrowserSettings() {
        val candidates = listOf(
            { DefaultBrowser.setDefaultIntent(this) },
            { DefaultBrowser.manageDefaultAppsIntent() }
        )
        for (build in candidates) {
            val intent = try {
                build()
            } catch (e: Exception) {
                continue
            }
            try {
                startActivity(intent)
                return
            } catch (e: Exception) {
                // Try the next candidate.
            }
        }
        Toast.makeText(this, R.string.open_settings_failed, Toast.LENGTH_SHORT).show()
    }

    /**
     * Extracts the URL from [intent] if it is a web link this app should handle.
     * Returns `null` for the launcher entry point and for unsupported schemes.
     */
    private fun extractUrl(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (!WebUrl.isSupportedScheme(data.scheme)) return null
        return data.toString()
    }

}