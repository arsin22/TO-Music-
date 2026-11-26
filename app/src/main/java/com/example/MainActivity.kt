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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.ui.components.SettingsModal
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
    var isSearchOpen by remember { mutableStateOf(false) }

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

    // Fallback SAF multi-file audio + ZIP picker (only triggered from Settings or empty-state fallback)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris)
        }
    }

    val openAudioPicker: () -> Unit = {
        filePickerLauncher.launch(
            arrayOf("audio/*", "application/zip", "application/x-zip-compressed")
        )
    }

    // Handle back press cleanly across overlays and drill-down views
    BackHandler(
        enabled = state.showSettingsModal ||
            state.isMobileSheetOpen ||
            state.selectedAlbum != null ||
            state.selectedArtist != null ||
            state.isSelectionMode ||
            isSearchOpen ||
            state.searchQuery.isNotEmpty()
    ) {
        when {
            state.showSettingsModal -> viewModel.closeSettingsModal()
            state.isMobileSheetOpen -> viewModel.setMobileSheetOpen(false)
            state.isSelectionMode -> viewModel.exitSelectionMode()
            state.selectedAlbum != null -> viewModel.selectAlbum(null)
            state.selectedArtist != null -> viewModel.selectArtist(null)
            state.searchQuery.isNotEmpty() -> {
                viewModel.setSearchQuery("")
                isSearchOpen = false
            }
            isSearchOpen -> isSearchOpen = false
        }
    }

    val ambientGlowColor = remember(state.activeTrack?.accentHue, colors.isDark) {
        state.activeTrack?.let {
            Color.hsl(it.accentHue.toFloat(), 0.80f, if (colors.isDark) 0.45f else 0.62f)
        } ?: Indigo600
    }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            // Fast 120Hz radial aura layers (no GPU-blocking Modifier.blur)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ambientGlowColor.copy(alpha = colors.ambientOrbAlpha),
                                Color.Transparent
                            ),
                            radius = 950f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(340.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colors.accentSky.copy(alpha = colors.secondaryOrbAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Clean Native Top App Bar (Title + Search + Settings)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
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
                                fontSize = 20.sp,
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
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (isSearchOpen && state.searchQuery.isNotEmpty()) {
                                    viewModel.setSearchQuery("")
                                }
                                isSearchOpen = !isSearchOpen
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("header_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = s.searchActionDesc,
                                tint = if (isSearchOpen || state.searchQuery.isNotEmpty()) {
                                    colors.accentSkyText
                                } else {
                                    colors.textSecondary
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = viewModel::openSettingsModal,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("header_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = s.settingsDesc,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Offline indicator banner
                AnimatedVisibility(visible = !state.isOnline) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.accentAmber.copy(alpha = 0.12f))
                            .padding(horizontal = 16.dp, vertical = 7.dp),
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

                // Adaptive Full-Screen Content (Compact Edge-to-Edge Phone vs Two-Pane Tablet)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val isTwoPane = maxWidth >= 840.dp

                    if (state.isInitialLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (isTwoPane) 16.dp else 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1.15f)
                                    .fillMaxHeight()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                repeat(5) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(58.dp)
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
                                )
                            }
                        }
                    } else if (isTwoPane) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .navigationBarsPadding(),
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
                                isSearchOpen = isSearchOpen,
                                sortMode = state.sortMode,
                                favoriteCount = state.favoriteCount,
                                recentCount = state.recentCount,
                                isSelectionMode = state.isSelectionMode,
                                selectedIds = state.selectedIds,
                                jumpSignal = state.jumpSignal,
                                isImporting = state.isImporting,
                                hasAudioPermission = state.hasAudioPermission,
                                showDesktopJumpButton = true,
                                listBottomPadding = 76.dp,
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
                                progressFlow = viewModel.playbackProgressFlow,
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
                                isFullScreen = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        // Full-Screen Edge-to-Edge Phone Layout
                        val showMiniPlayer = state.activeTrack != null && !state.isSelectionMode
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
                                isSearchOpen = isSearchOpen,
                                sortMode = state.sortMode,
                                favoriteCount = state.favoriteCount,
                                recentCount = state.recentCount,
                                isSelectionMode = state.isSelectionMode,
                                selectedIds = state.selectedIds,
                                jumpSignal = state.jumpSignal,
                                isImporting = state.isImporting,
                                hasAudioPermission = state.hasAudioPermission,
                                showDesktopJumpButton = false,
                                listBottomPadding = if (showMiniPlayer) 96.dp else 28.dp,
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
                                    .then(if (state.isSelectionMode) Modifier.navigationBarsPadding() else Modifier)
                            )

                            val active = state.activeTrack
                            if (active != null && !state.isSelectionMode) {
                                MobileMiniPlayer(
                                    activeTrack = active,
                                    isPlaying = state.isPlaying,
                                    progressFlow = viewModel.playbackProgressFlow,
                                    onTogglePlay = viewModel::togglePlay,
                                    onNext = { viewModel.handlePlayNext(false) },
                                    onExpand = { viewModel.setMobileSheetOpen(true) },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }

            // True 100% Full-Screen Native Now Playing Overlay
            AnimatedVisibility(
                visible = state.isMobileSheetOpen,
                enter = slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) { it } + fadeIn(),
                exit = slideOutVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) { it } + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                PlayerStage(
                    activeTrack = state.activeTrack,
                    isPlaying = state.isPlaying,
                    progressFlow = viewModel.playbackProgressFlow,
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
                    isFullScreen = true,
                    onCloseMobileSheet = { viewModel.setMobileSheetOpen(false) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Status Notice Toast Banner
            AnimatedVisibility(
                visible = state.statusNotice != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp, start = 16.dp, end = 16.dp)
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

    // Settings Modal (Appearance, Language, Library Rescan, EQ)
    if (state.showSettingsModal) {
        SettingsModal(
            themeMode = state.themeMode,
            appLanguage = state.appLanguage,
            trackCount = state.allTracks.size,
            hasAudioPermission = state.hasAudioPermission,
            isScanningDevice = state.isScanningDevice,
            isImporting = state.isImporting,
            onSelectThemeMode = viewModel::setThemeMode,
            onSelectLanguage = viewModel::setLanguage,
            onRescanDevice = { viewModel.refreshDeviceLibrary(quiet = false, resetIgnored = true) },
            onRequestAudioPermission = requestAudioAccess,
            onImportManually = openAudioPicker,
            onLoadStarterTracks = viewModel::loadStarterTracks,
            onOpenEqualizer = viewModel::openEqModal,
            onClose = viewModel::closeSettingsModal
        )
    }

    // Lyrics Modal
    val modalTrack = state.modalTrack
    if (state.showLyricsModal && modalTrack != null) {
        LyricsModal(
            track = modalTrack,
            progressFlow = viewModel.playbackProgressFlow,
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
