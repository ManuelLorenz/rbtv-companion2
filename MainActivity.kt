package tv.rocketbeans.companion

import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.content.Intent
import androidx.work.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleBackgroundRefresh()
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                if (uri.scheme == "https" || uri.scheme == "http") {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    return true
                }
                return false
            }
        }
        web.addJavascriptInterface(ApiBridge(), "RBTV")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
        // Fresh foreground refresh when the app is opened.
        executor.execute { RefreshWorker.refreshNow(applicationContext) }
    }

    private fun scheduleBackgroundRefresh() {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "rbtv_periodic_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    inner class ApiBridge {
        @JavascriptInterface
        fun getJson(path: String, requestId: String) {
            if (!path.startsWith("/") || path.contains("..") || !requestId.matches(Regex("[0-9]+"))) {
                web.post { web.evaluateJavascript("window.apiResult($requestId, null, 'Ungültiger API-Pfad')", null) }
                return
            }
            executor.execute {
                val (result, error) = RefreshWorker.fetch(path)
                val safeResult = result?.let { JSONObject.quote(it) } ?: "null"
                val safeError = error?.let { JSONObject.quote(it) } ?: "null"
                web.post { web.evaluateJavascript("window.apiResult($requestId, $safeResult, $safeError)", null) }
            }
        }

        @JavascriptInterface
        fun getCachedSnapshot() {
            val prefs = getSharedPreferences(RefreshWorker.PREFS, MODE_PRIVATE)
            val obj = JSONObject()
            listOf("plan", "videos", "news").forEach { key ->
                val raw = prefs.getString(key, null)
                if (raw != null) obj.put(key, org.json.JSONTokener(raw).nextValue())
            }
            obj.put("updatedAt", prefs.getLong("updatedAt", 0L))
            val payload = JSONObject.quote(obj.toString())
            web.post { web.evaluateJavascript("window.cachedSnapshot($payload)", null) }
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        web.destroy()
        super.onDestroy()
    }
}
