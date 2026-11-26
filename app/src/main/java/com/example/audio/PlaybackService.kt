package com.example.audio

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.Equalizer
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.data.local.PlayerPreferences
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var latestGainsDb: IntArray = intArrayOf(0, 0, 0, 0, 0)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        const val ACTION_SET_EQ = "com.example.action.SET_EQ"
        const val EXTRA_EQ_GAINS = "extra_eq_gains"
        private val TARGET_BAND_FREQS_HZ = intArrayOf(60, 230, 910, 3600, 14000)
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer = player

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                attachEqualizer(audioSessionId)
            }
        })

        serviceScope.launch {
            val prefs = PlayerPreferences(applicationContext).eqSettingsFlow.first()
            latestGainsDb = prefs.gainsDb.toIntArray()
            player.volume = prefs.volume
            player.setPlaybackSpeed(prefs.playbackRate)
            if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                attachEqualizer(player.audioSessionId)
            }
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val eqSessionCommand = SessionCommand(ACTION_SET_EQ, Bundle.EMPTY)

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .add(eqSessionCommand)
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction == ACTION_SET_EQ) {
                        val gains = args.getIntArray(EXTRA_EQ_GAINS)
                        if (gains != null && gains.size == 5) {
                            latestGainsDb = gains.copyOf()
                            applyEqGains(latestGainsDb)
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            })
            .build()
    }

    private fun attachEqualizer(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return
        runCatching {
            equalizer?.release()
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            equalizer = eq
            applyEqGains(latestGainsDb)
        }
    }

    private fun applyEqGains(gainsDb: IntArray) {
        val eq = equalizer ?: return
        runCatching {
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minLevel = range[0].toInt()
            val maxLevel = range[1].toInt()

            for (bandIdx in 0 until numBands) {
                val band = bandIdx.toShort()
                val centerHz = eq.getCenterFreq(band) / 1000
                // Find closest target band among [60, 230, 910, 3600, 14000]
                var bestTarget = 0
                var bestDist = Int.MAX_VALUE
                for (i in TARGET_BAND_FREQS_HZ.indices) {
                    val dist = abs(centerHz - TARGET_BAND_FREQS_HZ[i])
                    if (dist < bestDist) {
                        bestDist = dist
                        bestTarget = i
                    }
                }
                val millibels = (gainsDb.getOrElse(bestTarget) { 0 } * 100)
                    .coerceIn(minLevel, maxLevel)
                    .toShort()
                eq.setBandLevel(band, millibels)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        runCatching { equalizer?.release() }
        equalizer = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }
}
