package com.example.browserpicker

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browserpicker.core.BrowserCandidate
import com.example.browserpicker.core.BrowserSelection
import com.example.browserpicker.model.BrowserInfo
import com.example.browserpicker.model.BrowserListUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Queries the apps that are able to open an `ACTION_VIEW` web URL and exposes
 * them as [BrowserListUiState].
 */
class BrowserListViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BrowserListUiState())
    val uiState: StateFlow<BrowserListUiState> = _uiState.asStateFlow()

    /** Keeps track of the running query so a manual refresh cancels the previous one. */
    private var loadJob: Job? = null

    init {
        reloadBrowsers()
    }

    fun reloadBrowsers() {
        loadJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadJob = viewModelScope.launch {
            val app = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                queryInstalledBrowsers(app.packageManager, app.packageName)
            }
            _uiState.value = BrowserListUiState(
                isLoading = false,
                browsers = result.browsers,
                candidateCount = result.candidateCount
            )
        }
    }

    companion object {
        private const val PROBE_URL = "https://example.com"

        /**
         * Queries every app that is able to open a web URL.
         *
         * The query is intentionally kept as permissive as it was in v1.0:
         *
         *  * only `CATEGORY_BROWSABLE` is added — adding `CATEGORY_DEFAULT` as
         *    well narrows the match down to the app the system currently
         *    considers the default handler, which (since this app *is* the
         *    default browser) would leave us with an empty list, and
         *  * `MATCH_ALL` is used instead of `MATCH_DEFAULT_ONLY` for the same
         *    reason.
         *
         * Both of these were "tightened" at some point and immediately broke
         * browser discovery on real devices, so: do not restrict this query.
         */
        fun queryInstalledBrowsers(
            pm: PackageManager,
            ownPackageName: String
        ): BrowserQueryResult {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PROBE_URL)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }

            // A single app frequently registers several activities that can open a
            // URL. Collapse them by package *before* the (comparatively expensive)
            // icon is loaded, so each app is only processed once.
            val appInfos = LinkedHashMap<String, ApplicationInfo>()
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .forEach { resolveInfo ->
                    val appInfo = resolveInfo.activityInfo.applicationInfo
                    appInfos.putIfAbsent(appInfo.packageName, appInfo)
                }

            val candidates = appInfos.values.map { appInfo ->
                BrowserCandidate(
                    packageName = appInfo.packageName,
                    label = appInfo.loadLabel(pm).toString()
                )
            }

            return BrowserQueryResult(
                browsers = BrowserSelection.select(candidates, ownPackageName).map { candidate ->
                    val appInfo = appInfos.getValue(candidate.packageName)
                    BrowserInfo(
                        packageName = candidate.packageName,
                        label = candidate.label,
                        icon = appInfo.loadIcon(pm)
                    )
                },
                candidateCount = candidates.size
            )
        }
    }
}

/**
 * Result of a browser query.
 *
 * [candidateCount] is the number of apps the platform returned, *including*
 * this app. It is used as a diagnostic in the empty state.
 */
data class BrowserQueryResult(
    val browsers: List<BrowserInfo>,
    val candidateCount: Int
)