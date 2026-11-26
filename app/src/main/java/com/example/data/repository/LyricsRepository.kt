package com.example.data.repository

import com.example.data.model.LyricSearchResult
import com.example.data.model.LyricWordTiming
import com.example.data.model.SyncedLyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.pow

object LyricsRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val timeRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val wordTimeRegex = Regex("""<(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?>""")

    fun parseLrcLines(lrcText: String): List<SyncedLyricLine> {
        if (lrcText.isBlank()) return emptyList()
        val parsed = mutableListOf<SyncedLyricLine>()
        for (rawLine in lrcText.lineSequence()) {
            val matches = timeRegex.findAll(rawLine).toList()
            if (matches.isEmpty()) continue
            val rawWithoutLineTags = rawLine.replace(timeRegex, "").trim()
            if (rawWithoutLineTags.isEmpty()) continue

            val wordTimings = parseEnhancedWordTimings(rawWithoutLineTags)
            val cleanText = rawWithoutLineTags.replace(wordTimeRegex, "").replace(Regex("""\s{2,}"""), " ").trim()
            if (cleanText.isEmpty()) continue

            for (m in matches) {
                val timeSec = parseTimestamp(m.groupValues[1], m.groupValues[2], m.groupValues[3]) ?: continue
                parsed.add(
                    SyncedLyricLine(
                        timeSec = timeSec,
                        text = cleanText,
                        wordTimings = wordTimings
                    )
                )
            }
        }
        parsed.sortBy { it.timeSec }
        return parsed
    }

    private fun parseEnhancedWordTimings(lineBody: String): List<LyricWordTiming> {
        val wordMatches = wordTimeRegex.findAll(lineBody).toList()
        if (wordMatches.isEmpty()) return emptyList()

        val result = mutableListOf<LyricWordTiming>()
        for (i in wordMatches.indices) {
            val match = wordMatches[i]
            val t = parseTimestamp(match.groupValues[1], match.groupValues[2], match.groupValues[3]) ?: continue
            val textStart = match.range.last + 1
            val textEnd = if (i + 1 < wordMatches.size) wordMatches[i + 1].range.first else lineBody.length
            val segment = lineBody.substring(textStart, textEnd).trim()
            if (segment.isNotEmpty()) {
                result.add(LyricWordTiming(timeSec = t, text = segment))
            }
        }
        return result
    }

    private fun parseTimestamp(minsStr: String, secsStr: String, fracStr: String): Double? {
        val mins = minsStr.toIntOrNull() ?: return null
        val secs = secsStr.toIntOrNull() ?: return null
        val frac = if (fracStr.isNotEmpty()) {
            (fracStr.toDoubleOrNull() ?: 0.0) / 10.0.pow(fracStr.length)
        } else {
            0.0
        }
        val timeSec = mins * 60.0 + secs + frac
        return if (timeSec.isFinite()) timeSec else null
    }

    suspend fun searchOnlineLyrics(
        queryTitle: String,
        queryArtist: String?
    ): List<LyricSearchResult> = withContext(Dispatchers.IO) {
        val cleanTitle = queryTitle.trim()
        val cleanArtist = queryArtist?.trim().orEmpty()
        if (cleanTitle.isEmpty()) return@withContext emptyList()

        val useStrict = cleanTitle.isNotEmpty() &&
            cleanArtist.isNotEmpty() &&
            cleanArtist != "کتابخانه شخصی" &&
            cleanArtist != "هنرمند ناشناس"

        val primaryUrl = if (useStrict) {
            "https://lrclib.net/api/search?track_name=${encode(cleanTitle)}&artist_name=${encode(cleanArtist)}"
        } else {
            "https://lrclib.net/api/search?q=${encode("$cleanTitle $cleanArtist".trim())}"
        }

        val primaryResults = executeSearch(primaryUrl)
        if (primaryResults.isNotEmpty() || !useStrict) {
            return@withContext primaryResults
        }

        val fallbackUrl = "https://lrclib.net/api/search?q=${encode("$cleanTitle $cleanArtist".trim())}"
        executeSearch(fallbackUrl)
    }

    private fun executeSearch(urlStr: String): List<LyricSearchResult> {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "TO-Music-Android/1.0")
        }

        return try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("پاسخی از سرویس آنلاین متن آهنگ دریافت نشد.")
            }
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val items = json.decodeFromString<List<LyricSearchResult>>(body)
            items.filter { !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank() }
        } finally {
            conn.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
