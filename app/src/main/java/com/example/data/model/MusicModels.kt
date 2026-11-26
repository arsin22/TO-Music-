package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class AppThemeMode {
    DARK,
    LIGHT
}

enum class AppLanguage {
    SYSTEM,
    FA,
    EN
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class SortMode(val labelFa: String, val labelEn: String) {
    DATE_DESC("جدیدترین", "Newest"),
    TITLE_ASC("نام آهنگ", "Title"),
    ARTIST_ASC("هنرمند", "Artist"),
    DURATION_DESC("طولانی‌ترین", "Duration");

    fun label(isFa: Boolean): String = if (isFa) labelFa else labelEn
}

enum class EqPreset(val labelFa: String, val labelEn: String, val gainsDb: List<Int>) {
    FLAT("عادی", "Flat", listOf(0, 0, 0, 0, 0)),
    BASS_BOOST("بیس قوی", "Bass Boost", listOf(7, 5, 0, 1, 2)),
    POP("پاپ", "Pop", listOf(-1, 2, 4, 3, -1)),
    ROCK("راک", "Rock", listOf(5, 2, -1, 3, 5)),
    VOCAL("وکال شفاف", "Vocal", listOf(-3, -1, 5, 4, 1)),
    ACOUSTIC("آکوستیک", "Acoustic", listOf(3, 2, 1, 3, 4)),
    CUSTOM("دستی", "Custom", listOf(0, 0, 0, 0, 0));

    fun label(isFa: Boolean): String = if (isFa) labelFa else labelEn
}

enum class LibraryTab(val labelFa: String, val labelEn: String) {
    ALL("آهنگ‌ها", "Songs"),
    FAVORITES("علاقه‌مندی", "Favorites"),
    ALBUMS("آلبوم‌ها", "Albums"),
    ARTISTS("هنرمندان", "Artists"),
    RECENT("پخش اخیر", "Recent");

    fun label(isFa: Boolean): String = if (isFa) labelFa else labelEn
}

enum class RingtoneTarget(
    val labelFa: String,
    val labelEn: String,
    val maxDurationSec: Int,
    val prefix: String
) {
    RINGTONE("آهنگ زنگ", "Ringtone", 30, "ringtone"),
    ALARM("آلارم بیداری", "Alarm", 20, "alarm"),
    NOTIFICATION("صدای اعلان", "Notification", 5, "notification");

    fun label(isFa: Boolean): String = if (isFa) labelFa else labelEn
}

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Double,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val addedAt: Long,
    val audioFilePath: String,
    val coverFilePath: String? = null,
    val accentHue: Int = 218,
    val isDemo: Boolean = false,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long? = null,
    val playCount: Int = 0,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

data class AlbumGroup(
    val name: String,
    val artistSummary: String,
    val tracks: List<TrackEntity>,
    val totalDurationSec: Double,
    val coverFilePath: String?,
    val accentHue: Int
)

data class ArtistGroup(
    val name: String,
    val tracks: List<TrackEntity>,
    val albumCount: Int,
    val totalDurationSec: Double,
    val coverFilePath: String?,
    val accentHue: Int
)

@Serializable
data class LyricSearchResult(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String? = null,
    val duration: Double? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

data class LyricWordTiming(
    val timeSec: Double,
    val text: String
)

data class SyncedLyricLine(
    val timeSec: Double,
    val text: String,
    val wordTimings: List<LyricWordTiming> = emptyList()
)

data class EqBandInfo(
    val freqLabel: String,
    val descFa: String,
    val descEn: String
) {
    fun desc(isFa: Boolean): String = if (isFa) descFa else descEn
}

val EQ_BANDS = listOf(
    EqBandInfo("60Hz", "بیس عمیق", "Sub Bass"),
    EqBandInfo("230Hz", "گرمای صدا", "Warmth"),
    EqBandInfo("910Hz", "وکال", "Vocals"),
    EqBandInfo("3.6kHz", "وضوح", "Presence"),
    EqBandInfo("14kHz", "درخشش", "Brilliance")
)

fun formatDuration(seconds: Double): String {
    if (!seconds.isFinite() || seconds <= 0.0) return "0:00"
    val totalSec = seconds.toInt()
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) {
        "%d:%02d:%02d".format(hrs, mins, secs)
    } else {
        "%d:%02d".format(mins, secs)
    }
}
