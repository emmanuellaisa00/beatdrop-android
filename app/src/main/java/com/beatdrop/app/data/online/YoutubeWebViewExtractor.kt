package com.beatdrop.app.data.online

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Headless WebView stream extractor — the robust fallback (BotGuard-immune)
 * ported from github.com/emmanuellaisa00/beatdroppremium.
 *
 * Loads YouTube's embed page in a real (hidden) WebView so YouTube's own JS —
 * including base.js signature/n-throttle and PO-token logic — runs natively,
 * then reads `ytInitialPlayerResponse.streamingData` and returns the best
 * audio URL. Slower than the Innertube path, so it's only used when the
 * cipher-aware Innertube chain comes up empty.
 *
 * Requires an Application context, set once in BeatDropApp.onCreate().
 */
object YoutubeWebViewExtractor {

    private const val CHROME_UA =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    @Volatile private var appContext: Context? = null
    fun init(context: Context) { appContext = context.applicationContext }

    private val main = Handler(Looper.getMainLooper())

    suspend fun extract(videoId: String, timeoutMs: Long = 15_000): String? {
        val ctx = appContext ?: return null
        val deferred = CompletableDeferred<String?>()

        withContext(Dispatchers.Main) {
            @SuppressLint("SetJavaScriptEnabled")
            val web = WebView(ctx)
            web.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                userAgentString = CHROME_UA
            }
            web.addJavascriptInterface(object {
                @JavascriptInterface
                fun onResult(json: String) {
                    deferred.complete(parseBestAudio(json))
                }
                @JavascriptInterface
                fun onError(@Suppress("UNUSED_PARAMETER") msg: String) {
                    if (!deferred.isCompleted) deferred.complete(null)
                }
            }, "BeatDropBridge")

            web.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    // Poll for ytInitialPlayerResponse, then hand the streamingData back.
                    view.evaluateJavascript(POLL_JS, null)
                }
            }
            web.loadUrl(
                "https://www.youtube.com/embed/$videoId" +
                    "?autoplay=0&playsinline=1&mute=1&origin=https://www.youtube.com"
            )

            // Tear the WebView down when we're done.
            deferred.invokeOnCompletion {
                main.post {
                    web.stopLoading()
                    web.loadUrl("about:blank")
                    web.destroy()
                }
            }
        }

        return withTimeoutOrNull(timeoutMs) { deferred.await() }
    }

    /** Reads ytInitialPlayerResponse.streamingData and posts it back as JSON. */
    private val POLL_JS = """
        (function(){
          var tries = 0;
          var iv = setInterval(function(){
            tries++;
            try {
              var pr = window.ytInitialPlayerResponse;
              if (pr && pr.streamingData) {
                clearInterval(iv);
                BeatDropBridge.onResult(JSON.stringify(pr.streamingData));
              } else if (tries > 40) {
                clearInterval(iv);
                BeatDropBridge.onError('timeout');
              }
            } catch(e) { clearInterval(iv); BeatDropBridge.onError(String(e)); }
          }, 250);
        })();
    """.trimIndent()

    /** Pick the best audio URL from a streamingData JSON string (URLs here are already de-ciphered by the page). */
    private fun parseBestAudio(streamingDataJson: String): String? = runCatching {
        val sd = JSONObject(streamingDataJson)
        fun best(arr: JSONArray?): String? {
            if (arr == null) return null
            return (0 until arr.length())
                .map { arr.getJSONObject(it) }
                .filter { (it.optString("mimeType") + it.optString("type")).lowercase().contains("audio/") }
                .filter { it.has("url") && it.optString("url").isNotBlank() }
                .maxByOrNull { it.optLong("bitrate").coerceAtLeast(it.optLong("averageBitrate")) }
                ?.optString("url")
        }
        best(sd.optJSONArray("adaptiveFormats")) ?: best(sd.optJSONArray("formats"))
    }.getOrNull()
}
