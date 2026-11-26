package com.example.ui.components

import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RingtoneExporter
import com.example.data.model.RingtoneTarget
import com.example.data.model.TrackEntity
import com.example.data.model.formatDuration
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Indigo600
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalStrings
import com.example.ui.theme.Red400
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneMakerModal(
    track: TrackEntity,
    onPauseMainPlayer: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val totalSec = remember(track.id, track.durationSec) {
        max(2, floor(track.durationSec).toInt().takeIf { it > 0 } ?: 30)
    }

    var target by remember { mutableStateOf(RingtoneTarget.RINGTONE) }
    var startSec by remember(track.id) { mutableIntStateOf(0) }
    var endSec by remember(track.id) { mutableIntStateOf(min(totalSec, 30)) }
    var fadeEdges by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }
    var isPreviewing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var exportedMsg by remember { mutableStateOf<String?>(null) }

    val previewPlayerHolder = remember { arrayOfNulls<MediaPlayer>(1) }

    fun stopPreview() {
        previewPlayerHolder[0]?.let { mp ->
            runCatching {
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        previewPlayerHolder[0] = null
        isPreviewing = false
    }

    DisposableEffect(Unit) {
        onDispose { stopPreview() }
    }

    fun selectTarget(next: RingtoneTarget) {
        stopPreview()
        target = next
        val clampedEnd = min(totalSec, startSec + next.maxDurationSec)
        endSec = max(startSec + 1, clampedEnd)
    }

    fun buildSafeFileName(): String {
        val cleaned = track.title
            .replace(Regex("""[^\p{L}\p{N}_-]+"""), "-")
            .trim('-')
            .take(32)
            .ifEmpty { "audio" }
        return "${target.prefix}-$cleaned.wav"
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            stopPreview()
            onClose()
        },
        sheetState = sheetState,
        containerColor = colors.modalBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.ringtoneTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textStrong,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.title} · ${s.totalDurationLabel(formatDuration(totalSec.toDouble()))}",
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        stopPreview()
                        onClose()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = s.close,
                        tint = colors.textMuted
                    )
                }
            }

            HorizontalDivider(color = colors.glassBorder)

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val targets = listOf(
                        RingtoneTarget.RINGTONE to Icons.Rounded.PhoneInTalk,
                        RingtoneTarget.ALARM to Icons.Rounded.Alarm,
                        RingtoneTarget.NOTIFICATION to Icons.Rounded.NotificationsActive
                    )
                    targets.forEach { (item, icon) ->
                        val selected = target == item
                        val shape = RoundedCornerShape(16.dp)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 60.dp)
                                .clip(shape)
                                .background(if (selected) colors.accentSky.copy(alpha = 0.2f) else colors.glassSurface)
                                .border(
                                    1.dp,
                                    if (selected) colors.accentSky.copy(alpha = 0.55f) else colors.glassBorder,
                                    shape
                                )
                                .clickable { selectTarget(item) }
                                .padding(horizontal = 6.dp, vertical = 10.dp)
                                .testTag("ringtone_target_${item.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) colors.textStrong else colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.label(s.isFa),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) colors.textStrong else colors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.glassInput)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val clipDuration = max(1, endSec - startSec)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.selectedRangeLabel,
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "${formatDuration(startSec.toDouble())} – ${formatDuration(endSec.toDouble())} (${clipDuration}s)",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accentSkyText
                            )
                        }
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Start", fontSize = 11.sp, color = colors.textMuted)
                                Text(
                                    formatDuration(startSec.toDouble()),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                            Slider(
                                value = startSec.toFloat(),
                                onValueChange = {
                                    stopPreview()
                                    val nextStart = it.roundToInt().coerceIn(0, max(0, totalSec - 1))
                                    startSec = nextStart
                                    if (endSec <= nextStart) {
                                        endSec = min(totalSec, nextStart + target.maxDurationSec)
                                    }
                                },
                                valueRange = 0f..max(1, totalSec - 1).toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.accentSky,
                                    activeTrackColor = colors.accentSky,
                                    inactiveTrackColor = colors.sliderInactive
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("End", fontSize = 11.sp, color = colors.textMuted)
                                Text(
                                    formatDuration(endSec.toDouble()),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                            Slider(
                                value = endSec.toFloat(),
                                onValueChange = {
                                    stopPreview()
                                    endSec = it.roundToInt().coerceIn(startSec + 1, totalSec)
                                },
                                valueRange = 1f..totalSec.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.accentIndigo,
                                    activeTrackColor = colors.accentIndigo,
                                    inactiveTrackColor = colors.sliderInactive
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { fadeEdges = !fadeEdges }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.fadeEdgesLabel,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = fadeEdges,
                            onCheckedChange = { fadeEdges = it },
                            colors = CheckboxDefaults.colors(checkedColor = colors.accentSky)
                        )
                    }
                }

                errorMsg?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Red400.copy(alpha = 0.15f))
                            .border(1.dp, Red400.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(text = msg, fontSize = 12.sp, color = colors.accentRedText)
                    }
                }

                exportedMsg?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Emerald400.copy(alpha = 0.15f))
                            .border(1.dp, Emerald400.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(text = msg, fontSize = 12.sp, lineHeight = 19.sp, color = colors.accentEmeraldText)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isPreviewing) {
                                stopPreview()
                                return@OutlinedButton
                            }
                            onPauseMainPlayer()
                            errorMsg = null
                            isExporting = true
                            scope.launch {
                                try {
                                    val wavFile = RingtoneExporter.trimToCacheWav(
                                        context = context,
                                        sourcePath = track.audioFilePath,
                                        startSec = startSec.toDouble(),
                                        endSec = endSec.toDouble(),
                                        fadeEdges = fadeEdges,
                                        outputFileName = "preview_${System.currentTimeMillis()}.wav"
                                    )
                                    val mp = MediaPlayer().apply {
                                        setDataSource(wavFile.absolutePath)
                                        setOnCompletionListener { stopPreview() }
                                        prepare()
                                        start()
                                    }
                                    previewPlayerHolder[0] = mp
                                    isPreviewing = true
                                } catch (_: Throwable) {
                                    errorMsg = s.previewError
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        enabled = !isExporting,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("ringtone_preview_button")
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colors.textStrong
                            )
                        } else {
                            Icon(
                                imageVector = if (isPreviewing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = colors.textStrong,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPreviewing) s.stopPreview else s.previewClip,
                            fontSize = 12.sp,
                            color = colors.textStrong,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            stopPreview()
                            errorMsg = null
                            exportedMsg = null
                            isExporting = true
                            scope.launch {
                                try {
                                    val filename = buildSafeFileName()
                                    val wavFile = RingtoneExporter.trimToCacheWav(
                                        context = context,
                                        sourcePath = track.audioFilePath,
                                        startSec = startSec.toDouble(),
                                        endSec = endSec.toDouble(),
                                        fadeEdges = fadeEdges,
                                        outputFileName = filename
                                    )
                                    RingtoneExporter.saveToMediaStore(
                                        context = context,
                                        wavFile = wavFile,
                                        displayName = filename,
                                        target = target
                                    )
                                    exportedMsg = s.ringtoneSavedMsg(filename)
                                } catch (_: Throwable) {
                                    errorMsg = s.exportError
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("ringtone_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = s.saveToDevice,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            stopPreview()
                            errorMsg = null
                            exportedMsg = null
                            isExporting = true
                            scope.launch {
                                try {
                                    val filename = buildSafeFileName()
                                    val wavFile = RingtoneExporter.trimToCacheWav(
                                        context = context,
                                        sourcePath = track.audioFilePath,
                                        startSec = startSec.toDouble(),
                                        endSec = endSec.toDouble(),
                                        fadeEdges = fadeEdges,
                                        outputFileName = filename
                                    )
                                    val shareUri = RingtoneExporter.getShareUri(context, wavFile)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/wav"
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, filename))
                                    exportedMsg = s.ringtoneShareReadyMsg(filename)
                                } catch (_: Throwable) {
                                    errorMsg = s.shareError
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        enabled = !isExporting,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("ringtone_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = s.shareClipDesc,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = s.ringtoneTip,
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}
