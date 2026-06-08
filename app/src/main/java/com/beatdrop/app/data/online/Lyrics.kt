package com.beatdrop.app.data.online

/** One timed lyric line. */
data class LyricLine(val timeMs: Long, val text: String)

/** Parser for LRC `[mm:ss.xx]` timestamped lyrics. */
object LrcParser {
    private val tag = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")

    /**
     * Look for a sidecar ".lrc" file next to a local audio file (same stem) and
     * parse it. Returns empty if there's no path / no file / unreadable.
     */
    fun findSidecar(filePath: String?): List<LyricLine> {
        if (filePath.isNullOrBlank()) return emptyList()
        val dot = filePath.lastIndexOf('.')
        if (dot <= 0) return emptyList()
        val lrc = java.io.File(filePath.substring(0, dot) + ".lrc")
        if (!lrc.exists() || !lrc.canRead()) return emptyList()
        return runCatching { parse(lrc.readText()) }.getOrDefault(emptyList())
    }

    fun parse(content: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        content.lineSequence().forEach { raw ->
            val matches = tag.findAll(raw).toList()
            if (matches.isEmpty()) return@forEach
            val text = raw.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val f = m.groupValues[3]
                val frac = when (f.length) {
                    0 -> 0L; 1 -> f.toLong() * 100; 2 -> f.toLong() * 10; else -> f.take(3).toLong()
                }
                out.add(LyricLine(min * 60_000 + sec * 1000 + frac, text))
            }
        }
        return out.sortedBy { it.timeMs }
    }

    /** Index of the line to highlight for [posMs]; -1 if before the first line. O(log n). */
    fun activeIndex(lines: List<LyricLine>, posMs: Long): Int {
        if (lines.isEmpty() || posMs < lines.first().timeMs) return -1
        var lo = 0; var hi = lines.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= posMs) lo = mid + 1 else hi = mid - 1
        }
        return hi
    }
}
