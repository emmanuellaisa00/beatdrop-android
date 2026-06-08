package com.beatdrop.app.data.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * YouTube Innertube engine — same method as
 * github.com/emmanuellaisa00/beatdroppremium: no YouTube Data API key.
 *
 *  • Search songs/albums/playlists via music.youtube.com /youtubei/v1/search
 *    (WEB_REMIX client, category-filter params) → curated music results.
 *  • Resolve a playable audio URL via youtubei/v1/player with a chain of
 *    Innertube clients known to return plain (un-ciphered) audio in 2026.
 *
 * NOTE: this is a pragmatic port. The reference repo additionally handles
 * ciphered formats (base.js + Rhino), a WebView extractor, and Invidious
 * fallbacks. Those slot in behind getStream() without changing the UI.
 */
object YoutubeService {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val YT_MUSIC_SEARCH = "https://music.youtube.com/youtubei/v1/search"
    private const val YT_MUSIC_NEXT = "https://music.youtube.com/youtubei/v1/next"
    private const val YT_PLAYER = "https://www.youtube.com/youtubei/v1/player"

    // Category-filter params (base64) — songs / albums / playlists
    private const val PARAMS_SONGS = "EgWKAQIIAWoQEAMQBBAJEAoQERAQEBUQHg=="
    private const val PARAMS_ALBUMS = "EgWKAQIYAWoQEAMQBBAJEAoQERAQEBUQHg=="
    private const val PARAMS_PLAYLISTS = "EgWKAQIoAWoQEAMQBBAJEAoQERAQEBUQHg=="

    private val JSON = "application/json".toMediaType()

    // 90-minute stream-URL cache (googlevideo tokens expire fairly fast)
    private data class CacheEntry(val stream: ResolvedStream, val at: Long)
    private val streamCache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 90 * 60 * 1000L

    // ─────────────────────────────────────────────────────────── Search ──

    suspend fun searchAll(query: String): OnlineSearchResults = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext OnlineSearchResults()
        // Songs first (the primary list); albums + playlists best-effort.
        val songs = runCatching { searchSongs(q) }.getOrDefault(emptyList())
        val albums = runCatching { searchAlbums(q) }.getOrDefault(emptyList())
        val playlists = runCatching { searchPlaylists(q) }.getOrDefault(emptyList())
        OnlineSearchResults(songs, albums, playlists)
    }

    suspend fun searchSongs(query: String, max: Int = 30): List<OnlineResult> =
        withContext(Dispatchers.IO) {
            val json = musicSearch(query, PARAMS_SONGS) ?: return@withContext emptyList()
            parseSongs(json).take(max)
        }

    suspend fun searchAlbums(query: String, max: Int = 12): List<OnlineAlbum> =
        withContext(Dispatchers.IO) {
            val json = musicSearch(query, PARAMS_ALBUMS) ?: return@withContext emptyList()
            parseAlbums(json).take(max)
        }

    suspend fun searchPlaylists(query: String, max: Int = 12): List<OnlinePlaylist> =
        withContext(Dispatchers.IO) {
            val json = musicSearch(query, PARAMS_PLAYLISTS) ?: return@withContext emptyList()
            parsePlaylists(json).take(max)
        }

    private fun musicSearch(query: String, params: String): JSONObject? {
        val body = JSONObject().apply {
            put("query", query.trim())
            put("params", params)
            put("context", JSONObject().put("client", JSONObject().apply {
                put("clientName", "WEB_REMIX")
                put("clientVersion", "1.20240501.01.00")
                put("hl", "en"); put("gl", "US"); put("utcOffsetMinutes", 0)
            }))
        }.toString()
        val req = Request.Builder()
            .url("$YT_MUSIC_SEARCH?prettyPrint=false")
            .post(body.toRequestBody(JSON))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .header("X-Youtube-Client-Name", "67")
            .header("X-Youtube-Client-Version", "1.20240501.01.00")
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .build()
        return runCatching {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                JSONObject(resp.body!!.string())
            }
        }.getOrNull()
    }

    // ── Parsers (musicResponsiveListItemRenderer) ──

    private fun collectRenderers(root: JSONObject): List<JSONObject> {
        val items = ArrayList<JSONObject>()
        fun walk(o: Any?) {
            when (o) {
                is JSONObject -> {
                    o.optJSONObject("musicResponsiveListItemRenderer")?.let { items.add(it) }
                    o.keys().forEach { walk(o.opt(it)) }
                }
                is JSONArray -> for (i in 0 until o.length()) walk(o.opt(i))
            }
        }
        walk(root)
        return items
    }

    private fun flexText(item: JSONObject, col: Int, run: Int = 0): String? =
        item.optJSONArray("flexColumns")
            ?.optJSONObject(col)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")?.optJSONArray("runs")
            ?.optJSONObject(run)?.optString("text")

    private fun thumbOf(item: JSONObject, videoId: String? = null): String? {
        val t = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val url = t?.let { if (it.length() > 0) it.getJSONObject(it.length() - 1).optString("url") else null }
        return url ?: videoId?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
    }

    private fun parseSongs(root: JSONObject): List<OnlineResult> {
        val out = ArrayList<OnlineResult>()
        for (item in collectRenderers(root)) {
            val videoId = item.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?: item.optJSONObject("playlistItemData")?.optString("videoId")
                ?: continue
            if (videoId.isBlank()) continue
            val title = flexText(item, 0) ?: continue

            val secondary = item.optJSONArray("flexColumns")
                ?.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")?.optJSONArray("runs")
            var artist = ""
            var durationText = ""
            if (secondary != null) {
                val parts = (0 until secondary.length())
                    .mapNotNull { secondary.optJSONObject(it)?.optString("text") }
                    .filter { it.isNotBlank() && it != " • " }
                if (parts.isNotEmpty()) artist = parts.first()
                parts.lastOrNull { it.matches(Regex("\\d+:\\d{2}(?::\\d{2})?")) }?.let { durationText = it }
            }
            out.add(
                OnlineResult(
                    videoId = videoId, title = title, author = artist,
                    thumbnailUrl = thumbOf(item, videoId),
                    durationText = durationText, durationSecs = parseDuration(durationText),
                )
            )
        }
        return out
    }

    private fun parseAlbums(root: JSONObject): List<OnlineAlbum> {
        val out = ArrayList<OnlineAlbum>()
        for (item in collectRenderers(root)) {
            val nav = item.optJSONObject("navigationEndpoint") ?: continue
            val browseId = nav.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
            if (!browseId.startsWith("MPRE")) continue
            val title = flexText(item, 0) ?: continue
            val artist = flexText(item, 1, 0) ?: ""
            val audioPlaylistId = item.optJSONObject("menu")
                ?.optJSONObject("menuRenderer")?.optJSONArray("items")
                ?.let { menu ->
                    (0 until menu.length()).firstNotNullOfOrNull { i ->
                        menu.optJSONObject(i)
                            ?.optJSONObject("menuNavigationItemRenderer")
                            ?.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId")
                            ?.takeIf { it.isNotBlank() }
                    }
                }
            out.add(OnlineAlbum(browseId, audioPlaylistId, title, artist, thumbOf(item)))
        }
        return out
    }

    private fun parsePlaylists(root: JSONObject): List<OnlinePlaylist> {
        val out = ArrayList<OnlinePlaylist>()
        for (item in collectRenderers(root)) {
            val nav = item.optJSONObject("navigationEndpoint") ?: continue
            val browseId = nav.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
            // Playlists use VL/PL browse ids
            val playlistId = when {
                browseId.startsWith("VL") -> browseId.removePrefix("VL")
                browseId.startsWith("PL") || browseId.startsWith("RD") -> browseId
                else -> continue
            }
            val title = flexText(item, 0) ?: continue
            val author = flexText(item, 1, 0) ?: ""
            out.add(OnlinePlaylist(playlistId, title, author, thumbOf(item)))
        }
        return out
    }

    private fun parseDuration(text: String): Int {
        if (text.isBlank()) return 0
        val p = text.split(":").mapNotNull { it.toIntOrNull() }
        return when (p.size) {
            2 -> p[0] * 60 + p[1]
            3 -> p[0] * 3600 + p[1] * 60 + p[2]
            else -> 0
        }
    }

    // ─────────────────────────────────────── Playlist / album tracklist ──

    /**
     * Fetch the songs of a playlist/album via the YT-Music `next` (watch queue)
     * endpoint. Works for an audioPlaylistId (album) or a playlistId.
     */
    suspend fun playlistTracks(playlistId: String, max: Int = 60): List<OnlineResult> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("playlistId", playlistId)
                put("context", JSONObject().put("client", JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20240501.01.00")
                    put("hl", "en"); put("gl", "US"); put("utcOffsetMinutes", 0)
                }))
            }.toString()
            val req = Request.Builder()
                .url("$YT_MUSIC_NEXT?prettyPrint=false")
                .post(body.toRequestBody(JSON))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("X-Youtube-Client-Name", "67")
                .header("X-Youtube-Client-Version", "1.20240501.01.00")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .build()
            val json = runCatching {
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext emptyList<OnlineResult>()
                    JSONObject(resp.body!!.string())
                }
            }.getOrNull() ?: return@withContext emptyList()
            parsePlaylistItems(json).take(max)
        }

    /** Parse playlistPanelVideoRenderer entries from a `next` response. */
    private fun parsePlaylistItems(root: JSONObject): List<OnlineResult> {
        val out = ArrayList<OnlineResult>()
        val items = ArrayList<JSONObject>()
        fun walk(o: Any?) {
            when (o) {
                is JSONObject -> {
                    o.optJSONObject("playlistPanelVideoRenderer")?.let { items.add(it) }
                    o.keys().forEach { walk(o.opt(it)) }
                }
                is JSONArray -> for (i in 0 until o.length()) walk(o.opt(i))
            }
        }
        walk(root)
        for (item in items) {
            val videoId = item.optString("videoId").ifBlank {
                item.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId").orEmpty()
            }
            if (videoId.isBlank()) continue
            val title = item.optJSONObject("title")?.optJSONArray("runs")
                ?.optJSONObject(0)?.optString("text") ?: continue
            val artistRuns = item.optJSONObject("longBylineText")?.optJSONArray("runs")
            val artist = artistRuns?.optJSONObject(0)?.optString("text") ?: ""
            val durationText = item.optJSONObject("lengthText")?.optJSONArray("runs")
                ?.optJSONObject(0)?.optString("text") ?: ""
            out.add(
                OnlineResult(
                    videoId = videoId, title = title, author = artist,
                    thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                    durationText = durationText, durationSecs = parseDuration(durationText),
                )
            )
        }
        return out
    }

    // ─────────────────────────────────────────────── Stream resolution ──

    /** Innertube /player clients that return plain audio URLs (2026), ordered by hit-rate. */
    private data class YtClient(val name: String, val version: String, val ua: String, val cn: String)
    private val clients = listOf(
        YtClient("ANDROID_TESTSUITE", "1.9", "com.google.android.youtube/1.9 (Linux; U; Android 14; Pixel 8 Pro) gzip", "30"),
        YtClient("ANDROID_VR", "1.65.10", "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L) gzip", "28"),
        YtClient("ANDROID", "20.10.38", "com.google.android.youtube/20.10.38 (Linux; U; Android 14; Pixel 8 Pro) gzip", "3"),
        YtClient("IOS", "20.10.04", "com.google.ios.youtube/20.10.04 (iPhone16,2; U; CPU iOS 18_2_1 like Mac OS X;)", "5"),
    )

    suspend fun getStream(videoId: String, bypassCache: Boolean = false): ResolvedStream? = withContext(Dispatchers.IO) {
        OnlinePlaybackDebugLog.add("getStream: videoId=$videoId, bypassCache=$bypassCache")
        if (!bypassCache) {
            streamCache[videoId]?.let {
                if (System.currentTimeMillis() - it.at < CACHE_TTL_MS) {
                    OnlinePlaybackDebugLog.add("getStream: cache hit, ageMs=${System.currentTimeMillis() - it.at}")
                    return@withContext it.stream
                }
                OnlinePlaybackDebugLog.add("getStream: cache expired, ageMs=${System.currentTimeMillis() - it.at}")
            }
        } else {
            OnlinePlaybackDebugLog.add("getStream: cache bypassed for foreground playback")
        }
        // Warm the cipher (base.js) once so ciphered formats can be deciphered.
        runCatching {
            OnlinePlaybackDebugLog.add("Cipher warmup: discover player JS")
            YoutubeCipher.discoverPlayerJsUrlCached()?.let {
                OnlinePlaybackDebugLog.add("Cipher warmup: player JS found")
                YoutubeCipher.ensurePlayer(it)
            } ?: OnlinePlaybackDebugLog.add("Cipher warmup: no player JS URL")
        }.onFailure { OnlinePlaybackDebugLog.add("Cipher warmup failed", it) }

        for (c in clients) {
            OnlinePlaybackDebugLog.add("Trying Innertube client ${c.name} ${c.version}")
            val stream = runCatching { tryClient(videoId, c) }
                .onFailure { OnlinePlaybackDebugLog.add("Client ${c.name} exception", it) }
                .getOrNull()
            if (stream != null) {
                OnlinePlaybackDebugLog.add("Client ${c.name} success: host=${runCatching { java.net.URI(stream.url).host }.getOrNull()}")
                streamCache[videoId] = CacheEntry(stream, System.currentTimeMillis())
                return@withContext stream
            }
            OnlinePlaybackDebugLog.add("Client ${c.name} returned no playable audio")
        }
        // Last-ditch: WebView extractor (BotGuard-immune) reads ytInitialPlayerResponse.
        OnlinePlaybackDebugLog.add("Trying WebView extractor fallback")
        val viaWebView = runCatching { YoutubeWebViewExtractor.extract(videoId) }
            .onFailure { OnlinePlaybackDebugLog.add("WebView extractor exception", it) }
            .getOrNull()
        if (viaWebView != null) {
            OnlinePlaybackDebugLog.add("WebView extractor success: host=${runCatching { java.net.URI(viaWebView).host }.getOrNull()}")
            val s = ResolvedStream(viaWebView, clients.first().ua)
            streamCache[videoId] = CacheEntry(s, System.currentTimeMillis())
            return@withContext s
        }
        OnlinePlaybackDebugLog.add("getStream failed: all resolver strategies exhausted")
        null
    }

    private suspend fun tryClient(videoId: String, c: YtClient): ResolvedStream? {
        val body = JSONObject().apply {
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
            put("context", JSONObject().put("client", JSONObject().apply {
                put("clientName", c.name)
                put("clientVersion", c.version)
                put("hl", "en"); put("gl", "US")
            }))
        }.toString()
        val req = Request.Builder()
            .url("$YT_PLAYER?prettyPrint=false")
            .post(body.toRequestBody(JSON))
            .header("User-Agent", c.ua)
            .header("X-Youtube-Client-Name", c.cn)
            .header("X-Youtube-Client-Version", c.version)
            .header("Content-Type", "application/json")
            .build()

        val data = http.newCall(req).execute().use { resp ->
            OnlinePlaybackDebugLog.add("${c.name}: /player HTTP ${resp.code}")
            if (!resp.isSuccessful) return null
            JSONObject(resp.body!!.string())
        }
        val playability = data.optJSONObject("playabilityStatus")
        val status = playability?.optString("status")
        val reason = playability?.optString("reason")
        OnlinePlaybackDebugLog.add("${c.name}: playability status=${status ?: "missing"}${reason?.takeIf { it.isNotBlank() }?.let { ", reason=$it" }.orEmpty()}")
        if (status != null && status != "OK") return null

        val streaming = data.optJSONObject("streamingData") ?: run {
            OnlinePlaybackDebugLog.add("${c.name}: no streamingData")
            return null
        }
        val adaptive = streaming.optJSONArray("adaptiveFormats")
        val formats = streaming.optJSONArray("formats")
        OnlinePlaybackDebugLog.add("${c.name}: formats adaptive=${adaptive?.length() ?: 0}, regular=${formats?.length() ?: 0}")
        val url = bestAudioUrl(adaptive, c.name)
            ?: bestAudioUrl(formats, c.name)
            ?: run {
                OnlinePlaybackDebugLog.add("${c.name}: no audio URL after format scan")
                return null
            }
        return ResolvedStream(url = url, userAgent = c.ua)
    }

    /**
     * Pick the highest-bitrate audio format and resolve it via YoutubeCipher,
     * which handles BOTH plain `url` and ciphered `signatureCipher` formats.
     */
    private suspend fun bestAudioUrl(formats: JSONArray?, clientName: String): String? {
        if (formats == null) return null
        val audio = (0 until formats.length())
            .map { formats.getJSONObject(it) }
            .filter { (it.optString("mimeType") + it.optString("type")).lowercase().contains("audio/") }
            .sortedWith(
                compareByDescending<JSONObject> {
                    // M4A/MP4 is more reliable across OEM ExoPlayer stacks than WebM/Opus.
                    val mime = it.optString("mimeType").lowercase()
                    if (mime.contains("audio/mp4") || mime.contains("mp4a")) 1 else 0
                }.thenByDescending { it.optLong("bitrate").coerceAtLeast(it.optLong("averageBitrate")) }
            )
        OnlinePlaybackDebugLog.add("$clientName: audio candidates=${audio.size}")
        for (f in audio) {
            val itag = f.optString("itag")
            val mime = f.optString("mimeType").take(48)
            val hasUrl = f.has("url")
            val hasCipher = f.has("signatureCipher") || f.has("cipher")
            OnlinePlaybackDebugLog.add("$clientName: candidate itag=$itag, mime='$mime', url=$hasUrl, cipher=$hasCipher")
            val resolved = runCatching { YoutubeCipher.resolveFormatUrl(f) }
                .onFailure { OnlinePlaybackDebugLog.add("$clientName: candidate itag=$itag cipher/url resolve failed", it) }
                .getOrNull()
            if (!resolved.isNullOrBlank()) {
                OnlinePlaybackDebugLog.add("$clientName: candidate itag=$itag resolved")
                return resolved
            }
        }
        return null
    }
}
