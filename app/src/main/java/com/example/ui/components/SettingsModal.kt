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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModal(
    themeMode: AppThemeMode,
    appLanguage: AppLanguage,
    trackCount: Int,
    hasAudioPermission: Boolean,
    isScanningDevice: Boolean,
    isImporting: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onRescanDevice: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onImportManually: () -> Unit,
    onLoadStarterTracks: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalAppColors.current
    val s = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = colors.sheetBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Sheet Top Handle + Title
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.handleBar)
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.settingsTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textStrong
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("settings_close_button")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Appearance / Theme
                SettingsSection(title = s.appearanceSection) {
                    SettingsOptionRow(
                        icon = Icons.Rounded.DarkMode,
                        iconTint = colors.accentIndigo,
                        title = s.darkThemeTitle,
                        subtitle = s.darkThemeDesc,
                        selected = themeMode == AppThemeMode.DARK,
                        onClick = { onSelectThemeMode(AppThemeMode.DARK) },
                        modifier = Modifier.testTag("settings_theme_dark")
                    )
                    HorizontalDivider(color = colors.glassBorder)
                    SettingsOptionRow(
                        icon = Icons.Rounded.LightMode,
                        iconTint = colors.accentAmber,
                        title = s.lightThemeTitle,
                        subtitle = s.lightThemeDesc,
                        selected = themeMode == AppThemeMode.LIGHT,
                        onClick = { onSelectThemeMode(AppThemeMode.LIGHT) },
                        modifier = Modifier.testTag("settings_theme_light")
                    )
                }

                // 2. Language
                SettingsSection(title = s.languageSection) {
                    SettingsOptionRow(
                        icon = Icons.Rounded.Language,
                        iconTint = colors.accentSkyText,
                        title = s.langSystemTitle,
                        subtitle = s.langSystemDesc,
                        selected = appLanguage == AppLanguage.SYSTEM,
                        onClick = { onSelectLanguage(AppLanguage.SYSTEM) },
                        modifier = Modifier.testTag("settings_lang_system")
                    )
                    HorizontalDivider(color = colors.glassBorder)
                    SettingsOptionRow(
                        icon = Icons.Rounded.Language,
                        iconTint = colors.accentEmerald,
                        title = s.langFaTitle,
                        subtitle = null,
                        selected = appLanguage == AppLanguage.FA,
                        onClick = { onSelectLanguage(AppLanguage.FA) },
                        modifier = Modifier.testTag("settings_lang_fa")
                    )
                    HorizontalDivider(color = colors.glassBorder)
                    SettingsOptionRow(
                        icon = Icons.Rounded.Language,
                        iconTint = colors.accentIndigo,
                        title = s.langEnTitle,
                        subtitle = null,
                        selected = appLanguage == AppLanguage.EN,
                        onClick = { onSelectLanguage(AppLanguage.EN) },
                        modifier = Modifier.testTag("settings_lang_en")
                    )
                }

                // 3. Sound & Playback
                SettingsSection(title = s.audioSection) {
                    SettingsActionRow(
                        icon = Icons.Rounded.Tune,
                        iconTint = colors.accentIndigo,
                        title = s.openEqTitle,
                        subtitle = s.openEqDesc,
                        onClick = {
                            onClose()
                            onOpenEqualizer()
                        },
                        modifier = Modifier.testTag("settings_open_eq")
                    )
                }

                // 4. Device Music Library
                SettingsSection(title = s.librarySection) {
                    if (!hasAudioPermission) {
                        SettingsActionRow(
                            icon = Icons.Rounded.LibraryMusic,
                            iconTint = colors.accentSkyText,
                            title = s.permissionTitle,
                            subtitle = s.permissionDesc,
                            onClick = {
                                onClose()
                                onRequestAudioPermission()
                            },
                            modifier = Modifier.testTag("settings_grant_permission")
                        )
                        HorizontalDivider(color = colors.glassBorder)
                    }
                    SettingsActionRow(
                        icon = Icons.Rounded.Refresh,
                        iconTint = colors.accentSkyText,
                        title = s.rescanDeviceTitle,
                        subtitle = if (isScanningDevice) s.scanningLibrary else s.rescanDeviceDesc(trackCount),
                        trailing = if (isScanningDevice) {
                            {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.accentSkyText
                                )
                            }
                        } else null,
                        enabled = !isScanningDevice,
                        onClick = {
                            if (hasAudioPermission) onRescanDevice()
                            else {
                                onClose()
                                onRequestAudioPermission()
                            }
                        },
                        modifier = Modifier.testTag("settings_rescan_library")
                    )
                    HorizontalDivider(color = colors.glassBorder)
                    SettingsActionRow(
                        icon = Icons.Rounded.FolderOpen,
                        iconTint = colors.textSecondary,
                        title = s.importManuallyTitle,
                        subtitle = s.importManuallyDesc,
                        enabled = !isImporting,
                        onClick = {
                            onClose()
                            onImportManually()
                        },
                        modifier = Modifier.testTag("settings_import_manual")
                    )
                    HorizontalDivider(color = colors.glassBorder)
                    SettingsActionRow(
                        icon = Icons.Rounded.MusicNote,
                        iconTint = colors.accentEmerald,
                        title = s.loadStarterTracksBtn,
                        subtitle = null,
                        enabled = !isImporting,
                        onClick = {
                            onLoadStarterTracks()
                            onClose()
                        },
                        modifier = Modifier.testTag("settings_load_starter")
                    )
                }

                // 5. About
                SettingsSection(title = s.aboutSection) {
                    SettingsActionRow(
                        icon = Icons.Rounded.Info,
                        iconTint = colors.textMuted,
                        title = s.brandName,
                        subtitle = s.versionLabel,
                        onClick = {},
                        enabled = false
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.accentSkyText,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.glassSurfaceElevated)
                .border(1.dp, colors.glassBorder, RoundedCornerShape(18.dp))
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsOptionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = colors.textStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) colors.accentSkyText else colors.textSubtle,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}
