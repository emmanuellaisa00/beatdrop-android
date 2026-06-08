package com.beatdrop.app.data.online

import android.content.Context
import com.beatdrop.app.data.model.MediaSource
import com.beatdrop.app.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class DownloadStatus { IDLE, QUEUED, DOWNLOADING, COMPLETED, FAILED }

data class DownloadJob(
    val trackId: String,
    val status: DownloadStatus,
    val percent: Int = 0,
)

/**
 * Singleton download manager — streams an online track's resolved audio to the
 * app's files dir with progress, then registers it as an offline LOCAL track.
 * Mirrors the approach in github.com/emmanuellaisa00/beatdroppremium (a long-lived
 * scope + per-track progress StateFlow), trimmed to our needs.
 */
object DownloadManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // no read-timeout ceiling for large files
        .build()

    private val _jobs = MutableStateFlow<Map<String, DownloadJob>>(emptyMap())
    val jobs: StateFlow<Map<String, DownloadJob>> = _jobs

    private val _downloaded = MutableStateFlow<List<Track>>(emptyList())
    val downloaded: StateFlow<List<Track>> = _downloaded

    fun init(context: Context) {
        if (_downloaded.value.isEmpty()) _downloaded.value = loadIndex(context)
    }

    fun jobFor(trackId: String): DownloadJob? = _jobs.value[trackId]
    fun isDownloaded(trackId: String): Boolean = _downloaded.value.any { it.id == trackId }

    fun enqueue(track: Track, context: Context) {
        if (track.source != MediaSource.ONLINE) return
        val id = track.id
        if (isDownloaded(id)) return
        val existing = _jobs.value[id]
        if (existing?.status == DownloadStatus.QUEUED || existing?.status == DownloadStatus.DOWNLOADING) return

        // Keep the process alive while downloading (survives backgrounding).
        runCatching { com.beatdrop.app.player.DownloadService.start(context.applicationContext) }

        update(DownloadJob(id, DownloadStatus.QUEUED))
        val job = scope.launch {
            update(DownloadJob(id, DownloadStatus.DOWNLOADING, 0))
            val ok = runCatching { download(track, context) }.getOrDefault(false)
            if (ok) update(DownloadJob(id, DownloadStatus.COMPLETED, 100))
            else update(DownloadJob(id, DownloadStatus.FAILED))
            activeJobs.remove(id)
        }
        activeJobs[id] = job
    }

    fun cancel(trackId: String) {
        activeJobs.remove(trackId)?.cancel()
        update(DownloadJob(trackId, DownloadStatus.IDLE))
    }

    /** Remove a completed download: delete the file, drop it from the index. */
    fun delete(trackId: String, context: Context) {
        val track = _downloaded.value.firstOrNull { it.id == trackId } ?: return
        runCatching {
            track.uri.path?.let { File(it).takeIf(File::exists)?.delete() }
        }
        val next = _downloaded.value.filterNot { it.id == trackId }
        _downloaded.value = next
        saveIndex(context, next)
        _jobs.value = _jobs.value.toMutableMap().apply { remove(trackId) }
    }

    /** Total bytes used by completed downloads on disk. */
    fun usageBytes(context: Context): Long {
        val dir = File(context.filesDir, "downloads")
        if (!dir.exists()) return 0L
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private suspend fun download(track: Track, context: Context): Boolean {
        val videoId = track.onlineId ?: return false
        val stream = YoutubeService.getStream(videoId) ?: return false

        val dir = File(context.filesDir, "downloads").apply { mkdirs() }
        val outFile = File(dir, "$videoId.m4a")

        val req = Request.Builder().url(stream.url)
            .header("User-Agent", stream.userAgent)
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return false
            val body = resp.body ?: return false
            val total = body.contentLength()
            outFile.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read)
                        done += read
                        if (total > 0) {
                            update(DownloadJob(track.id, DownloadStatus.DOWNLOADING, ((done * 100) / total).toInt()))
                        }
                    }
                }
            }
        }

        // Register as an offline LOCAL track pointing at the saved file.
        val offline = track.copy(
            uri = android.net.Uri.fromFile(outFile),
            source = MediaSource.LOCAL,
            onlineId = null,
            filePath = outFile.absolutePath,
        )
        val next = listOf(offline) + _downloaded.value.filterNot { it.id == offline.id }
        _downloaded.value = next
        saveIndex(context, next)
        return true
    }

    private fun update(job: DownloadJob) {
        _jobs.value = _jobs.value.toMutableMap().apply { put(job.trackId, job) }
    }

    // ── Persistent index of completed downloads (SharedPreferences/JSON) ──
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("beatdrop_downloads", Context.MODE_PRIVATE)

    private fun saveIndex(context: Context, list: List<Track>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id); put("title", t.title); put("artist", t.artist)
                put("album", t.album); put("durationMs", t.durationMs)
                put("uri", t.uri.toString()); put("artworkUri", t.artworkUri?.toString())
                put("colorKey", t.colorKey)
            })
        }
        prefs(context).edit().putString("index", arr.toString()).apply()
    }

    private fun loadIndex(context: Context): List<Track> {
        val raw = prefs(context).getString("index", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Track(
                    id = o.getString("id"),
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    album = o.optString("album"),
                    durationMs = o.optLong("durationMs"),
                    uri = android.net.Uri.parse(o.optString("uri")),
                    artworkUri = o.optString("artworkUri", null)?.let(android.net.Uri::parse),
                    colorKey = o.optString("colorKey", null),
                    source = MediaSource.LOCAL,
                )
            }
        }.getOrDefault(emptyList())
    }
}
