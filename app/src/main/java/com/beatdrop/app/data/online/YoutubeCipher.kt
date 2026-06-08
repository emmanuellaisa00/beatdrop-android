package com.beatdrop.app.data.online

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.mozilla.javascript.Context as RhinoContext
import org.mozilla.javascript.Function as RhinoFunction
import org.mozilla.javascript.ScriptableObject
import java.util.concurrent.TimeUnit

/**
 * Native YouTube signature + n-throttle decipher — ported from
 * github.com/emmanuellaisa00/beatdroppremium (YoutubeCipher.kt).
 *
 * Many YouTube formats ship a `signatureCipher` (obfuscated URL signature) and/or
 * a throttled `n` query param. yt-dlp / NewPipe solve this by extracting the two
 * JS transforms from the player's base.js and running them. We do the same:
 * download base.js once (cached), extract the `sig` + `nsig` functions, and run
 * them with Mozilla Rhino so we can accept ciphered formats the plain path drops.
 */
object YoutubeCipher {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val mutex = Mutex()

    private data class PlayerFns(val sigJs: String?, val nsigJs: String?)
    @Volatile private var cachedUrl: String? = null
    @Volatile private var cachedFns: PlayerFns? = null
    private val nCache = HashMap<String, String>()

    /**
     * Resolve a single format JSON into a directly-playable URL.
     *   1. Plain `url`              → return (after nsig fix)
     *   2. `signatureCipher`/`cipher` → decipher signature, then nsig fix
     * Returns null if it cannot be resolved.
     */
    suspend fun resolveFormatUrl(format: JSONObject): String? {
        val plain = format.optString("url").ifBlank { null }
        val cipher = format.optString("signatureCipher")
            .ifBlank { format.optString("cipher").ifBlank { null } }

        val baseUrl: String
        val sigParam: String?
        val sigValue: String?

        if (plain != null) {
            baseUrl = plain; sigParam = null; sigValue = null
        } else if (cipher != null) {
            val params = parseQuery(cipher)
            val u = params["url"] ?: return null
            val s = params["s"]
            val sp = params["sp"] ?: "signature"
            val decoded = if (s != null) decipherSignature(s) ?: return null else null
            baseUrl = u; sigParam = sp; sigValue = decoded
        } else return null

        var finalUrl = baseUrl
        if (sigValue != null && sigParam != null) finalUrl = appendQuery(finalUrl, sigParam, sigValue)
        finalUrl = fixNParam(finalUrl)
        return finalUrl
    }

    suspend fun ensurePlayer(playerJsUrl: String) {
        if (cachedUrl == playerJsUrl && cachedFns != null) return
        mutex.withLock {
            if (cachedUrl == playerJsUrl && cachedFns != null) return
            val js = downloadBaseJs(playerJsUrl) ?: return
            cachedFns = PlayerFns(extractSigFunction(js), extractNSigFunction(js))
            cachedUrl = playerJsUrl
            nCache.clear()
        }
    }

    private suspend fun decipherSignature(s: String): String? = withContext(Dispatchers.Default) {
        val sigJs = cachedFns?.sigJs ?: return@withContext null
        runJs(sigJs, "beatdropSig", s)
    }

    private suspend fun fixNParam(url: String): String = withContext(Dispatchers.Default) {
        val nsigJs = cachedFns?.nsigJs ?: return@withContext url
        val n = Uri.parse(url).getQueryParameter("n") ?: return@withContext url
        val key = (cachedUrl ?: "") + "|" + n
        val transformed = synchronized(nCache) { nCache[key] }
            ?: runJs(nsigJs, "beatdropNsig", n)?.also { synchronized(nCache) { nCache[key] = it } }
            ?: return@withContext url
        if (transformed.isBlank() || transformed.startsWith("enhanced_except")) return@withContext url
        appendQuery(stripQuery(url, "n"), "n", transformed)
    }

    private fun downloadBaseJs(playerJsUrl: String): String? = try {
        val url = if (playerJsUrl.startsWith("http")) playerJsUrl else "https://www.youtube.com$playerJsUrl"
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    } catch (_: Exception) { null }

    @Volatile private var cachedPlayerJsUrl: String? = null

    suspend fun discoverPlayerJsUrlCached(): String? {
        cachedPlayerJsUrl?.let { return it }
        val url = discoverPlayerJsUrl()
        if (url != null) {
            cachedPlayerJsUrl = url
            synchronized(nCache) { nCache.clear() }
        }
        return url
    }

    private suspend fun discoverPlayerJsUrl(): String? = withContext(Dispatchers.Default) {
        runCatching {
            val embedReq = Request.Builder()
                .url("https://www.youtube.com/embed/dQw4w9WgXcQ")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .header("Range", "bytes=0-153600")
                .build()
            val body = http.newCall(embedReq).execute().use { it.body?.string() } ?: return@runCatching null
            val m = Regex("""(/s/player/[0-9a-fA-F]{6,}/player[a-zA-Z0-9_]*\.vflset/[a-zA-Z_]+/base\.js)""")
                .find(body) ?: return@runCatching null
            "https://www.youtube.com${m.groupValues[1]}"
        }.getOrNull()
    }

    // ── JS function extraction (yt-dlp-style regexes) ──

    private fun extractSigFunction(js: String): String? {
        val sigFnName =
            Regex("""(?:\b|[^a-zA-Z0-9${'$'}])([a-zA-Z0-9${'$'}]{2,})\s*=\s*function\(\s*([a-zA-Z0-9${'$'}]+)\s*\)\s*\{\s*\2\s*=\s*\2\.split\(\s*""\s*\)""")
                .find(js)?.groupValues?.get(1)
            ?: Regex("""\b[a-zA-Z0-9${'$'}]+\s*&&\s*[a-zA-Z0-9${'$'}]+\.set\([^,]+,\s*(?:\([^)]*\)\s*)?\(?\s*0?,?\s*([a-zA-Z0-9${'$'}]+)\s*\)?\(""")
                .find(js)?.groupValues?.get(1)?.ifBlank { null }
            ?: Regex("""(?:["'])?([a-zA-Z0-9${'$'}]+)(?:["'])?\s*:\s*function\(\s*[a-zA-Z]\s*\)\s*\{\s*[a-zA-Z]\s*=\s*[a-zA-Z]\.split\(\s*""\s*\)""")
                .find(js)?.groupValues?.get(1)
            ?: findSigNameViaCaller(js)
            ?: return null

        val fnBody = extractFunctionBody(js, sigFnName) ?: return null
        val helperName = Regex(""";([a-zA-Z0-9${'$'}]{2,})\.""").find(fnBody)?.groupValues?.get(1)
        val helperObj = helperName?.let { extractObject(js, it) } ?: ""
        return """
            $helperObj
            function $sigFnName$fnBody
            function beatdropSig(s){ return $sigFnName(s); }
        """.trimIndent()
    }

    private fun findSigNameViaCaller(js: String): String? =
        Regex("""\b([a-zA-Z0-9${'$'}]+)\s*\(\s*decodeURIComponent""").find(js)?.groupValues?.get(1)

    private fun extractNSigFunction(js: String): String? {
        val nFnName =
            Regex("""(?:\.get\(\s*"n"\s*\)\s*\)|=\s*[a-zA-Z]\.get\([a-zA-Z]\)\s*\))\s*&&\s*\(\s*[a-zA-Z]\s*=\s*([a-zA-Z0-9${'$'}]+)(?:\[(\d+)\])?\(""")
                .find(js)?.let { m ->
                    val raw = m.groupValues[1]
                    val idx = m.groupValues.getOrNull(2)
                    if (!idx.isNullOrBlank()) resolveArrayRef(js, raw, idx.toInt()) else raw
                }
            ?: Regex("""([a-zA-Z0-9${'$'}]+)\s*=\s*function\(\s*[a-zA-Z]\s*\)\s*\{\s*var\s*[a-zA-Z]\s*=\s*[a-zA-Z]\.split\(""")
                .find(js)?.groupValues?.get(1)
            ?: Regex(""";\s*([a-zA-Z0-9_${'$'}]+)\s*=\s*function\([a-zA-Z0-9_${'$'}]+\)\s*\{[^{}]*enhanced_except""")
                .find(js)?.groupValues?.get(1)
            ?: Regex("""function\s+([a-zA-Z0-9_${'$'}]+)\s*\([a-zA-Z0-9_${'$'}]+\)\s*\{[^{}]*enhanced_except""")
                .find(js)?.groupValues?.get(1)
            ?: return null

        val body = extractFunctionBody(js, nFnName) ?: return null
        return """
            function $nFnName$body
            function beatdropNsig(n){ try { return $nFnName(n); } catch(e){ return n; } }
        """.trimIndent()
    }

    private fun resolveArrayRef(js: String, arrName: String, idx: Int): String? {
        val arr = Regex("""var\s+${Regex.escape(arrName)}\s*=\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
            .find(js)?.groupValues?.get(1) ?: return null
        return arr.split(',').map { it.trim() }.getOrNull(idx)
    }

    private fun extractFunctionBody(js: String, name: String): String? {
        val escaped = Regex.escape(name)
        val patterns = listOf(
            Regex("""function\s+$escaped\s*\("""),
            Regex("""\b$escaped\s*=\s*function\s*\("""),
            Regex("""var\s+$escaped\s*=\s*function\s*\("""),
            Regex("""["']?$escaped["']?\s*:\s*function\s*\("""),
        )
        for (re in patterns) {
            val m = re.find(js) ?: continue
            val parenOpen = js.indexOf('(', m.range.first); if (parenOpen < 0) continue
            val parenClose = js.indexOf(')', parenOpen); if (parenClose < 0) continue
            val braceOpen = js.indexOf('{', parenClose); if (braceOpen < 0) continue
            val braceClose = matchBrace(js, braceOpen) ?: continue
            val args = js.substring(parenOpen, parenClose + 1)
            return "$args${js.substring(braceOpen, braceClose + 1)}"
        }
        return null
    }

    private fun extractObject(js: String, name: String): String? {
        val escaped = Regex.escape(name)
        val m = Regex("""var\s+$escaped\s*=\s*\{""").find(js) ?: return null
        val open = js.indexOf('{', m.range.first)
        val close = matchBrace(js, open) ?: return null
        return "var $name=${js.substring(open, close + 1)};"
    }

    private fun matchBrace(s: String, openIdx: Int): Int? {
        var depth = 0; var i = openIdx; var inStr: Char? = null; var prev = ' '
        while (i < s.length) {
            val c = s[i]
            if (inStr != null) {
                if (c == inStr && prev != '\\') inStr = null
            } else when (c) {
                '"', '\'', '`' -> inStr = c
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) return i }
            }
            prev = c; i++
        }
        return null
    }

    private fun runJs(source: String, entry: String, arg: String): String? {
        val ctx = RhinoContext.enter()
        return try {
            ctx.optimizationLevel = -1 // interpreted mode (required on Android)
            val scope: ScriptableObject = ctx.initSafeStandardObjects()
            ctx.evaluateString(scope, source, "yt", 1, null)
            val fn = scope.get(entry, scope) as? RhinoFunction ?: return null
            val result = fn.call(ctx, scope, scope, arrayOf<Any>(arg))
            RhinoContext.toString(result)
        } catch (_: Exception) { null } finally { RhinoContext.exit() }
    }

    private fun parseQuery(q: String): Map<String, String> =
        q.split('&').mapNotNull {
            val idx = it.indexOf('=')
            if (idx < 0) null else Uri.decode(it.substring(0, idx)) to Uri.decode(it.substring(idx + 1))
        }.toMap()

    private fun appendQuery(url: String, key: String, value: String): String {
        val sep = if (url.contains('?')) '&' else '?'
        return "$url$sep${Uri.encode(key)}=${Uri.encode(value)}"
    }

    private fun stripQuery(url: String, key: String): String {
        val uri = Uri.parse(url)
        val builder = uri.buildUpon().clearQuery()
        for (name in uri.queryParameterNames) {
            if (name == key) continue
            for (v in uri.getQueryParameters(name)) builder.appendQueryParameter(name, v)
        }
        return builder.build().toString()
    }
}
