package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppThemeMode
import com.example.ui.PlayerViewModel
import com.example.ui.components.EditTrackModal
import com.example.ui.components.EqualizerModal
import com.example.ui.components.LyricsModal
import com.example.ui.components.MobileMiniPlayer
import com.example.ui.components.PlayerStage
import com.example.ui.components.RingtoneMakerModal
import com.example.ui.components.TrackListPanel
import com.example.ui.theme.Indigo600
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalStrings
import com.example.ui.theme.ToMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PlayerViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            DisposableEffect(state.themeMode) {
                val isDark = state.themeMode == AppThemeMode.DARK
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            ToMusicTheme(
                themeMode = state.themeMode,
                language = state.appLanguage
            ) {
                ToMusicApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToMusicApp(
    viewModel: PlayerViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    val s = LocalStrings.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioPermission = remember { viewModel.requiredAudioPermission() }
    var askedPermissionOnce by remember { mutableStateOf(false) }

    // Runtime permission launcher for audio discovery + optional notification permission (API 33+)
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioGranted = results[audioPermission]
            ?: (ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED)
        viewModel.onAudioPermissionResult(audioGranted)
    }

    val requestAudioAccess: () -> Unit = {
        val activity = context as? ComponentActivity
        val alreadyDeniedPermanently = askedPermissionOnce &&
            activity != null &&
            ContextCompat.checkSelfPermission(context, audioPermission) != PackageManager.PERMISSION_GRANTED &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, audioPermission)

        if (alreadyDeniedPermanently) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            }
        } else {
            askedPermissionOnce = true
            val perms = buildList {
                add(audioPermission)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            permissionsLauncher.launch(perms.toTypedArray())
        }
    }

    // Request permissions on launch if missing, or immediately scan device music
    LaunchedEffect(Unit) {
        val audioGranted = ContextCompat.checkSelfPermission(
            context,
            audioPermission
        ) == PackageManager.PERMISSION_GRANTED
        if (audioGranted) {
            viewModel.onAudioPermissionResult(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        } else {
            askedPermissionOnce = true
            val perms = buildList {
                add(audioPermission)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            permissionsLauncher.launch(perms.toTypedArray())
        }
    }

    // Auto-sync whenever returning to the app (catches new downloads or permission changes)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onPermissionCheckOrResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Fallback SAF multi-file audio + ZIP picker (only shown in empty/permission helper state)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris)
        }
    }

    val openAudioPicker = {
        filePickerLauncher.launch(
            arrayOf("audio/*", "application/zip", "application/x-zip-compressed")
        )
    }

    // Handle back press when mobile full-screen player sheet or album/artist drilldown is open
    BackHandler(enabled = state.isMobileSheetOpen || state.selectedAlbum != null || state.selectedArtist != null || state.isSelectionMode) {
        when {
            state.isMobileSheetOpen -> viewModel.setMobileSheetOpen(false)
            state.isSelectionMode -> viewModel.exitSelectionMode()
            state.selectedAlbum != null -> viewModel.selectAlbum(null)
            state.selectedArtist != null -> viewModel.selectArtist(null)
        }
    }

    val ambientGlowColor = state.activeTrack?.let {
        Color.hsl(it.accentHue.toFloat(), 0.80f, if (colors.isDark) 0.45f else 0.62f)
    } ?: Indigo600

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            // Ambient glassmorphic radial orbs
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ambientGlowColor.copy(alpha = colors.ambientOrbAlpha),
                                Color.Transparent
                            ),
                            radius = 900f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(320.dp)
                    .blur(80.dp)
                    .background(colors.accentSky.copy(alpha = colors.secondaryOrbAlpha), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Sticky App Header (Clean Brand + Auto-sync indicator + Language & Theme Glass Controls)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(colors.headerBackground)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = s.brandName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textStrong
                            )
                        }

                        AnimatedVisibility(visible = state.isScanningDevice) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.glassSurfaceElevated)
                                    .border(1.dp, colors.glassBorder, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.8.dp,
                                    color = colors.accentSkyText
                                )
                                Text(
                                    text = s.scanningLibrary,
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Subtle manual rescan trigger (no "Add Song" button)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.glassSurfaceElevated)
                                .border(1.dp, colors.glassBorderStrong, RoundedCornerShape(12.dp))
                                .clickable(enabled = !state.isScanningDevice) {
                                    if (state.hasAudioPermission) {
                                        viewModel.refreshDeviceLibrary(quiet = false, resetIgnored = true)
                                    } else {
                                        requestAudioAccess()
                                    }
                                }
                                .testTag("rescan_library_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = s.rescanDesc,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Language Switch Pill (System default -> toggleable FA / EN)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.glassSurfaceElevated)
                                .border(1.dp, colors.glassBorderStrong, RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleLanguage() }
                                .padding(horizontal = 12.dp)
                                .testTag("language_toggle_button")
                        ) {
                            Text(
                                text = s.switchLangLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textStrong
                            )
                        }

                        // Theme Switch Button (Dark default <-> Frosted Glass Light)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.glassSurfaceElevated)
                                .border(1.dp, colors.glassBorderStrong, RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleThemeMode() }
                                .testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (colors.isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = if (colors.isDark) s.switchToLightDesc else s.switchToDarkDesc,
                                tint = if (colors.isDark) colors.accentAmber else colors.accentIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = colors.glassBorder)

                // Offline indicator banner
                AnimatedVisibility(visible = !state.isOnline) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.accentAmber.copy(alpha = 0.12f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiOff,
                            contentDescription = null,
                            tint = colors.accentAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.offlineBanner,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.accentAmberText
                        )
                    }
                }

                // Adaptive Main Content (Phone vs Tablet/Foldable Two-Pane)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    val isTwoPane = maxWidth >= 840.dp

                    if (state.isInitialLoading) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1.15f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(colors.glassSurface)
                                    .border(1.dp, colors.glassBorderStrong, RoundedCornerShape(26.dp))
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                repeat(4) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(colors.glassSurfaceElevated)
                                    )
                                }
                            }
                            if (isTwoPane) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(colors.glassSurface)
                                        .border(1.dp, colors.glassBorderStrong, RoundedCornerShape(26.dp))
                                )
                            }
                        }
                    } else if (isTwoPane) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            TrackListPanel(
                                allTracks = state.allTracks,
                                tracks = state.filteredTracks,
                                albumGroups = state.albumGroups,
                                artistGroups = state.artistGroups,
                                activeTrackId = state.activeTrack?.id,
                                isPlaying = state.isPlaying,
                                libraryTab = state.libraryTab,
                                selectedAlbum = state.selectedAlbum,
                                selectedArtist = state.selectedArtist,
                                searchQuery = state.searchQuery,
                                sortMode = state.sortMode,
                                favoriteCount = state.favoriteCount,
                                recentCount = state.recentCount,
                                isSelectionMode = state.isSelectionMode,
                                selectedIds = state.selectedIds,
                                jumpSignal = state.jumpSignal,
                                isImporting = state.isImporting,
                                hasAudioPermission = state.hasAudioPermission,
                                showDesktopJumpButton = true,
                                onLibraryTabChange = viewModel::setLibraryTab,
                                onSelectAlbum = viewModel::selectAlbum,
                                onSelectArtist = viewModel::selectArtist,
                                onPlayCollection = viewModel::playCollection,
                                onSearchChange = viewModel::setSearchQuery,
                                onSortChange = viewModel::setSortMode,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onSelectTrack = { viewModel.loadAndPlayTrack(it, true) },
                                onEnterSelectionWithTrack = viewModel::enterSelectionWithTrack,
                                onExitSelectionMode = viewModel::exitSelectionMode,
                                onToggleTrackSelection = viewModel::toggleTrackSelection,
                                onSelectAllFiltered = viewModel::selectAllFiltered,
                                onClearSelection = viewModel::clearSelection,
                                onBulkDelete = viewModel::bulkDelete,
                                onBulkQueue = viewModel::bulkQueue,
                                onPlayNextInQueue = viewModel::playNextInQueue,
                                onOpenTrackLyrics = { viewModel.openLyricsModal(it) },
                                onOpenTrackRingtone = { viewModel.openRingtoneModal(it) },
                                onOpenTrackEdit = { viewModel.openEditModal(it) },
                                onJumpToActive = viewModel::jumpToActive,
                                onRequestAudioPermission = requestAudioAccess,
                                onTriggerAddFiles = openAudioPicker,
                                onLoadStarterTracks = viewModel::loadStarterTracks,
                                modifier = Modifier
                                    .weight(1.15f)
                                    .fillMaxHeight()
                            )

                            PlayerStage(
                                activeTrack = state.activeTrack,
                                isPlaying = state.isPlaying,
                                currentTimeSec = state.currentTimeSec,
                                durationSec = state.durationSec,
                                volume = state.volume,
                                shuffle = state.shuffle,
                                repeatMode = state.repeatMode,
                                sleepMinutesLeft = state.sleepMinutesLeft,
                                eqPreset = state.eqPreset,
                                playbackRate = state.playbackRate,
                                upNextTracks = state.upNextTracks,
                                manualQueueCount = state.manualQueueIds.size,
                                onTogglePlay = viewModel::togglePlay,
                                onNext = { viewModel.handlePlayNext(false) },
                                onPrev = viewModel::handlePlayPrev,
                                onSeek = viewModel::seekTo,
                                onVolumeChange = viewModel::setVolume,
                                onToggleShuffle = viewModel::toggleShuffle,
                                onCycleRepeat = viewModel::cycleRepeatMode,
                                onSetSleepTimer = viewModel::setSleepTimer,
                                onSelectTrack = { viewModel.loadAndPlayTrack(it, true) },
                                onClearManualQueue = viewModel::clearManualQueue,
                                onJumpToActiveInList = viewModel::jumpToActive,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onOpenLyrics = { viewModel.openLyricsModal(state.activeTrack?.id) },
                                onOpenRingtoneMaker = { viewModel.openRingtoneModal(state.activeTrack?.id) },
                                onOpenEqualizer = viewModel::openEqModal,
                                onOpenEditTrack = { viewModel.openEditModal(state.activeTrack?.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        // Compact Phone Layout
                        Box(modifier = Modifier.fillMaxSize()) {
                            TrackListPanel(
                                allTracks = state.allTracks,
                                tracks = state.filteredTracks,
                                albumGroups = state.albumGroups,
                                artistGroups = state.artistGroups,
                                activeTrackId = state.activeTrack?.id,
                                isPlaying = state.isPlaying,
                                libraryTab = state.libraryTab,
                                selectedAlbum = state.selectedAlbum,
                                selectedArtist = state.selectedArtist,
                                searchQuery = state.searchQuery,
                                sortMode = state.sortMode,
                                favoriteCount = state.favoriteCount,
                                recentCount = state.recentCount,
                                isSelectionMode = state.isSelectionMode,
                                selectedIds = state.selectedIds,
                                jumpSignal = state.jumpSignal,
                                isImporting = state.isImporting,
                                hasAudioPermission = state.hasAudioPermission,
                                showDesktopJumpButton = false,
                                onLibraryTabChange = viewModel::setLibraryTab,
                                onSelectAlbum = viewModel::selectAlbum,
                                onSelectArtist = viewModel::selectArtist,
                                onPlayCollection = viewModel::playCollection,
                                onSearchChange = viewModel::setSearchQuery,
                                onSortChange = viewModel::setSortMode,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onSelectTrack = { viewModel.loadAndPlayTrack(it, true) },
                                onEnterSelectionWithTrack = viewModel::enterSelectionWithTrack,
                                onExitSelectionMode = viewModel::exitSelectionMode,
                                onToggleTrackSelection = viewModel::toggleTrackSelection,
                                onSelectAllFiltered = viewModel::selectAllFiltered,
                                onClearSelection = viewModel::clearSelection,
                                onBulkDelete = viewModel::bulkDelete,
                                onBulkQueue = viewModel::bulkQueue,
                                onPlayNextInQueue = viewModel::playNextInQueue,
                                onOpenTrackLyrics = { viewModel.openLyricsModal(it) },
                                onOpenTrackRingtone = { viewModel.openRingtoneModal(it) },
                                onOpenTrackEdit = { viewModel.openEditModal(it) },
                                onJumpToActive = viewModel::jumpToActive,
                                onRequestAudioPermission = requestAudioAccess,
                                onTriggerAddFiles = openAudioPicker,
                                onLoadStarterTracks = viewModel::loadStarterTracks,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        bottom = if (state.activeTrack != null && !state.isSelectionMode) 82.dp else 0.dp
                                    )
                            )

                            // Mobile floating jump-to-active pill
                            val active = state.activeTrack
                            if (active != null && !state.isSelectionMode && !state.isMobileSheetOpen) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .heightIn(min = 42.dp)
                                            .clip(CircleShape)
                                            .background(colors.modalBackground)
                                            .border(1.dp, colors.accentSky.copy(alpha = 0.35f), CircleShape)
                                            .clickable { viewModel.jumpToActive() }
                                            .padding(horizontal = 14.dp, vertical = 9.dp)
                                            .testTag("mobile_jump_to_active_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MyLocation,
                                            contentDescription = s.jumpToActiveDesc,
                                            tint = colors.accentSkyText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = s.activeTrackPill,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.accentSkySoft
                                        )
                                    }

                                    MobileMiniPlayer(
                                        activeTrack = active,
                                        isPlaying = state.isPlaying,
                                        currentTimeSec = state.currentTimeSec,
                                        durationSec = state.durationSec,
                                        onTogglePlay = viewModel::togglePlay,
                                        onNext = { viewModel.handlePlayNext(false) },
                                        onExpand = { viewModel.setMobileSheetOpen(true) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Status Notice Toast Banner
            AnimatedVisibility(
                visible = state.statusNotice != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp)
            ) {
                state.statusNotice?.let { notice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.modalBackground)
                            .border(1.dp, colors.accentEmerald.copy(alpha = 0.38f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = colors.accentEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = notice,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.accentEmeraldText
                        )
                    }
                }
            }
        }
    }

    // Mobile Full-Screen Player Sheet
    if (state.isMobileSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.setMobileSheetOpen(false) },
            sheetState = sheetState,
            containerColor = colors.sheetBackground,
            dragHandle = null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(10.dp)
            ) {
                PlayerStage(
                    activeTrack = state.activeTrack,
                    isPlaying = state.isPlaying,
                    currentTimeSec = state.currentTimeSec,
                    durationSec = state.durationSec,
                    volume = state.volume,
                    shuffle = state.shuffle,
                    repeatMode = state.repeatMode,
                    sleepMinutesLeft = state.sleepMinutesLeft,
                    eqPreset = state.eqPreset,
                    playbackRate = state.playbackRate,
                    upNextTracks = state.upNextTracks,
                    manualQueueCount = state.manualQueueIds.size,
                    onTogglePlay = viewModel::togglePlay,
                    onNext = { viewModel.handlePlayNext(false) },
                    onPrev = viewModel::handlePlayPrev,
                    onSeek = viewModel::seekTo,
                    onVolumeChange = viewModel::setVolume,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeatMode,
                    onSetSleepTimer = viewModel::setSleepTimer,
                    onSelectTrack = { viewModel.loadAndPlayTrack(it, true) },
                    onClearManualQueue = viewModel::clearManualQueue,
                    onJumpToActiveInList = viewModel::jumpToActive,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onOpenLyrics = { viewModel.openLyricsModal(state.activeTrack?.id) },
                    onOpenRingtoneMaker = { viewModel.openRingtoneModal(state.activeTrack?.id) },
                    onOpenEqualizer = viewModel::openEqModal,
                    onOpenEditTrack = { viewModel.openEditModal(state.activeTrack?.id) },
                    onCloseMobileSheet = { viewModel.setMobileSheetOpen(false) }
                )
            }
        }
    }

    // Lyrics Modal
    val modalTrack = state.modalTrack
    if (state.showLyricsModal && modalTrack != null) {
        LyricsModal(
            track = modalTrack,
            currentTimeSec = state.currentTimeSec,
            durationSec = state.durationSec,
            isPlaying = state.isPlaying,
            playbackRate = state.playbackRate,
            onTogglePlay = viewModel::togglePlay,
            onSeek = viewModel::seekTo,
            onSaveLyrics = viewModel::saveLyrics,
            onClose = viewModel::closeLyricsModal
        )
    }

    // Ringtone Maker Modal
    if (state.showRingtoneModal && modalTrack != null) {
        RingtoneMakerModal(
            track = modalTrack,
            onPauseMainPlayer = viewModel::pauseForPreview,
            onClose = viewModel::closeRingtoneModal
        )
    }

    // Equalizer Modal
    if (state.showEqModal) {
        EqualizerModal(
            gainsDb = state.eqGainsDb,
            preset = state.eqPreset,
            playbackRate = state.playbackRate,
            onSelectPreset = viewModel::selectEqPreset,
            onChangeBand = viewModel::changeEqBand,
            onChangePlaybackRate = viewModel::changePlaybackRate,
            onClose = viewModel::closeEqModal
        )
    }

    // Edit Track Modal
    if (state.showEditModal && modalTrack != null) {
        EditTrackModal(
            track = modalTrack,
            onSave = viewModel::saveTrackDetails,
            onClose = viewModel::closeEditModal
        )
    }
}
