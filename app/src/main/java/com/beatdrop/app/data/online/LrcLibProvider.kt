package com.beatdrop.app.data.online

import com.beatdrop.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Online lyrics from LRCLIB (https://lrclib.net) — free, open, no API key.
 * Ported from github.com/emmanuellaisa00/beatdroppremium: exact-match first,
 * then search fallbacks; prefers synced lyrics, falls back to timed-plain.
 */
object LrcLibProvider {

    private const val BASE = "https://lrclib.net/api"
    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    /** Distinguishes "fetched, has timed lyrics", "fetched, plain only" and "none". */
    data class Result(val lines: List<LyricLine>, val synced: Boolean)

    private val cache = ConcurrentHashMap<String, Result>()

    suspend fun fetch(track: Track): Result = withContext(Dispatchers.IO) {
        val key = track.id
        cache[key]?.let { return@withContext it }

        val durationSec = (track.durationMs / 1000).toInt()

        // 1) Exact match
        runCatching {
            val url = "$BASE/get?track_name=${enc(track.title)}&artist_name=${enc(track.artist)}" +
                "&album_name=${enc(track.album)}" +
                if (durationSec > 0) "&duration=$durationSec" else ""
            getObject(url)?.let { fromObject(it, durationSec) }?.let {
                cache[key] = it; return@withContext it
            }
        }

        val cleanTitle = track.title
            .replace(Regex("""\s*[(\[].*?[)\]]"""), "")
            .replace(Regex("""\s*-\s*(official|audio|video|lyric|music).*""", RegexOption.IGNORE_CASE), "")
            .trim()

        // 2) Search by title + artist
        runCatching {
            val arr = getArray("$BASE/search?track_name=${enc(cleanTitle)}&artist_name=${enc(track.artist)}")
            pickBest(arr, durationSec)?.let { fromObject(it, durationSec) }?.let {
                cache[key] = it; return@withContext it
            }
        }

        // 3) Broad query
        runCatching {
            val arr = getArray("$BASE/search?q=${enc(track.artist + " " + cleanTitle)}")
            pickBest(arr, durationSec)?.let { fromObject(it, durationSec) }?.let {
                cache[key] = it; return@withContext it
            }
        }

        val empty = Result(emptyList(), false)
        cache[key] = empty
        empty
    }

    private fun pickBest(arr: JSONArray?, durationSec: Int): JSONObject? {
        if (arr == null || arr.length() == 0) return null
        var best: JSONObject? = null
        var bestScore = Long.MAX_VALUE
        var bestHasSynced = false
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val hasSynced = !o.optString("syncedLyrics").isNullOrBlank()
            val dur = o.optInt("duration", 0)
            val score = if (durationSec > 0) abs(dur - durationSec).toLong() else 0L
            val better = when {
                hasSynced && !bestHasSynced -> true
                hasSynced == bestHasSynced -> score < bestScore
                else -> false
            }
            if (best == null || better) { best = o; bestScore = score; bestHasSynced = hasSynced }
        }
        return best
    }

    private fun fromObject(o: JSONObject, durationSec: Int): Result? {
        val synced = o.optString("syncedLyrics")
        if (!synced.isNullOrBlank()) {
            val lines = LrcParser.parse(synced)
            if (lines.isNotEmpty()) return Result(lines, synced = true)
        }
        val plain = o.optString("plainLyrics")
        if (!plain.isNullOrBlank()) {
            val lines = plain.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isNotEmpty()) return Result(plainToTimed(lines, if (durationSec > 0) durationSec else 180), synced = false)
        }
        return null
    }

    /** Convert plain lines to weighted timed lines so they still scroll. */
    private fun plainToTimed(lines: List<String>, durationSec: Int): List<LyricLine> {
        val dur = maxOf(durationSec.toDouble(), lines.size * 1.5)
        val intro = minOf(dur * 0.05, 8.0)
        val outro = minOf(dur * 0.04, 6.0)
        val avail = dur - intro - outro
        val minHold = 1.2
        val weights = lines.map { maxOf(0.5, minOf(2.5, it.length / 30.0)) }
        val total = weights.sum().takeIf { it > 0 } ?: 1.0
        val out = ArrayList<LyricLine>()
        var cursor = intro
        for (i in lines.indices) {
            out.add(LyricLine((cursor * 1000).toLong(), lines[i]))
            cursor += maxOf(minHold, (weights[i] / total) * avail)
        }
        return out
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun httpGet(url: String): String? = runCatching {
        val req = Request.Builder().url(url)
            .header("User-Agent", "BeatDrop/1.0 (Android; +https://lrclib.net)")
            .build()
        http.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
    }.getOrNull()

    private fun getObject(url: String) = httpGet(url)?.let { runCatching { JSONObject(it) }.getOrNull() }
    private fun getArray(url: String) = httpGet(url)?.let { runCatching { JSONArray(it) }.getOrNull() }
}
