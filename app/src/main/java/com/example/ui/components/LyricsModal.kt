package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LyricSearchResult
import com.example.data.model.SyncedLyricLine
import com.example.data.model.TrackEntity
import com.example.data.model.formatDuration
import com.example.data.repository.LyricsRepository
import com.example.ui.PlaybackProgress
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo600
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

private val rtlRegex = Regex("""[\u0600-\u06FF]""")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsModal(
    track: TrackEntity,
    progressFlow: StateFlow<PlaybackProgress>,
    isPlaying: Boolean,
    playbackRate: Float,
    onTogglePlay: () -> Unit,
    onSeek: (Double) -> Unit,
    onSaveLyrics: (trackId: String, plainLyrics: String?, syncedLyrics: String?) -> Unit,
    onClose: () -> Unit
) {
    val progress by progressFlow.collectAsStateWithLifecycle()
    val currentTimeSec = progress.currentTimeSec
    val durationSec = progress.durationSec
    val colors = LocalAppColors.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val defaultArtist = remember(track.id, track.artist) {
        if (track.artist in setOf("کتابخانه شخصی", "هنرمند ناشناس", "My Library", "Unknown Artist")) "" else track.artist
    }

    var queryTitle by remember(track.id) { mutableStateOf(track.title) }
    var queryArtist by remember(track.id) { mutableStateOf(defaultArtist) }
    var results by remember(track.id) { mutableStateOf<List<LyricSearchResult>>(emptyList()) }
    var isSearching by remember(track.id) { mutableStateOf(false) }
    var searchError by remember(track.id) { mutableStateOf<String?>(null) }
    var isEditingManual by remember(track.id) { mutableStateOf(false) }
    var showSearchBar by remember(track.id) {
        mutableStateOf(track.syncedLyrics.isNullOrBlank() && track.plainLyrics.isNullOrBlank())
    }
    var manualText by remember(track.id, track.syncedLyrics, track.plainLyrics) {
        mutableStateOf(track.syncedLyrics ?: track.plainLyrics ?: "")
    }
    var savedNotice by remember(track.id) { mutableStateOf(false) }
    var syncOffsetSec by remember(track.id) { mutableDoubleStateOf(0.0) }

    // 60fps high-precision time interpolation anchored to ExoPlayer position + playbackRate
    var anchorAudioSec by remember { mutableDoubleStateOf(currentTimeSec) }
    var anchorSystemNanos by remember { mutableLongStateOf(System.nanoTime()) }
    var smoothTimeSec by remember { mutableDoubleStateOf(currentTimeSec) }

    LaunchedEffect(currentTimeSec) {
        anchorAudioSec = currentTimeSec
        anchorSystemNanos = System.nanoTime()
        if (!isPlaying) {
            smoothTimeSec = currentTimeSec
        }
    }

    LaunchedEffect(isPlaying, playbackRate) {
        if (!isPlaying) {
            smoothTimeSec = anchorAudioSec
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos {
                val elapsedSec = ((System.nanoTime() - anchorSystemNanos) / 1_000_000_000.0)
                    .coerceIn(0.0, 0.35)
                smoothTimeSec = anchorAudioSec + elapsedSec * playbackRate
            }
        }
    }

    val syncedLines = remember(track.syncedLyrics) {
        track.syncedLyrics?.let { LyricsRepository.parseLrcLines(it) }.orEmpty()
    }

    val effectiveTime = max(0.0, smoothTimeSec + syncOffsetSec)
    val activeLineIndex = remember(syncedLines, effectiveTime) {
        if (syncedLines.isEmpty()) -1
        else {
            var idx = -1
            for (i in syncedLines.indices) {
                if (syncedLines[i].timeSec <= effectiveTime + 0.04) {
                    idx = i
                } else {
                    break
                }
            }
            idx
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && syncedLines.isNotEmpty() && !isEditingManual) {
            val layoutInfo = listState.layoutInfo
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            val targetOffset = if (viewportHeight > 0) -(viewportHeight / 3) else -220
            listState.animateScrollToItem(
                index = activeLineIndex,
                scrollOffset = targetOffset
            )
        }
    }

    fun triggerSearch() {
        if (queryTitle.isBlank()) return
        scope.launch {
            isSearching = true
            searchError = null
            results = emptyList()
            try {
                val found = LyricsRepository.searchOnlineLyrics(queryTitle, queryArtist)
                if (found.isEmpty()) {
                    searchError = s.lyricsNotFoundOnline
                } else {
                    results = found.take(6)
                }
            } catch (t: Throwable) {
                searchError = t.message ?: s.lyricsOnlineError
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(track.id) {
        if (track.plainLyrics.isNullOrBlank() && track.syncedLyrics.isNullOrBlank() && !track.isDemo) {
            triggerSearch()
        }
    }

    fun flashSaved() {
        scope.launch {
            savedNotice = true
            delay(1800L)
            savedNotice = false
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accentColor = Color.hsl(
        track.accentHue.toFloat(),
        if (colors.isDark) 0.82f else 0.76f,
        if (colors.isDark) 0.56f else 0.46f
    )
    val accentBright = Color.hsl(
        ((track.accentHue + 24) % 360).toFloat(),
        0.88f,
        if (colors.isDark) 0.72f else 0.52f
    )

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = colors.sheetBackground,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isPlaying) 0.24f else 0.14f),
                            colors.sheetBackground
                        ),
                        radius = 900f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.glassSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TrackArtwork(
                            title = track.title,
                            coverFilePath = track.coverFilePath,
                            accentHue = track.accentHue,
                            size = ArtworkSize.SM,
                            isPlaying = isPlaying
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textStrong,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append(track.artist)
                                    if (syncedLines.isNotEmpty()) append(" · ${s.liveSyncBadge}")
                                },
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (savedNotice) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = s.savedDesc,
                                tint = colors.accentEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (showSearchBar) colors.accentSky.copy(alpha = 0.2f) else colors.glassSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (showSearchBar) colors.accentSky.copy(alpha = 0.45f) else colors.glassBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { showSearchBar = !showSearchBar }
                                .padding(horizontal = 10.dp)
                                .testTag("lyrics_search_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = s.searchOnlineLyricsDesc,
                                tint = if (showSearchBar) colors.accentSkyText else colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isEditingManual) colors.accentIndigo.copy(alpha = 0.22f) else colors.glassSurfaceElevated)
                                .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                                .clickable { isEditingManual = !isEditingManual }
                                .padding(horizontal = 10.dp)
                                .testTag("lyrics_edit_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = if (isEditingManual) s.viewModeDesc else s.editModeDesc,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = s.close,
                                tint = colors.textMuted
                            )
                        }
                    }
                }

                HorizontalDivider(color = colors.glassBorder)

                // Collapsible Search Bar
                AnimatedVisibility(visible = showSearchBar) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.glassInput)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentSky.copy(alpha = 0.65f),
                            unfocusedBorderColor = colors.glassBorder,
                            focusedContainerColor = colors.glassSurface,
                            unfocusedContainerColor = colors.glassSurface,
                            focusedTextColor = colors.textStrong,
                            unfocusedTextColor = colors.textStrong
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = queryTitle,
                                onValueChange = { queryTitle = it },
                                placeholder = { Text(s.songNameHint, fontSize = 12.sp, color = colors.textSubtle) },
                                singleLine = true,
                                colors = fieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("lyrics_query_title")
                            )
                            OutlinedTextField(
                                value = queryArtist,
                                onValueChange = { queryArtist = it },
                                placeholder = { Text(s.artistOptionalHint, fontSize = 12.sp, color = colors.textSubtle) },
                                singleLine = true,
                                colors = fieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("lyrics_query_artist")
                            )
                        }
                        Button(
                            onClick = { triggerSearch() },
                            enabled = !isSearching && queryTitle.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .testTag("lyrics_fetch_online_button")
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSearching) s.searching else s.fetchOnline,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isEditingManual) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OutlinedTextField(
                                value = manualText,
                                onValueChange = { manualText = it },
                                placeholder = {
                                    Text(
                                        s.manualLyricsHint,
                                        fontSize = 13.sp,
                                        color = colors.textSubtle
                                    )
                                },
                                minLines = 10,
                                maxLines = 16,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accentSky.copy(alpha = 0.6f),
                                    unfocusedBorderColor = colors.glassBorder,
                                    focusedContainerColor = colors.glassSurface,
                                    unfocusedContainerColor = colors.glassSurface,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lyrics_manual_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onSaveLyrics(track.id, null, null)
                                        manualText = ""
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = null,
                                        tint = colors.accentRedText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(s.clearLyrics, fontSize = 12.sp, color = colors.accentRedText)
                                }

                                Button(
                                    onClick = {
                                        val trimmed = manualText.trim()
                                        val hasTimestamps = Regex("""\[\d{1,2}:\d{2}""").containsMatchIn(trimmed)
                                        onSaveLyrics(
                                            track.id,
                                            if (hasTimestamps) null else trimmed.ifEmpty { null },
                                            if (hasTimestamps) trimmed else null
                                        )
                                        isEditingManual = false
                                        showSearchBar = false
                                        flashSaved()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .testTag("lyrics_save_manual_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(s.saveOfflineLyrics, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (results.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(colors.accentIndigo.copy(alpha = 0.12f))
                                            .border(1.dp, colors.accentIndigo.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = s.onlineResultsHint,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.accentIndigoText
                                            )
                                            Text(
                                                text = s.close,
                                                fontSize = 12.sp,
                                                color = colors.textMuted,
                                                modifier = Modifier.clickable { results = emptyList() }
                                            )
                                        }

                                        results.forEach { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(colors.glassSurfaceElevated)
                                                    .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        onSaveLyrics(track.id, item.plainLyrics, item.syncedLyrics)
                                                        manualText = item.syncedLyrics ?: item.plainLyrics ?: ""
                                                        results = emptyList()
                                                        showSearchBar = false
                                                        flashSaved()
                                                    }
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.trackName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = colors.textStrong,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = buildString {
                                                            append(item.artistName)
                                                            if (!item.albumName.isNullOrBlank()) append(" · ${item.albumName}")
                                                            append(if (!item.syncedLyrics.isNullOrBlank()) " · ${s.syncedTag}" else " · ${s.plainTag}")
                                                        },
                                                        fontSize = 11.sp,
                                                        color = colors.textMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Indigo600)
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(s.select, fontSize = 11.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            searchError?.let { err ->
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.accentAmber.copy(alpha = 0.12f))
                                            .border(1.dp, colors.accentAmber.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = err,
                                            fontSize = 12.sp,
                                            lineHeight = 20.sp,
                                            color = colors.accentAmberText
                                        )
                                    }
                                }
                            }

                            if (syncedLines.isNotEmpty()) {
                                itemsIndexed(syncedLines, key = { idx, line -> "${line.timeSec}_$idx" }) { idx, line ->
                                    SyncedLyricLineItem(
                                        line = line,
                                        nextLineTimeSec = syncedLines.getOrNull(idx + 1)?.timeSec,
                                        effectiveTime = effectiveTime,
                                        isCurrent = idx == activeLineIndex,
                                        isPast = activeLineIndex >= 0 && idx < activeLineIndex,
                                        distance = if (activeLineIndex >= 0) abs(idx - activeLineIndex) else 0,
                                        hasActiveLine = activeLineIndex >= 0,
                                        accentColor = accentColor,
                                        accentBright = accentBright,
                                        onSeekToLine = { onSeek(max(0.0, line.timeSec - syncOffsetSec)) }
                                    )
                                }
                            } else if (!track.plainLyrics.isNullOrBlank()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(colors.glassSurface)
                                            .border(1.dp, colors.glassBorder, RoundedCornerShape(22.dp))
                                            .padding(20.dp)
                                    ) {
                                        Text(
                                            text = track.plainLyrics,
                                            fontSize = 15.sp,
                                            lineHeight = 30.sp,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            } else {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp, horizontal = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = colors.textSubtle,
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = s.noLyricsYetTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = s.noLyricsYetDesc,
                                            fontSize = 12.sp,
                                            lineHeight = 20.sp,
                                            color = colors.textSubtle,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OutlinedButton(
                                            onClick = {
                                                showSearchBar = true
                                                triggerSearch()
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.heightIn(min = 44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Search,
                                                contentDescription = null,
                                                tint = colors.textStrong,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(s.searchOnlineLyricsBtn, fontSize = 12.sp, color = colors.textStrong)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Sync Offset & Playback Scrubber
                HorizontalDivider(color = colors.glassBorder)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.glassSurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (syncedLines.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = s.syncOffsetHint,
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.glassSurfaceElevated)
                                            .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                syncOffsetSec = round((syncOffsetSec - 0.5) * 10.0) / 10.0
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text("-0.5s", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = colors.textSecondary)
                                    }
                                    Text(
                                        text = if (syncOffsetSec > 0) "+${"%.1f".format(syncOffsetSec)}s" else "${"%.1f".format(syncOffsetSec)}s",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = colors.accentSkyText,
                                        modifier = Modifier
                                            .clickable { syncOffsetSec = 0.0 }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.glassSurfaceElevated)
                                            .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                syncOffsetSec = round((syncOffsetSec + 0.5) * 10.0) / 10.0
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text("+0.5s", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = colors.textSecondary)
                                    }
                                }
                            }
                        }
                    }

                    val effectiveDuration = if (durationSec > 0.0) durationSec else track.durationSec
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onTogglePlay,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryBtnBg)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) s.pauseDesc else s.playDesc,
                                    tint = colors.primaryBtnFg,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = formatDuration(smoothTimeSec),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )

                            Slider(
                                value = smoothTimeSec.toFloat().coerceIn(0f, max(1f, effectiveDuration.toFloat())),
                                onValueChange = { onSeek(it.toDouble()) },
                                valueRange = 0f..max(1f, effectiveDuration.toFloat()),
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.primaryBtnBg,
                                    activeTrackColor = accentColor,
                                    inactiveTrackColor = colors.sliderInactive
                                ),
                                modifier = Modifier.weight(1f)
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
        }
    }
}

@Composable
private fun SyncedLyricLineItem(
    line: SyncedLyricLine,
    nextLineTimeSec: Double?,
    effectiveTime: Double,
    isCurrent: Boolean,
    isPast: Boolean,
    distance: Int,
    hasActiveLine: Boolean,
    accentColor: Color,
    accentBright: Color,
    onSeekToLine: () -> Unit
) {
    val colors = LocalAppColors.current
    val lineRtl = remember(line.text) { rtlRegex.containsMatchIn(line.text) }
    val lineDir = if (lineRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val targetAlpha = when {
        !hasActiveLine -> 0.72f
        isCurrent -> 1f
        distance == 1 -> 0.56f
        isPast -> 0.28f
        else -> 0.34f
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isCurrent) 1.02f else 0.985f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "lyric_scale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(260),
        label = "lyric_alpha"
    )

    val rawGap = nextLineTimeSec?.let { it - line.timeSec } ?: 4.5
    val activeSingWindow = max(
        0.55,
        min(rawGap * 0.90, max(1.3, line.text.length * 0.062 + 0.45))
    )
    val lineProgress = when {
        isCurrent -> ((effectiveTime - line.timeSec) / activeSingWindow).coerceIn(0.0, 1.0)
        isPast -> 1.0
        else -> 0.0
    }

    val dimColor = colors.lyricDim
    val peakColor = colors.lyricPeak
    val midColor = lerp(accentBright, peakColor, 0.45f)

    CompositionLocalProvider(LocalLayoutDirection provides lineDir) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scale(animatedScale)
                .alpha(animatedAlpha)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isCurrent) colors.glassSurfaceElevated else Color.Transparent)
                .border(
                    1.dp,
                    if (isCurrent) accentColor.copy(alpha = 0.42f) else Color.Transparent,
                    RoundedCornerShape(18.dp)
                )
                .clickable(onClick = onSeekToLine)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(listOf(accentBright, accentColor))
                        )
                )
            }

            if (isCurrent) {
                val annotated = if (line.wordTimings.isNotEmpty()) {
                    buildAnnotatedString {
                        line.wordTimings.forEachIndexed { wIdx, wt ->
                            val nextWordStart = line.wordTimings.getOrNull(wIdx + 1)?.timeSec
                                ?: (line.timeSec + activeSingWindow)
                            val wordDur = max(0.18, nextWordStart - wt.timeSec)
                            val wordProg = ((effectiveTime - wt.timeSec) / wordDur).coerceIn(0.0, 1.0).toFloat()
                            val color = if (wordProg < 0.5f) {
                                lerp(dimColor, midColor, wordProg * 2f)
                            } else {
                                lerp(midColor, peakColor, (wordProg - 0.5f) * 2f)
                            }
                            val shadow = if (wordProg > 0.12f) {
                                Shadow(
                                    color = accentBright.copy(alpha = (if (colors.isDark) 0.55f else 0.30f) * wordProg),
                                    offset = Offset.Zero,
                                    blurRadius = 14f * wordProg
                                )
                            } else null
                            withStyle(
                                SpanStyle(
                                    color = color,
                                    shadow = shadow
                                )
                            ) {
                                append(wt.text)
                            }
                            if (wIdx < line.wordTimings.lastIndex) append(" ")
                        }
                    }
                } else {
                    val tokens = line.text.split(Regex("""(?<=\s)|(?=\s+)"""))
                    val totalWeight = max(1.0, tokens.sumOf { tok ->
                        if (tok.isBlank()) 0.0 else (tok.length + 1.2)
                    })
                    var consumedWeight = 0.0

                    buildAnnotatedString {
                        tokens.forEach { tok ->
                            if (tok.isBlank()) {
                                append(tok)
                            } else {
                                val tokWeight = tok.length + 1.2
                                val startRatio = consumedWeight / totalWeight
                                consumedWeight += tokWeight
                                val endRatio = consumedWeight / totalWeight
                                val span = max(0.015, endRatio - startRatio)
                                val wordProg = ((lineProgress - startRatio) / span).coerceIn(0.0, 1.0).toFloat()

                                val color = if (wordProg < 0.45f) {
                                    lerp(dimColor, midColor, wordProg / 0.45f)
                                } else {
                                    lerp(midColor, peakColor, (wordProg - 0.45f) / 0.55f)
                                }
                                val shadow = if (wordProg > 0.12f) {
                                    Shadow(
                                        color = accentBright.copy(alpha = (if (colors.isDark) 0.55f else 0.30f) * wordProg),
                                        offset = Offset.Zero,
                                        blurRadius = 14f * wordProg
                                    )
                                } else null

                                withStyle(
                                    SpanStyle(
                                        color = color,
                                        shadow = shadow
                                    )
                                ) {
                                    append(tok)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = annotated,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 31.sp,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = line.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = formatDuration(line.timeSec),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = if (isCurrent) colors.accentSkyText.copy(alpha = 0.85f) else colors.textSubtle.copy(alpha = 0.65f)
                )
            }
        }
    }
}
