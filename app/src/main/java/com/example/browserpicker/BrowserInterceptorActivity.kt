package com.example.browserpicker

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
import com.example.browserpicker.model.BrowserInfo
import com.example.browserpicker.ui.BrowserPickerDialog
import com.example.browserpicker.ui.MainScreen
import com.example.browserpicker.ui.theme.BrowserPickerColorScheme

class BrowserInterceptorActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FORWARDED = "com.example.browserpicker.FORWARDED"
    }

    private var hasLaunchedBrowser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val incomingUrl = extractUrl(intent)

        setContent {
            MaterialTheme(colorScheme = BrowserPickerColorScheme) {
                if (incomingUrl != null) {
                    val viewModel by viewModels<BrowserListViewModel>()
                    val browsers by viewModel.browsers.collectAsState()

                    BrowserPickerDialog(
                        url = incomingUrl,
                        browsers = browsers,
                        // 只有在未发起跳转时才允许关闭弹窗 finish
                        // Dialog 在切后台时可能自动 dismiss，加守卫避免误销毁
                        onDismiss = {
                            if (!hasLaunchedBrowser) finish()
                        },
                        onBrowserSelected = { browser ->
                            hasLaunchedBrowser = true
                            openUrlInBrowser(incomingUrl, browser)
                        }
                    )
                } else {
                    val viewModel by viewModels<BrowserListViewModel>()
                    val browsers by viewModel.browsers.collectAsState()

                    MainScreen(
                        browsers = browsers,
                        onRefresh = { viewModel.reloadBrowsers() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    override fun onResume() {
        super.onResume()
        // 用户从目标浏览器按返回回到本 Activity，
        // 且之前已经成功发起过跳转 → 关闭自己。
        if (hasLaunchedBrowser) {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (hasLaunchedBrowser && isFinishing) {
            // Activity 正在被销毁，目标浏览器尚未完全打开——
            // 走不到 onResume 清理路径，但此为止也无碍。
        }
    }

    private fun extractUrl(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.getBooleanExtra(EXTRA_FORWARDED, false)) return null
        return intent.data?.toString()
    }

    private fun openUrlInBrowser(url: String, browser: BrowserInfo) {
        try {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(browser.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_FORWARDED, true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            hasLaunchedBrowser = false
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
