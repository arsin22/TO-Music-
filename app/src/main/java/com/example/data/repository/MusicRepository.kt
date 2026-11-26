package com.example.data.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.example.data.local.PlayerPreferences
import com.example.data.local.TrackDao
import com.example.data.model.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class MediaStoreSyncResult(
    val addedCount: Int,
    val removedCount: Int,
    val totalDeviceTracks: Int
)

class MusicRepository(
    private val context: Context,
    private val trackDao: TrackDao,
    private val preferences: PlayerPreferences
) {
    private val tracksDir = File(context.filesDir, "tracks").apply { mkdirs() }
    private val coversDir = File(context.filesDir, "covers").apply { mkdirs() }

    private val audioExtensions = setOf("mp3", "wav", "ogg", "m4a", "aac", "flac", "webm", "opus")
    private val paletteHues = listOf(198, 218, 238, 258, 280, 174)

    val allTracksFlow: Flow<List<TrackEntity>> = trackDao.getAllTracks()

    fun requiredAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            requiredAudioPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun observeMediaStoreChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(Unit)
            }
        }

        val resolver = context.contentResolver
        runCatching {
            resolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        }

        awaitClose {
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }

    suspend fun syncDeviceMediaStore(
        isFa: Boolean,
        includeIgnoredReset: Boolean = false
    ): MediaStoreSyncResult = withContext(Dispatchers.IO) {
        if (!hasAudioPermission()) {
            return@withContext MediaStoreSyncResult(0, 0, 0)
        }

        if (includeIgnoredReset) {
            preferences.clearIgnoredMediaStoreIds()
        }

        val ignoredIds = preferences.getIgnoredMediaStoreIds()
        val existingTracks = trackDao.getAllTracksOnce()
        val existingById = existingTracks.associateBy { it.id }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 3000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val scannedIds = mutableSetOf<String>()
        val toUpsert = mutableListOf<TrackEntity>()
        var addedCount = 0

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idCol)
                val id = "ms_$mediaId"
                if (id in ignoredIds) continue
                scannedIds.add(id)

                val displayName = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
                val rawTitle = if (titleCol >= 0) cursor.getString(titleCol).orEmpty().trim() else ""
                val rawArtist = if (artistCol >= 0) cursor.getString(artistCol).orEmpty().trim() else ""
                val rawAlbum = if (albumCol >= 0) cursor.getString(albumCol).orEmpty().trim() else ""
                val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else 0L
                val durationMs = if (durCol >= 0) cursor.getLong(durCol) else 0L
                val mimeType = if (mimeCol >= 0) cursor.getString(mimeCol).orEmpty() else ""
                val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                val dateAddedSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L

                val fallbackFileName = displayName.ifBlank {
                    (rawTitle.ifBlank { "track_$mediaId" }) + ".mp3"
                }
                val (parsedTitle, parsedArtist) = parseFilenameFallback(fallbackFileName, isFa)

                val cleanTitle = rawTitle
                    .takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                    ?: parsedTitle
                val cleanArtist = rawArtist
                    .takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                    ?: parsedArtist
                val cleanAlbum = rawAlbum
                    .takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                    ?: if (isFa) "حافظه گوشی" else "Device Music"

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    mediaId
                ).toString()

                val albumArtUri = if (albumId > 0L) {
                    ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()
                } else null

                val existing = existingById[id]
                if (existing == null) {
                    addedCount++
                    toUpsert.add(
                        TrackEntity(
                            id = id,
                            title = cleanTitle,
                            artist = cleanArtist,
                            album = cleanAlbum,
                            durationSec = durationMs / 1000.0,
                            fileName = fallbackFileName,
                            mimeType = guessMimeType(fallbackFileName, mimeType),
                            sizeBytes = sizeBytes,
                            addedAt = if (dateAddedSec > 0L) dateAddedSec * 1000L else System.currentTimeMillis(),
                            audioFilePath = contentUri,
                            coverFilePath = albumArtUri,
                            accentHue = hashHue("$cleanTitle-$cleanArtist"),
                            isDemo = false
                        )
                    )
                } else {
                    val resolvedCover = existing.coverFilePath ?: albumArtUri
                    val resolvedDur = if (durationMs > 0L) durationMs / 1000.0 else existing.durationSec
                    if (existing.audioFilePath != contentUri ||
                        existing.coverFilePath != resolvedCover ||
                        existing.sizeBytes != sizeBytes ||
                        abs(existing.durationSec - resolvedDur) > 0.5
                    ) {
                        toUpsert.add(
                            existing.copy(
                                audioFilePath = contentUri,
                                coverFilePath = resolvedCover,
                                sizeBytes = sizeBytes,
                                durationSec = resolvedDur,
                                mimeType = guessMimeType(fallbackFileName, mimeType)
                            )
                        )
                    }
                }
            }
        }

        // Remove any previously scanned MediaStore items that were deleted from device storage
        val missingIds = existingTracks
            .filter { it.id.startsWith("ms_") && it.id !in scannedIds }
            .map { it.id }

        // If device has real songs, remove untouched demo starter tracks so real library takes precedence
        val demoIdsToClean = if (scannedIds.isNotEmpty()) {
            existingTracks
                .filter { it.isDemo && !it.isFavorite && it.playCount == 0 }
                .map { it.id }
        } else emptyList()

        val allDeletes = (missingIds + demoIdsToClean).distinct()
        if (allDeletes.isNotEmpty()) {
            trackDao.deleteTracksByIds(allDeletes)
        }
        if (toUpsert.isNotEmpty()) {
            trackDao.insertTracks(toUpsert)
            preferences.setSeeded(true)
        }

        MediaStoreSyncResult(
            addedCount = addedCount,
            removedCount = missingIds.size,
            totalDeviceTracks = scannedIds.size
        )
    }

    suspend fun bootstrapIfNeeded(isFa: Boolean): List<TrackEntity> = withContext(Dispatchers.IO) {
        if (hasAudioPermission()) {
            syncDeviceMediaStore(isFa)
        }
        val existing = trackDao.getAllTracksOnce()
        val alreadySeeded = preferences.isSeeded()
        if (existing.isEmpty() && !alreadySeeded) {
            val demoTracks = createStarterDemoTracks(isFa)
            trackDao.insertTracks(demoTracks)
            preferences.setSeeded(true)
            return@withContext demoTracks
        }
        existing
    }

    suspend fun loadStarterTracks(isFa: Boolean): List<TrackEntity> = withContext(Dispatchers.IO) {
        val existingIds = trackDao.getAllTracksOnce().map { it.id }.toSet()
        val starters = createStarterDemoTracks(isFa).filter { it.id !in existingIds }
        if (starters.isNotEmpty()) {
            trackDao.insertTracks(starters)
        }
        preferences.setSeeded(true)
        starters
    }

    suspend fun importFromUris(uris: List<Uri>, isFa: Boolean): List<TrackEntity> = withContext(Dispatchers.IO) {
        val imported = mutableListOf<TrackEntity>()
        var offset = 0L

        for (uri in uris) {
            val displayName = resolveDisplayName(uri) ?: "track_${System.currentTimeMillis()}.mp3"
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val ext = displayName.substringAfterLast('.', "").lowercase()

            if (ext == "zip" || mimeType == "application/zip" || mimeType == "application/x-zip-compressed") {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        ZipInputStream(stream.buffered()).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                val entryName = entry.name
                                val cleanName = entryName.substringAfterLast('/')
                                val entryExt = cleanName.substringAfterLast('.', "").lowercase()
                                if (!entry.isDirectory &&
                                    !entryName.startsWith("__MACOSX/") &&
                                    entryExt in audioExtensions
                                ) {
                                    val id = generateTrackId(offset)
                                    val outFile = File(tracksDir, "$id.$entryExt")
                                    FileOutputStream(outFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                    val entity = buildTrackFromFile(
                                        id = id,
                                        fileName = cleanName,
                                        audioFile = outFile,
                                        mimeType = guessMimeType(cleanName, null),
                                        indexOffset = offset++,
                                        isFa = isFa
                                    )
                                    imported.add(entity)
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }
                    }
                }
                continue
            }

            if (mimeType.startsWith("audio/") || ext in audioExtensions) {
                runCatching {
                    val safeExt = if (ext.isNotBlank()) ext else "mp3"
                    val id = generateTrackId(offset)
                    val outFile = File(tracksDir, "$id.$safeExt")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: return@runCatching
                    val entity = buildTrackFromFile(
                        id = id,
                        fileName = displayName,
                        audioFile = outFile,
                        mimeType = guessMimeType(displayName, mimeType),
                        indexOffset = offset++,
                        isFa = isFa
                    )
                    imported.add(entity)
                }
            }
        }

        if (imported.isNotEmpty()) {
            trackDao.insertTracks(imported)
            preferences.setSeeded(true)
        }
        imported
    }

    suspend fun deleteTracks(ids: List<String>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val targets = trackDao.getTracksByIds(ids)
        trackDao.deleteTracksByIds(ids)

        val msIds = targets.filter { it.id.startsWith("ms_") }.map { it.id }
        if (msIds.isNotEmpty()) {
            preferences.addIgnoredMediaStoreIds(msIds)
        }

        for (track in targets) {
            if (!track.audioFilePath.startsWith("content://") && !track.audioFilePath.startsWith("file://")) {
                runCatching { File(track.audioFilePath).delete() }
            }
            track.coverFilePath?.let { path ->
                if (!path.startsWith("content://") && !path.startsWith("file://")) {
                    runCatching { File(path).delete() }
                }
            }
        }
    }

    suspend fun toggleFavorite(id: String, nextFavorite: Boolean) = withContext(Dispatchers.IO) {
        trackDao.updateFavorite(id, nextFavorite)
    }

    suspend fun updateMetadata(id: String, title: String, artist: String, album: String) =
        withContext(Dispatchers.IO) {
            trackDao.updateMetadata(id, title, artist, album)
        }

    suspend fun recordPlayback(id: String, playedAt: Long, playCount: Int) =
        withContext(Dispatchers.IO) {
            trackDao.updatePlayStats(id, playedAt, playCount)
        }

    suspend fun saveLyrics(id: String, plainLyrics: String?, syncedLyrics: String?) =
        withContext(Dispatchers.IO) {
            trackDao.updateLyrics(id, plainLyrics, syncedLyrics)
        }

    private fun buildTrackFromFile(
        id: String,
        fileName: String,
        audioFile: File,
        mimeType: String,
        indexOffset: Long,
        isFa: Boolean
    ): TrackEntity {
        val retriever = MediaMetadataRetriever()
        var metaTitle: String? = null
        var metaArtist: String? = null
        var metaAlbum: String? = null
        var durationSec = 0.0
        var coverPath: String? = null

        try {
            retriever.setDataSource(audioFile.absolutePath)
            metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()?.takeIf { it.isNotEmpty() }
            metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()?.takeIf { it.isNotEmpty() }
            metaAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.trim()?.takeIf { it.isNotEmpty() }
            val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            durationSec = durMs / 1000.0

            val pictureBytes = retriever.embeddedPicture
            if (pictureBytes != null && pictureBytes.size in 16..3_000_000) {
                val coverFile = File(coversDir, "$id.jpg")
                coverFile.writeBytes(pictureBytes)
                coverPath = coverFile.absolutePath
            }
        } catch (_: Throwable) {
            // Fallback cleanly to filename parsing
        } finally {
            runCatching { retriever.release() }
        }

        val (fallbackTitle, fallbackArtist) = parseFilenameFallback(fileName, isFa)
        val title = metaTitle ?: fallbackTitle
        val artist = metaArtist ?: fallbackArtist
        val album = metaAlbum ?: if (isFa) "افزوده‌شده از دستگاه" else "Added from Device"

        return TrackEntity(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationSec = durationSec,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = audioFile.length(),
            addedAt = System.currentTimeMillis() + indexOffset,
            audioFilePath = audioFile.absolutePath,
            coverFilePath = coverPath,
            accentHue = hashHue("$title-$artist"),
            isDemo = false
        )
    }

    private fun parseFilenameFallback(fileName: String, isFa: Boolean): Pair<String, String> {
        val withoutExt = fileName.substringBeforeLast('.', fileName).trim()
        val parts = withoutExt.split(" - ")
        return if (parts.size >= 2) {
            val artist = parts[0].trim().ifEmpty { if (isFa) "هنرمند ناشناس" else "Unknown Artist" }
            val title = parts.drop(1).joinToString(" - ").trim().ifEmpty { withoutExt }
            title to artist
        } else {
            val title = withoutExt.ifEmpty { if (isFa) "قطعه بدون عنوان" else "Untitled Track" }
            title to (if (isFa) "کتابخانه شخصی" else "My Library")
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun guessMimeType(fileName: String, fallback: String?): String {
        if (!fallback.isNullOrBlank() && fallback.startsWith("audio/")) return fallback
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "webm" -> "audio/webm"
            "opus" -> "audio/opus"
            else -> "audio/mpeg"
        }
    }

    private fun hashHue(seed: String): Int {
        var hash = 0
        for (ch in seed) {
            hash = (hash * 31 + ch.code) % 360
        }
        return paletteHues[abs(hash) % paletteHues.size]
    }

    private fun generateTrackId(offset: Long): String {
        val shortRand = UUID.randomUUID().toString().take(6)
        return "trk_${System.currentTimeMillis()}_${offset}_$shortRand"
    }

    private data class DemoSpec(
        val id: String,
        val titleFa: String,
        val titleEn: String,
        val artistFa: String,
        val artistEn: String,
        val albumFa: String,
        val albumEn: String,
        val durationSec: Int,
        val freqs: DoubleArray,
        val pulseHz: Double,
        val hue: Int
    )

    private fun createStarterDemoTracks(isFa: Boolean): List<TrackEntity> {
        val now = System.currentTimeMillis()
        val specs = listOf(
            DemoSpec(
                id = "demo_midnight_glass",
                titleFa = "شب شیشه‌ای",
                titleEn = "Midnight Glass",
                artistFa = "آوای نیمه‌شب",
                artistEn = "Midnight Echo",
                albumFa = "مجموعه نمونه",
                albumEn = "Starter Collection",
                durationSec = 18,
                freqs = doubleArrayOf(220.0, 277.18, 329.63, 440.0),
                pulseHz = 0.45,
                hue = 218
            ),
            DemoSpec(
                id = "demo_rain_horizon",
                titleFa = "افق بارانی",
                titleEn = "Rain Horizon",
                artistFa = "صدای آرامش",
                artistEn = "Calm Tide",
                albumFa = "مجموعه نمونه",
                albumEn = "Starter Collection",
                durationSec = 20,
                freqs = doubleArrayOf(196.0, 246.94, 293.66, 392.0),
                pulseHz = 0.35,
                hue = 198
            ),
            DemoSpec(
                id = "demo_indigo_drift",
                titleFa = "موج نیلی",
                titleEn = "Indigo Drift",
                artistFa = "نوای دیجیتال",
                artistEn = "Digital Pulse",
                albumFa = "مجموعه نمونه",
                albumEn = "Starter Collection",
                durationSec = 16,
                freqs = doubleArrayOf(261.63, 329.63, 392.0, 493.88),
                pulseHz = 0.55,
                hue = 258
            ),
            DemoSpec(
                id = "demo_dawn_chord",
                titleFa = "طلوع آکوستیک",
                titleEn = "Acoustic Dawn",
                artistFa = "آوای صبح",
                artistEn = "Morning Breeze",
                albumFa = "مجموعه نمونه",
                albumEn = "Starter Collection",
                durationSec = 22,
                freqs = doubleArrayOf(174.61, 220.0, 261.63, 349.23),
                pulseHz = 0.30,
                hue = 174
            )
        )

        return specs.mapIndexed { idx, spec ->
            val file = File(tracksDir, "${spec.id}.wav")
            if (!file.exists() || file.length() < 100) {
                val wavBytes = synthesizeAmbientWav(spec.freqs, spec.durationSec, spec.pulseHz)
                file.writeBytes(wavBytes)
            }
            val title = if (isFa) spec.titleFa else spec.titleEn
            TrackEntity(
                id = spec.id,
                title = title,
                artist = if (isFa) spec.artistFa else spec.artistEn,
                album = if (isFa) spec.albumFa else spec.albumEn,
                durationSec = spec.durationSec.toDouble(),
                fileName = "$title.wav",
                mimeType = "audio/wav",
                sizeBytes = file.length(),
                addedAt = now - idx * 1000L,
                audioFilePath = file.absolutePath,
                coverFilePath = null,
                accentHue = spec.hue,
                isDemo = true
            )
        }
    }

    private fun synthesizeAmbientWav(
        frequencies: DoubleArray,
        durationSec: Int,
        pulseHz: Double
    ): ByteArray {
        val sampleRate = 22050
        val totalSamples = sampleRate * durationSec
        val dataSize = totalSamples * 2
        val out = ByteArrayOutputStream(44 + dataSize)

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1)
        header.putShort(1)
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)
        out.write(header.array())

        val pcmBuffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val fadeIn = min(1.0, t / 0.8)
            val fadeOut = min(1.0, (durationSec - t) / 1.2)
            val envelope = fadeIn * fadeOut
            val lfo = 0.65 + 0.35 * sin(2.0 * PI * pulseHz * t)

            var sample = 0.0
            for (fIdx in frequencies.indices) {
                val freq = frequencies[fIdx]
                val weight = 1.0 / (fIdx + 1.4)
                sample += sin(2.0 * PI * freq * t) * weight
                sample += sin(2.0 * PI * (freq * 1.003) * t) * weight * 0.35
            }
            val normalized = max(-1.0, min(1.0, (sample / frequencies.size) * envelope * lfo * 0.45))
            pcmBuffer.putShort((normalized * 32767.0).roundToInt().toShort())
        }
        out.write(pcmBuffer.array())
        return out.toByteArray()
    }
}
