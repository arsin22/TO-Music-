package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.RingtoneTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object RingtoneExporter {

    suspend fun trimToCacheWav(
        context: Context,
        sourcePath: String,
        startSec: Double,
        endSec: Double,
        fadeEdges: Boolean,
        outputFileName: String
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "ringtones").apply { mkdirs() }
        val outFile = File(dir, outputFileName)

        val pcmResult = decodeToPcm16(context, sourcePath, startSec, endSec)
        writeWavWithFade(
            outFile = outFile,
            pcmBytes = pcmResult.pcmBytes,
            sampleRate = pcmResult.sampleRate,
            channels = pcmResult.channels,
            fadeEdges = fadeEdges
        )
        outFile
    }

    suspend fun saveToMediaStore(
        context: Context,
        wavFile: File,
        displayName: String,
        target: RingtoneTarget
    ): Uri = withContext(Dispatchers.IO) {
        val relativeDir = when (target) {
            RingtoneTarget.RINGTONE -> Environment.DIRECTORY_RINGTONES
            RingtoneTarget.ALARM -> Environment.DIRECTORY_ALARMS
            RingtoneTarget.NOTIFICATION -> Environment.DIRECTORY_NOTIFICATIONS
        }

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.TITLE, displayName.substringBeforeLast('.'))
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.IS_RINGTONE, target == RingtoneTarget.RINGTONE)
            put(MediaStore.Audio.Media.IS_ALARM, target == RingtoneTarget.ALARM)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, target == RingtoneTarget.NOTIFICATION)
            put(MediaStore.Audio.Media.IS_MUSIC, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativeDir)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(collection, values)
            ?: throw IllegalStateException("ذخیره فایل در حافظه رسانه ممکن نشد.")

        try {
            resolver.openOutputStream(itemUri)?.use { out ->
                wavFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: throw IllegalStateException("خطا در نوشتن فایل خروجی.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val doneValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                resolver.update(itemUri, doneValues, null, null)
            }
            itemUri
        } catch (t: Throwable) {
            runCatching { resolver.delete(itemUri, null, null) }
            throw t
        }
    }

    fun getShareUri(context: Context, wavFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            wavFile
        )
    }

    private data class DecodedPcm(
        val pcmBytes: ByteArray,
        val sampleRate: Int,
        val channels: Int
    )

    private fun decodeToPcm16(
        context: Context,
        sourcePath: String,
        startSec: Double,
        endSec: Double
    ): DecodedPcm {
        val extractor = MediaExtractor()
        if (sourcePath.startsWith("content://") || sourcePath.startsWith("file://")) {
            extractor.setDataSource(context, Uri.parse(sourcePath), null)
        } else {
            extractor.setDataSource(sourcePath)
        }

        var audioTrackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                format = trackFormat
                break
            }
        }

        if (audioTrackIndex < 0 || format == null) {
            extractor.release()
            throw IllegalStateException("جریان صوتی معتبری در فایل پیدا نشد.")
        }

        extractor.selectTrack(audioTrackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        var sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } else 44100
        var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else 1

        val clampedStartUs = max(0L, (startSec * 1_000_000.0).toLong())
        val clampedEndUs = max(clampedStartUs + 500_000L, (endSec * 1_000_000.0).toLong())

        extractor.seekTo(clampedStartUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        // Fast path if raw PCM WAV
        if (mime == MediaFormat.MIMETYPE_AUDIO_RAW) {
            val out = ByteArrayOutputStream()
            val buf = ByteBuffer.allocate(65536)
            while (true) {
                val sampleTime = extractor.sampleTime
                if (sampleTime < 0 || sampleTime > clampedEndUs) break
                val size = extractor.readSampleData(buf, 0)
                if (size < 0) break
                if (sampleTime >= clampedStartUs) {
                    val arr = ByteArray(size)
                    buf.position(0)
                    buf.get(arr)
                    out.write(arr)
                }
                buf.clear()
                extractor.advance()
            }
            extractor.release()
            return DecodedPcm(out.toByteArray(), sampleRate, channels)
        }

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmOut = ByteArrayOutputStream()
        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false

        try {
            while (!sawOutputEos) {
                if (!sawInputEos) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)!!
                        val sampleTime = extractor.sampleTime
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0 || sampleTime > clampedEndUs + 200_000L) {
                            codec.queueInputBuffer(
                                inIdx,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, sampleSize, sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFormat = codec.outputFormat
                        if (outFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (outFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                    }
                    outIdx >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 0) {
                            val pts = bufferInfo.presentationTimeUs
                            if (pts in clampedStartUs..clampedEndUs) {
                                outBuf.position(bufferInfo.offset)
                                outBuf.limit(bufferInfo.offset + bufferInfo.size)
                                val chunk = ByteArray(bufferInfo.size)
                                outBuf.get(chunk)
                                pcmOut.write(chunk)
                            }
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            sawOutputEos = true
                        }
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
            runCatching { extractor.release() }
        }

        val bytes = pcmOut.toByteArray()
        if (bytes.isEmpty()) {
            throw IllegalStateException("بازه انتخابی قابل برش نبود.")
        }
        return DecodedPcm(bytes, sampleRate, channels)
    }

    private fun writeWavWithFade(
        outFile: File,
        pcmBytes: ByteArray,
        sampleRate: Int,
        channels: Int,
        fadeEdges: Boolean
    ) {
        val ch = max(1, min(2, channels))
        val bytesPerSample = 2
        val blockAlign = ch * bytesPerSample
        val frameCount = pcmBytes.size / blockAlign
        val alignedSize = frameCount * blockAlign

        val processed = pcmBytes.copyOf(alignedSize)
        if (fadeEdges && frameCount > 10) {
            val fadeSamples = min((sampleRate * 0.6).toInt(), (frameCount * 0.15).toInt())
            if (fadeSamples > 0) {
                val bb = ByteBuffer.wrap(processed).order(ByteOrder.LITTLE_ENDIAN)
                for (frame in 0 until frameCount) {
                    val gain = when {
                        frame < fadeSamples -> frame.toDouble() / fadeSamples
                        frame > frameCount - fadeSamples -> (frameCount - frame).toDouble() / fadeSamples
                        else -> 1.0
                    }
                    if (gain < 0.999) {
                        for (c in 0 until ch) {
                            val byteIdx = (frame * ch + c) * 2
                            val raw = bb.getShort(byteIdx).toDouble()
                            val scaled = (raw * gain).roundToInt().coerceIn(-32768, 32767).toShort()
                            bb.putShort(byteIdx, scaled)
                        }
                    }
                }
            }
        }

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + alignedSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1)
        header.putShort(ch.toShort())
        header.putInt(sampleRate)
        header.putInt(sampleRate * blockAlign)
        header.putShort(blockAlign.toShort())
        header.putShort(16)
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(alignedSize)

        FileOutputStream(outFile).use { fos ->
            fos.write(header.array())
            fos.write(processed)
        }
    }
}
