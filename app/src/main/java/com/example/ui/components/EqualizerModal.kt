package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EQ_BANDS
import com.example.data.model.EqPreset
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalStrings
import kotlin.math.roundToInt

private val PRESET_OPTIONS = listOf(
    EqPreset.FLAT,
    EqPreset.BASS_BOOST,
    EqPreset.POP,
    EqPreset.ROCK,
    EqPreset.VOCAL,
    EqPreset.ACOUSTIC
)

private val SPEED_OPTIONS = listOf(0.75f, 0.9f, 1f, 1.15f, 1.25f, 1.5f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerModal(
    gainsDb: List<Int>,
    preset: EqPreset,
    playbackRate: Float,
    onSelectPreset: (EqPreset) -> Unit,
    onChangeBand: (index: Int, db: Int) -> Unit,
    onChangePlaybackRate: (Float) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalAppColors.current
    val s = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = colors.accentSkyText,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = s.eqTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textStrong
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectPreset(EqPreset.FLAT)
                                onChangePlaybackRate(1f)
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .testTag("eq_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = s.reset, fontSize = 12.sp, color = colors.textMuted)
                    }

                    IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = s.close,
                            tint = colors.textMuted
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.glassBorder)

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = s.eqPresetsTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted
                    )
                    val rows = PRESET_OPTIONS.chunked(3)
                    rows.forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPresets.forEach { item ->
                                val selected = preset == item
                                val shape = RoundedCornerShape(12.dp)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .clip(shape)
                                        .background(
                                            if (selected) colors.accentSky.copy(alpha = 0.2f)
                                            else colors.glassSurface
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) colors.accentSky.copy(alpha = 0.5f) else colors.glassBorder,
                                            shape
                                        )
                                        .clickable { onSelectPreset(item) }
                                        .testTag("eq_preset_${item.name.lowercase()}")
                                ) {
                                    Text(
                                        text = item.label(s.isFa),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selected) colors.textStrong else colors.textSecondary
                                    )
                                }
                            }
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = s.eqManualTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted
                    )
                    EQ_BANDS.forEachIndexed { idx, band ->
                        val value = gainsDb.getOrElse(idx) { 0 }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.width(68.dp)) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = band.freqLabel,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                }
                                Text(
                                    text = band.desc(s.isFa),
                                    fontSize = 11.sp,
                                    color = colors.textSubtle
                                )
                            }

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Slider(
                                    value = value.toFloat(),
                                    onValueChange = { onChangeBand(idx, it.roundToInt()) },
                                    valueRange = -12f..12f,
                                    steps = 23,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colors.accentSky,
                                        activeTrackColor = colors.accentSky,
                                        inactiveTrackColor = colors.sliderInactive
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = if (value > 0) "+$value dB" else "$value dB",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = colors.accentSkyText,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(50.dp)
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.playbackSpeedTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "${playbackRate}x",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = colors.accentSkyText
                            )
                        }
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SPEED_OPTIONS.forEach { rate ->
                                val selected = kotlin.math.abs(playbackRate - rate) < 0.01f
                                val shape = RoundedCornerShape(12.dp)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .clip(shape)
                                        .background(
                                            if (selected) colors.accentIndigo.copy(alpha = 0.24f)
                                            else colors.glassSurface
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) colors.accentIndigo.copy(alpha = 0.55f) else colors.glassBorder,
                                            shape
                                        )
                                        .clickable { onChangePlaybackRate(rate) }
                                        .testTag("speed_option_${rate}")
                                ) {
                                    Text(
                                        text = "${rate}x",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = if (selected) colors.textStrong else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
