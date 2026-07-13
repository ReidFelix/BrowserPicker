package com.example.browserpicker

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browserpicker.model.BrowserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowserListViewModel(private val app: android.app.Application) : AndroidViewModel(app) {

    private val _browsers = MutableStateFlow<List<BrowserInfo>>(emptyList())
    val browsers: StateFlow<List<BrowserInfo>> = _browsers.asStateFlow()

    init {
        loadBrowsers()
    }

    fun reloadBrowsers() {
        loadBrowsers()
    }

    private fun loadBrowsers() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                queryInstalledBrowsers(app.packageManager, app.packageName)
            }
            _browsers.value = list
        }
    }

    companion object {
        fun queryInstalledBrowsers(pm: PackageManager, ownPackageName: String): List<BrowserInfo> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }

            return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .filter { it.activityInfo.applicationInfo.packageName != ownPackageName }
                .map { ri ->
                    val appInfo = ri.activityInfo.applicationInfo
                    BrowserInfo(
                        packageName = appInfo.packageName,
                        label = appInfo.loadLabel(pm).toString(),
                        icon = appInfo.loadIcon(pm)
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
        }
    }
}
