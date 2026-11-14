package tv.rocketbeans.companion

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val paths = mapOf(
            "plan" to "/schedule/publish?from=${System.currentTimeMillis() / 1000}",
            "videos" to "/media/episode/preview/newest?limit=50&order=DESC",
            "news" to "/blog/preview/all?limit=50"
        )
        var successes = 0
        paths.forEach { (key, path) ->
            val (body, _) = fetch(path)
            if (body != null) {
                applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(key, body).apply()
                successes++
            }
        }
        if (successes > 0) {
            applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong("updatedAt", System.currentTimeMillis()).apply()
            return Result.success()
        }
        return Result.retry()
    }

    companion object {
        const val PREFS = "rbtv_cache"
        private const val BASE = "https://api.rocketbeans.tv/v1"

        fun fetch(path: String): Pair<String?, String?> {
            var connection: HttpURLConnection? = null
            return try {
                connection = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12000
                    readTimeout = 15000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "RBTVCompanion-Android/1.1")
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }
                if (code in 200..299 && !text.isNullOrBlank()) Pair(text, null)
                else Pair(null, "HTTP $code")
            } catch (e: Exception) {
                Pair(null, e.message ?: "Netzwerkfehler")
            } finally {
                connection?.disconnect()
            }
        }

        fun refreshNow(context: Context) {
            val paths = mapOf(
                "plan" to "/schedule/publish?from=${System.currentTimeMillis() / 1000}",
                "videos" to "/media/episode/preview/newest?limit=50&order=DESC",
                "news" to "/blog/preview/all?limit=50"
            )
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            var any = false
            paths.forEach { (key, path) ->
                val (body, _) = fetch(path)
                if (body != null) {
                    prefs.edit().putString(key, body).apply()
                    any = true
                }
            }
            if (any) prefs.edit().putLong("updatedAt", System.currentTimeMillis()).apply()
        }
    }
}
