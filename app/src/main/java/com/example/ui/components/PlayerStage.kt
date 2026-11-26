package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EqPreset
import com.example.data.model.RepeatMode
import com.example.data.model.SyncedLyricLine
import com.example.data.model.TrackEntity
import com.example.data.model.formatDuration
import com.example.data.repository.LyricsRepository
import com.example.ui.PlaybackProgress
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalStrings
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun PlayerStage(
    activeTrack: TrackEntity?,
    isPlaying: Boolean,
    progressFlow: StateFlow<PlaybackProgress>,
    volume: Float,
    shuffle: Boolean,
    repeatMode: RepeatMode,
    sleepMinutesLeft: Int?,
    eqPreset: EqPreset,
    playbackRate: Float,
    upNextTracks: List<TrackEntity>,
    manualQueueCount: Int,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Double) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onSelectTrack: (TrackEntity) -> Unit,
    onClearManualQueue: () -> Unit,
    onJumpToActiveInList: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenRingtoneMaker: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenEditTrack: () -> Unit,
    isFullScreen: Boolean = false,
    onCloseMobileSheet: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val s = LocalStrings.current

    if (activeTrack == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .then(
                    if (isFullScreen) {
                        Modifier
                            .background(colors.sheetBackground)
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(26.dp))
                            .background(colors.glassSurface)
                            .border(1.dp, colors.glassBorderStrong, RoundedCornerShape(26.dp))
                    }
                )
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(colors.glassSurfaceElevated)
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(26.dp))
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = s.readyToPlayTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textStrong
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = s.readyToPlayDesc,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = colors.textMuted,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val parsedLyrics = remember(activeTrack.syncedLyrics) {
        activeTrack.syncedLyrics?.let { LyricsRepository.parseLrcLines(it) } ?: emptyList()
    }

    var showSleepMenu by remember { mutableStateOf(false) }
    val accentA = remember(activeTrack.accentHue, colors.isDark) {
        Color.hsl(
            activeTrack.accentHue.toFloat(),
            if (colors.isDark) 0.85f else 0.78f,
            if (colors.isDark) 0.58f else 0.48f
        )
    }
    val accentB = remember(activeTrack.accentHue, colors.isDark) {
        Color.hsl(
            ((activeTrack.accentHue + 35) % 360).toFloat(),
            0.88f,
            if (colors.isDark) 0.66f else 0.52f
        )
    }

    var headerDragDy by remember { mutableFloatStateOf(0f) }

    val containerModifier = if (isFullScreen) {
        modifier
            .fillMaxSize()
            .background(colors.sheetBackground)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentA.copy(alpha = if (isPlaying) 0.30f else 0.18f),
                        colors.sheetBackground.copy(alpha = 0.92f),
                        colors.sheetBackground
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    } else {
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.glassSurface)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentA.copy(alpha = if (isPlaying) 0.20f else 0.10f),
                        Color.Transparent
                    )
                )
            )
            .border(1.dp, colors.glassBorderStrong, RoundedCornerShape(26.dp))
    }

    Column(
        modifier = containerModifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = if (isFullScreen) 10.dp else 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Navigation Bar (supports swipe-down to dismiss full-screen player)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onCloseMobileSheet != null) {
                        Modifier.pointerInput(onCloseMobileSheet) {
                            detectVerticalDragGestures(
                                onDragStart = { headerDragDy = 0f },
                                onVerticalDrag = { _, dragAmount -> headerDragDy += dragAmount },
                                onDragEnd = {
                                    if (headerDragDy > 70f) onCloseMobileSheet()
                                    headerDragDy = 0f
                                }
                            )
                        }
                    } else Modifier
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCloseMobileSheet != null) {
                IconButton(
                    onClick = onCloseMobileSheet,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("close_player_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = s.closePlayerSheetDesc,
                        tint = colors.textStrong,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Text(
                    text = s.nowPlaying,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(CircleShape)
                        .background(colors.glassSurfaceElevated)
                        .border(1.dp, colors.glassBorder, CircleShape)
                        .clickable {
                            onJumpToActiveInList()
                            onCloseMobileSheet?.invoke()
                        }
                        .padding(horizontal = 12.dp)
                        .testTag("show_in_list_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = null,
                        tint = colors.accentSkyText,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = s.showInList,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clip(CircleShape)
                            .background(
                                if (sleepMinutesLeft != null) colors.accentSky.copy(alpha = 0.2f)
                                else colors.glassSurfaceElevated
                            )
                            .border(
                                1.dp,
                                if (sleepMinutesLeft != null) colors.accentSky.copy(alpha = 0.45f) else colors.glassBorder,
                                CircleShape
                            )
                            .clickable { showSleepMenu = true }
                            .padding(horizontal = 12.dp)
                            .testTag("sleep_timer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = if (sleepMinutesLeft != null) colors.accentSkySoft else colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (sleepMinutesLeft != null) s.minutesLeft(sleepMinutesLeft) else s.sleepTimer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (sleepMinutesLeft != null) colors.accentSkySoft else colors.textSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = showSleepMenu,
                        onDismissRequest = { showSleepMenu = false },
                        modifier = Modifier.background(colors.modalBackground)
                    ) {
                        val options = listOf(
                            s.sleepOff to null,
                            s.sleepMinutesOption(15) to 15,
                            s.sleepMinutesOption(30) to 30,
                            s.sleepMinutesOption(45) to 45,
                            s.sleepMinutesOption(60) to 60
                        )
                        options.forEach { (label, mins) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 12.sp, color = colors.textPrimary) },
                                onClick = {
                                    onSetSleepTimer(mins)
                                    showSleepMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isFullScreen) 24.dp else 18.dp))

        // Glowing Artwork (radial gradient instead of GPU-heavy Modifier.blur for locked 120fps)
        val artScale by animateFloatAsState(
            targetValue = if (isPlaying) 1f else 0.94f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "artwork_scale"
        )
        var dragAccum by remember { mutableFloatStateOf(0f) }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .pointerInput(activeTrack.id) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                        onDragEnd = {
                            if (dragAccum < -100f) onNext()
                            else if (dragAccum > 100f) onPrev()
                            dragAccum = 0f
                        }
                    )
                }
        ) {
            if (isPlaying) {
                val glowTransition = rememberInfiniteTransition(label = "art_glow")
                val glowAlpha by glowTransition.animateFloat(
                    initialValue = if (colors.isDark) 0.28f else 0.22f,
                    targetValue = if (colors.isDark) 0.48f else 0.36f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200),
                        repeatMode = AnimRepeatMode.Reverse
                    ),
                    label = "glow_alpha"
                )
                Box(
                    modifier = Modifier
                        .size(292.dp)
                        .alpha(glowAlpha)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentA.copy(alpha = 0.72f),
                                    accentA.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            AnimatedContent(
                targetState = activeTrack,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(160)) },
                label = "artwork_switch"
            ) { track ->
                TrackArtwork(
                    title = track.title,
                    coverFilePath = track.coverFilePath,
                    accentHue = track.accentHue,
                    size = ArtworkSize.LG,
                    isPlaying = isPlaying,
                    modifier = Modifier.scale(artScale)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        LiveVisualizer(
            isPlaying = isPlaying,
            accentHue = activeTrack.accentHue,
            playbackRate = playbackRate
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title, Artist, Favorite
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeTrack.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(activeTrack.artist)
                        if (activeTrack.album.isNotBlank()) append(" · ${activeTrack.album}")
                    },
                    fontSize = 13.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = { onToggleFavorite(activeTrack.id) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (activeTrack.isFavorite) colors.accentRose.copy(alpha = 0.16f) else colors.glassSurfaceElevated)
                    .testTag("player_favorite_button")
            ) {
                Icon(
                    imageVector = if (activeTrack.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (activeTrack.isFavorite) s.removeFromFavorites else s.addToFavorites,
                    tint = if (activeTrack.isFavorite) colors.accentRoseText else colors.textMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Live Lyric Preview Pill (scoped progress collection)
        if (parsedLyrics.isNotEmpty()) {
            LiveLyricPreviewBanner(
                parsedLyrics = parsedLyrics,
                progressFlow = progressFlow,
                onClick = onOpenLyrics
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Seek Bar (scoped progress collection — zero parent recomposition!)
        SeekBarSection(
            fallbackDurationSec = activeTrack.durationSec,
            progressFlow = progressFlow,
            accentA = accentA,
            accentB = accentB,
            onSeek = onSeek
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Transport Controls (LTR)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (shuffle) colors.accentSky.copy(alpha = 0.18f) else Color.Transparent)
                        .testTag("shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = s.shuffleDesc,
                        tint = if (shuffle) colors.accentSkyText else colors.textMuted,
                        modifier = Modifier.size(21.dp)
                    )
                }

                IconButton(
                    onClick = onPrev,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(colors.glassSurfaceElevated)
                        .testTag("prev_track_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = s.prevTrackDesc,
                        tint = colors.textStrong,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(colors.primaryBtnBg)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) s.pauseDesc else s.playDesc,
                        tint = colors.primaryBtnFg,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(colors.glassSurfaceElevated)
                        .testTag("next_track_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = s.nextTrackDesc,
                        tint = colors.textStrong,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (repeatMode != RepeatMode.OFF) colors.accentSky.copy(alpha = 0.18f) else Color.Transparent)
                        .testTag("repeat_button")
                ) {
                    Icon(
                        imageVector = if (repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = s.repeatDesc,
                        tint = if (repeatMode != RepeatMode.OFF) colors.accentSkyText else colors.textMuted,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4 Quick Tool Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val hasLyrics = !activeTrack.syncedLyrics.isNullOrBlank() || !activeTrack.plainLyrics.isNullOrBlank()
            val eqActive = eqPreset != EqPreset.FLAT || kotlin.math.abs(playbackRate - 1f) > 0.01f

            QuickToolButton(
                label = s.lyrics,
                icon = Icons.Rounded.Description,
                highlighted = hasLyrics,
                highlightColor = colors.accentSky,
                textColor = if (hasLyrics) colors.accentSkySoft else colors.textSecondary,
                onClick = onOpenLyrics,
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_lyrics_button")
            )
            QuickToolButton(
                label = s.equalizer,
                icon = Icons.Rounded.Tune,
                highlighted = eqActive,
                highlightColor = colors.accentIndigo,
                textColor = if (eqActive) colors.accentIndigoText else colors.textSecondary,
                onClick = onOpenEqualizer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_eq_button")
            )
            QuickToolButton(
                label = s.ringtoneAndAlarm,
                icon = Icons.Rounded.NotificationsActive,
                highlighted = false,
                highlightColor = colors.accentSky,
                textColor = colors.textSecondary,
                onClick = onOpenRingtoneMaker,
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_ringtone_button")
            )
            QuickToolButton(
                label = s.edit,
                icon = Icons.Rounded.Edit,
                highlighted = false,
                highlightColor = colors.accentSky,
                textColor = colors.textSecondary,
                onClick = onOpenEditTrack,
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_edit_track_button")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Volume Control (LTR)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onVolumeChange(if (volume > 0f) 0f else 0.85f) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (volume <= 0.01f) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = s.muteUnmuteDesc,
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                SleekSlider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    thumbColor = colors.accentSky,
                    activeTrackColor = colors.accentSky,
                    inactiveTrackColor = colors.sliderInactive,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${(volume * 100).roundToInt()}%",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(38.dp)
                )
            }
        }

        // Up Next Queue
        if (upNextTracks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = colors.glassBorder)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (manualQueueCount > 0) s.customQueueTitle else s.upNextTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (manualQueueCount > 0) {
                        Text(
                            text = s.clearQueue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.accentSkyText,
                            modifier = Modifier.clickable { onClearManualQueue() }
                        )
                    }
                    Text(
                        text = s.tracksCount(upNextTracks.size),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                upNextTracks.take(5).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassSurfaceElevated)
                            .clickable { onSelectTrack(item) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.artist,
                                fontSize = 11.sp,
                                color = colors.textSubtle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = formatDuration(item.durationSec),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = colors.textSubtle
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveLyricPreviewBanner(
    parsedLyrics: List<SyncedLyricLine>,
    progressFlow: StateFlow<PlaybackProgress>,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val progress by progressFlow.collectAsStateWithLifecycle()
    val currentTimeSec = progress.currentTimeSec
    val liveLyricLine = remember(parsedLyrics, currentTimeSec) {
        if (parsedLyrics.isEmpty()) return@remember null
        var current = parsedLyrics.first().text
        for (line in parsedLyrics) {
            if (line.timeSec <= currentTimeSec + 0.05) current = line.text
            else break
        }
        current
    }
    if (!liveLyricLine.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.glassSurfaceElevated)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("live_lyric_preview_pill")
        ) {
            Text(
                text = liveLyricLine,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.accentSkyText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SeekBarSection(
    fallbackDurationSec: Double,
    progressFlow: StateFlow<PlaybackProgress>,
    accentA: Color,
    accentB: Color,
    onSeek: (Double) -> Unit
) {
    val colors = LocalAppColors.current
    val progress by progressFlow.collectAsStateWithLifecycle()
    val effectiveDuration = if (progress.durationSec > 0.0) progress.durationSec else fallbackDurationSec
    val currentTimeSec = progress.currentTimeSec

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
        ) {
            SleekSlider(
                value = currentTimeSec.toFloat().coerceIn(0f, max(1f, effectiveDuration.toFloat())),
                onValueChange = { onSeek(it.toDouble()) },
                valueRange = 0f..max(1f, effectiveDuration.toFloat()),
                thumbColor = accentB,
                activeTrackColor = accentA,
                inactiveTrackColor = colors.sliderInactive,
                modifier = Modifier.testTag("player_seek_slider")
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentTimeSec),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
                Text(
                    text = formatDuration(effectiveDuration),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleekSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    thumbColor: Color,
    activeTrackColor: Color,
    inactiveTrackColor: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        interactionSource = interactionSource,
        thumb = {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        },
        track = { sliderState ->
            val span = (sliderState.valueRange.endInclusive - sliderState.valueRange.start).coerceAtLeast(1e-4f)
            val fraction = ((sliderState.value - sliderState.valueRange.start) / span).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(inactiveTrackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(activeTrackColor)
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun QuickToolButton(
    label: String,
    icon: ImageVector,
    highlighted: Boolean,
    highlightColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(shape)
            .background(if (highlighted) highlightColor.copy(alpha = 0.16f) else colors.glassSurfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MobileMiniPlayer(
    activeTrack: TrackEntity,
    isPlaying: Boolean,
    progressFlow: StateFlow<PlaybackProgress>,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val s = LocalStrings.current
    val accent = remember(activeTrack.accentHue, colors.isDark) {
        Color.hsl(
            activeTrack.accentHue.toFloat(),
            0.85f,
            if (colors.isDark) 0.64f else 0.48f
        )
    }
    var dragDy by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.modalBackground)
            .navigationBarsPadding()
            .pointerInput(activeTrack.id) {
                detectVerticalDragGestures(
                    onDragStart = { dragDy = 0f },
                    onVerticalDrag = { _, dragAmount -> dragDy += dragAmount },
                    onDragEnd = {
                        if (dragDy < -45f) onExpand()
                        dragDy = 0f
                    }
                )
            }
            .testTag("mobile_mini_player")
    ) {
        // Hairline top progress bar (scoped so parent rows never recompose)
        MiniProgressLine(
            fallbackDurationSec = activeTrack.durationSec,
            progressFlow = progressFlow,
            accent = accent,
            inactiveColor = colors.sliderInactive
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .clickable(onClick = onExpand)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TrackArtwork(
                    title = activeTrack.title,
                    coverFilePath = activeTrack.coverFilePath,
                    accentHue = activeTrack.accentHue,
                    size = ArtworkSize.SM,
                    isPlaying = isPlaying
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = activeTrack.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textStrong,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        MiniEqualizer(isPlaying = isPlaying, accentHue = activeTrack.accentHue)
                    }
                    Text(
                        text = activeTrack.artist,
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.primaryBtnBg)
                            .testTag("mini_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) s.pauseDesc else s.playDesc,
                            tint = colors.primaryBtnFg,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .testTag("mini_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = s.nextShortDesc,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniProgressLine(
    fallbackDurationSec: Double,
    progressFlow: StateFlow<PlaybackProgress>,
    accent: Color,
    inactiveColor: Color
) {
    val progressState by progressFlow.collectAsStateWithLifecycle()
    val effectiveDuration = if (progressState.durationSec > 0.0) progressState.durationSec else fallbackDurationSec
    val ratio = if (effectiveDuration > 0.0) {
        (progressState.currentTimeSec / effectiveDuration).toFloat().coerceIn(0f, 1f)
    } else 0f

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(inactiveColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(2.5.dp)
                    .background(accent)
            )
        }
    }
}
