package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.audio.ConnectivityObserver
import com.example.audio.PlaybackService
import com.example.data.local.AppDatabase
import com.example.data.local.PlayerPreferences
import com.example.data.model.AlbumGroup
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.ArtistGroup
import com.example.data.model.EqPreset
import com.example.data.model.LibraryTab
import com.example.data.model.RepeatMode
import com.example.data.model.SortMode
import com.example.data.model.TrackEntity
import com.example.data.repository.MusicRepository
import com.example.ui.theme.resolveIsPersian
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackProgress(
    val currentTimeSec: Double = 0.0,
    val durationSec: Double = 0.0
)

data class PlayerUiState(
    val allTracks: List<TrackEntity> = emptyList(),
    val filteredTracks: List<TrackEntity> = emptyList(),
    val albumGroups: List<AlbumGroup> = emptyList(),
    val artistGroups: List<ArtistGroup> = emptyList(),
    val upNextTracks: List<TrackEntity> = emptyList(),
    val activeTrack: TrackEntity? = null,
    val modalTrack: TrackEntity? = null,
    val favoriteCount: Int = 0,
    val recentCount: Int = 0,
    val isInitialLoading: Boolean = true,
    val isImporting: Boolean = false,
    val isScanningDevice: Boolean = false,
    val hasAudioPermission: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val isPlaying: Boolean = false,
    val volume: Float = 0.85f,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val manualQueueIds: List<String> = emptyList(),
    val sleepMinutesLeft: Int? = null,
    val libraryTab: LibraryTab = LibraryTab.ALL,
    val selectedAlbum: String? = null,
    val selectedArtist: String? = null,
    val searchQuery: String = "",
    val sortMode: SortMode = SortMode.DATE_DESC,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val jumpSignal: Int = 0,
    val isMobileSheetOpen: Boolean = false,
    val statusNotice: String? = null,
    val isOnline: Boolean = true,
    val eqPreset: EqPreset = EqPreset.FLAT,
    val eqGainsDb: List<Int> = listOf(0, 0, 0, 0, 0),
    val playbackRate: Float = 1f,
    val showLyricsModal: Boolean = false,
    val showRingtoneModal: Boolean = false,
    val showEqModal: Boolean = false,
    val showEditModal: Boolean = false,
    val showSettingsModal: Boolean = false
)

@OptIn(FlowPreview::class)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val preferences = PlayerPreferences(application)
    private val repository = MusicRepository(application, database.trackDao(), preferences)
    private val connectivityObserver = ConnectivityObserver(application)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var noticeJob: Job? = null
    private var sleepJob: Job? = null
    private var positionPollingJob: Job? = null
    private var mediaObserverJob: Job? = null

    private val _isInitialLoading = MutableStateFlow(true)
    private val _isImporting = MutableStateFlow(false)
    private val _isScanningDevice = MutableStateFlow(false)
    private val _hasAudioPermission = MutableStateFlow(repository.hasAudioPermission())
    private val _themeMode = MutableStateFlow(AppThemeMode.DARK)
    private val _appLanguage = MutableStateFlow(AppLanguage.SYSTEM)

    private val _activeTrackId = MutableStateFlow<String?>(null)
    private val _isPlaying = MutableStateFlow(false)
    private val _playbackProgress = MutableStateFlow(PlaybackProgress())
    val playbackProgressFlow: StateFlow<PlaybackProgress> = _playbackProgress.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    private val _manualQueueIds = MutableStateFlow<List<String>>(emptyList())
    private val _sleepMinutesLeft = MutableStateFlow<Int?>(null)

    private val _libraryTab = MutableStateFlow(LibraryTab.ALL)
    private val _selectedAlbum = MutableStateFlow<String?>(null)
    private val _selectedArtist = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _sortMode = MutableStateFlow(SortMode.DATE_DESC)
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _jumpSignal = MutableStateFlow(0)
    private val _isMobileSheetOpen = MutableStateFlow(false)
    private val _statusNotice = MutableStateFlow<String?>(null)

    private val _modalTrackId = MutableStateFlow<String?>(null)
    private val _showLyricsModal = MutableStateFlow(false)
    private val _showRingtoneModal = MutableStateFlow(false)
    private val _showEqModal = MutableStateFlow(false)
    private val _showEditModal = MutableStateFlow(false)
    private val _showSettingsModal = MutableStateFlow(false)

    private data class PlaybackControlState(
        val activeTrackId: String?,
        val isPlaying: Boolean,
        val volume: Float,
        val shuffle: Boolean,
        val repeatMode: RepeatMode,
        val manualQueueIds: List<String>,
        val sleepMinutesLeft: Int?
    )

    private data class LibraryFilterState(
        val libraryTab: LibraryTab,
        val selectedAlbum: String?,
        val selectedArtist: String?,
        val searchQuery: String,
        val sortMode: SortMode,
        val isSelectionMode: Boolean,
        val selectedIds: Set<String>
    )

    private data class UiMetaState(
        val isInitialLoading: Boolean,
        val isImporting: Boolean,
        val isScanningDevice: Boolean,
        val hasAudioPermission: Boolean,
        val themeMode: AppThemeMode,
        val appLanguage: AppLanguage,
        val jumpSignal: Int,
        val isMobileSheetOpen: Boolean,
        val statusNotice: String?,
        val isOnline: Boolean,
        val eqPreset: EqPreset,
        val eqGains: List<Int>,
        val playbackRate: Float,
        val modalTrackId: String?,
        val showLyrics: Boolean,
        val showRingtone: Boolean,
        val showEq: Boolean,
        val showEdit: Boolean,
        val showSettings: Boolean
    )

    private val _eqPreset = MutableStateFlow(EqPreset.FLAT)
    private val _eqGains = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    private val _playbackRate = MutableStateFlow(1f)
    private val _volume = MutableStateFlow(0.85f)

    private val playbackControlFlow = combine(
        _activeTrackId,
        _isPlaying,
        _volume,
        _shuffle,
        _repeatMode,
        _manualQueueIds,
        _sleepMinutesLeft
    ) { arr ->
        PlaybackControlState(
            activeTrackId = arr[0] as String?,
            isPlaying = arr[1] as Boolean,
            volume = arr[2] as Float,
            shuffle = arr[3] as Boolean,
            repeatMode = arr[4] as RepeatMode,
            manualQueueIds = @Suppress("UNCHECKED_CAST") (arr[5] as List<String>),
            sleepMinutesLeft = arr[6] as Int?
        )
    }

    private val libraryFilterFlow = combine(
        _libraryTab,
        _selectedAlbum,
        _selectedArtist,
        _searchQuery,
        _sortMode,
        _isSelectionMode,
        _selectedIds
    ) { arr ->
        LibraryFilterState(
            libraryTab = arr[0] as LibraryTab,
            selectedAlbum = arr[1] as String?,
            selectedArtist = arr[2] as String?,
            searchQuery = arr[3] as String,
            sortMode = arr[4] as SortMode,
            isSelectionMode = arr[5] as Boolean,
            selectedIds = @Suppress("UNCHECKED_CAST") (arr[6] as Set<String>)
        )
    }

    private val uiMetaFlow = combine(
        combine(_isInitialLoading, _isImporting, _isScanningDevice, _hasAudioPermission, _themeMode) { a, b, c, d, e ->
            listOf(a, b, c, d, e)
        },
        combine(_appLanguage, _jumpSignal, _isMobileSheetOpen, _statusNotice, connectivityObserver.isOnline) { a, b, c, d, e ->
            listOf(a, b, c, d, e)
        },
        combine(_eqPreset, _eqGains, _playbackRate) { a, b, c ->
            listOf(a, b, c)
        },
        combine(_modalTrackId, _showLyricsModal, _showRingtoneModal, _showEqModal, _showEditModal, _showSettingsModal) { arr ->
            arr
        }
    ) { p1, p2, p3, p4 ->
        UiMetaState(
            isInitialLoading = p1[0] as Boolean,
            isImporting = p1[1] as Boolean,
            isScanningDevice = p1[2] as Boolean,
            hasAudioPermission = p1[3] as Boolean,
            themeMode = p1[4] as AppThemeMode,
            appLanguage = p2[0] as AppLanguage,
            jumpSignal = p2[1] as Int,
            isMobileSheetOpen = p2[2] as Boolean,
            statusNotice = p2[3] as String?,
            isOnline = p2[4] as Boolean,
            eqPreset = p3[0] as EqPreset,
            eqGains = @Suppress("UNCHECKED_CAST") (p3[1] as List<Int>),
            playbackRate = p3[2] as Float,
            modalTrackId = p4[0] as String?,
            showLyrics = p4[1] as Boolean,
            showRingtone = p4[2] as Boolean,
            showEq = p4[3] as Boolean,
            showEdit = p4[4] as Boolean,
            showSettings = p4[5] as Boolean
        )
    }

    val uiState: StateFlow<PlayerUiState> = combine(
        repository.allTracksFlow,
        playbackControlFlow,
        libraryFilterFlow,
        uiMetaFlow
    ) { tracks, playback, filter, meta ->
        val isFa = resolveIsPersian(meta.appLanguage)
        val filtered = filterAndSortTracks(tracks, filter, isFa)
        val albums = buildAlbumGroups(tracks, filter.searchQuery, isFa)
        val artists = buildArtistGroups(tracks, filter.searchQuery, isFa)
        val activeTrack = tracks.firstOrNull { it.id == playback.activeTrackId }
        val modalTrack = tracks.firstOrNull { it.id == meta.modalTrackId } ?: activeTrack
        val upNext = buildUpNext(activeTrack, tracks, filtered, playback.manualQueueIds)

        PlayerUiState(
            allTracks = tracks,
            filteredTracks = filtered,
            albumGroups = albums,
            artistGroups = artists,
            upNextTracks = upNext,
            activeTrack = activeTrack,
            modalTrack = modalTrack,
            favoriteCount = tracks.count { it.isFavorite },
            recentCount = tracks.count { it.lastPlayedAt != null },
            isInitialLoading = meta.isInitialLoading,
            isImporting = meta.isImporting,
            isScanningDevice = meta.isScanningDevice,
            hasAudioPermission = meta.hasAudioPermission,
            themeMode = meta.themeMode,
            appLanguage = meta.appLanguage,
            isPlaying = playback.isPlaying,
            volume = playback.volume,
            shuffle = playback.shuffle,
            repeatMode = playback.repeatMode,
            manualQueueIds = playback.manualQueueIds,
            sleepMinutesLeft = playback.sleepMinutesLeft,
            libraryTab = filter.libraryTab,
            selectedAlbum = filter.selectedAlbum,
            selectedArtist = filter.selectedArtist,
            searchQuery = filter.searchQuery,
            sortMode = filter.sortMode,
            isSelectionMode = filter.isSelectionMode,
            selectedIds = filter.selectedIds,
            jumpSignal = meta.jumpSignal,
            isMobileSheetOpen = meta.isMobileSheetOpen,
            statusNotice = meta.statusNotice,
            isOnline = meta.isOnline,
            eqPreset = meta.eqPreset,
            eqGainsDb = meta.eqGains,
            playbackRate = meta.playbackRate,
            showLyricsModal = meta.showLyrics,
            showRingtoneModal = meta.showRingtone,
            showEqModal = meta.showEq,
            showEditModal = meta.showEdit,
            showSettingsModal = meta.showSettings
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerUiState()
        )

    init {
        bootstrap()
        connectMediaController()
    }

    private fun isFaNow(): Boolean = resolveIsPersian(_appLanguage.value)

    private fun bootstrap() {
        viewModelScope.launch {
            try {
                val eqPrefs = preferences.eqSettingsFlow.first()
                _eqPreset.value = eqPrefs.preset
                _eqGains.value = eqPrefs.gainsDb
                _playbackRate.value = eqPrefs.playbackRate
                _volume.value = eqPrefs.volume

                val appearance = preferences.appearanceSettingsFlow.first()
                _themeMode.value = appearance.themeMode
                _appLanguage.value = appearance.language

                val hasPerm = repository.hasAudioPermission()
                _hasAudioPermission.value = hasPerm
                if (hasPerm) {
                    startMediaStoreObserver()
                }

                val loaded = repository.bootstrapIfNeeded(isFaNow())
                if (loaded.isNotEmpty() && _activeTrackId.value == null) {
                    val recentTrack = loaded
                        .sortedByDescending { it.lastPlayedAt ?: 0L }
                        .firstOrNull() ?: loaded.first()
                    _activeTrackId.value = recentTrack.id
                    _playbackProgress.value = PlaybackProgress(0.0, recentTrack.durationSec)
                }
            } catch (_: Throwable) {
                showNotice(if (isFaNow()) "خطا در بارگذاری کتابخانه موسیقی." else "Error loading music library.")
            } finally {
                _isInitialLoading.value = false
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        if (_themeMode.value == mode) return
        _themeMode.value = mode
        viewModelScope.launch {
            preferences.saveThemeMode(mode)
        }
    }

    fun toggleThemeMode() {
        val next = if (_themeMode.value == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK
        setThemeMode(next)
    }

    fun setLanguage(language: AppLanguage) {
        if (_appLanguage.value == language) return
        _appLanguage.value = language
        viewModelScope.launch {
            preferences.saveLanguage(language)
        }
    }

    fun toggleLanguage() {
        val currentlyFa = isFaNow()
        setLanguage(if (currentlyFa) AppLanguage.EN else AppLanguage.FA)
    }

    fun requiredAudioPermission(): String = repository.requiredAudioPermission()

    fun onPermissionCheckOrResume() {
        val granted = repository.hasAudioPermission()
        val wasGranted = _hasAudioPermission.value
        _hasAudioPermission.value = granted
        if (granted) {
            startMediaStoreObserver()
            refreshDeviceLibrary(quiet = wasGranted)
        }
    }

    fun onAudioPermissionResult(granted: Boolean) {
        _hasAudioPermission.value = granted
        if (granted) {
            startMediaStoreObserver()
            refreshDeviceLibrary(quiet = false)
        }
    }

    private fun startMediaStoreObserver() {
        if (mediaObserverJob?.isActive == true) return
        mediaObserverJob = viewModelScope.launch {
            repository.observeMediaStoreChanges()
                .debounce(700L)
                .collect {
                    if (repository.hasAudioPermission()) {
                        syncInternal(quiet = false, resetIgnored = false)
                    }
                }
        }
    }

    fun refreshDeviceLibrary(quiet: Boolean = false, resetIgnored: Boolean = false) {
        if (!repository.hasAudioPermission()) {
            _hasAudioPermission.value = false
            return
        }
        _hasAudioPermission.value = true
        startMediaStoreObserver()
        viewModelScope.launch {
            syncInternal(quiet = quiet, resetIgnored = resetIgnored)
        }
    }

    private suspend fun syncInternal(quiet: Boolean, resetIgnored: Boolean) {
        if (_isScanningDevice.value) return
        _isScanningDevice.value = true
        try {
            val isFa = isFaNow()
            val result = repository.syncDeviceMediaStore(
                isFa = isFa,
                includeIgnoredReset = resetIgnored
            )
            val tracksNow = uiState.value.allTracks
            if (_activeTrackId.value == null && tracksNow.isNotEmpty()) {
                val first = tracksNow.first()
                _activeTrackId.value = first.id
                _playbackProgress.value = PlaybackProgress(0.0, first.durationSec)
            }
            if (!quiet && result.addedCount > 0) {
                showNotice(
                    if (isFa) "${result.addedCount} آهنگ جدید از گوشی اضافه شد."
                    else "${result.addedCount} new songs found on device."
                )
            }
        } catch (_: Throwable) {
            if (!quiet) {
                showNotice(
                    if (isFaNow()) "خطا در همگام‌سازی آهنگ‌های گوشی."
                    else "Failed to sync device songs."
                )
            }
        } finally {
            _isScanningDevice.value = false
        }
    }

    private fun connectMediaController() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future

        future.addListener({
            runCatching {
                val controller = future.get()
                mediaController = controller
                controller.volume = _volume.value
                controller.setPlaybackSpeed(_playbackRate.value)
                syncControllerState(controller)
                sendEqCommand(_eqGains.value)

                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) startPositionPolling() else stopPositionPolling()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            val durMs = controller.duration
                            if (durMs > 0 && durMs != androidx.media3.common.C.TIME_UNSET) {
                                _playbackProgress.value = _playbackProgress.value.copy(
                                    durationSec = durMs / 1000.0
                                )
                            }
                        } else if (playbackState == Player.STATE_ENDED) {
                            handlePlayNext(fromTrackEnded = true)
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaItem?.mediaId?.let { id ->
                            if (id.isNotEmpty()) _activeTrackId.value = id
                        }
                    }
                })
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun syncControllerState(controller: MediaController) {
        _isPlaying.value = controller.isPlaying
        controller.currentMediaItem?.mediaId?.takeIf { it.isNotEmpty() }?.let {
            _activeTrackId.value = it
        }
        val dur = controller.duration
        val resolvedDur = if (dur > 0 && dur != androidx.media3.common.C.TIME_UNSET) {
            dur / 1000.0
        } else {
            _playbackProgress.value.durationSec
        }
        _playbackProgress.value = PlaybackProgress(
            currentTimeSec = (controller.currentPosition / 1000.0).coerceAtLeast(0.0),
            durationSec = resolvedDur
        )
        if (controller.isPlaying) {
            startPositionPolling()
        }
    }

    private fun startPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = (controller.currentPosition / 1000.0).coerceAtLeast(0.0)
                    val dur = controller.duration
                    val durSec = if (dur > 0 && dur != androidx.media3.common.C.TIME_UNSET) {
                        dur / 1000.0
                    } else {
                        _playbackProgress.value.durationSec
                    }
                    _playbackProgress.value = PlaybackProgress(pos, durSec)
                }
                val fastPoll = _showLyricsModal.value || _isMobileSheetOpen.value
                delay(if (fastPoll) 100L else 240L)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = null
    }

    fun showNotice(message: String) {
        _statusNotice.value = message
        noticeJob?.cancel()
        noticeJob = viewModelScope.launch {
            delay(3000L)
            _statusNotice.value = null
        }
    }

    fun loadAndPlayTrack(track: TrackEntity, shouldPlay: Boolean) {
        val isUriPath = track.audioFilePath.startsWith("content://") || track.audioFilePath.startsWith("file://")
        val mediaUri: Uri = if (isUriPath) {
            val parsed = Uri.parse(track.audioFilePath)
            val accessible = runCatching {
                getApplication<Application>().contentResolver.openAssetFileDescriptor(parsed, "r")?.use { true } ?: false
            }.getOrDefault(false)
            if (!accessible) {
                showNotice(
                    if (isFaNow()) "فایل صوتی این قطعه روی گوشی پیدا نشد."
                    else "Audio file could not be accessed on device."
                )
                return
            }
            parsed
        } else {
            val file = File(track.audioFilePath)
            if (!file.exists()) {
                showNotice(
                    if (isFaNow()) "فایل صوتی این قطعه در حافظه پیدا نشد."
                    else "Audio file was not found in storage."
                )
                return
            }
            Uri.fromFile(file)
        }

        _activeTrackId.value = track.id
        _playbackProgress.value = PlaybackProgress(0.0, track.durationSec)

        val controller = mediaController ?: run {
            showNotice(
                if (isFaNow()) "پخش‌کننده در حال آماده‌سازی است؛ لحظه‌ای دیگر تلاش کنید."
                else "Player is getting ready, please try again."
            )
            return
        }

        val artworkUri: Uri? = track.coverFilePath?.let { path ->
            when {
                path.startsWith("content://") || path.startsWith("file://") -> Uri.parse(path)
                else -> File(path).takeIf { it.exists() }?.let { Uri.fromFile(it) }
            }
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album.ifBlank { "TO Music" })
            .apply { if (artworkUri != null) setArtworkUri(artworkUri) }
            .build()

        val item = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(mediaUri)
            .setMediaMetadata(metadata)
            .build()

        controller.setMediaItem(item)
        controller.volume = _volume.value
        controller.setPlaybackSpeed(_playbackRate.value)
        controller.prepare()
        if (shouldPlay) {
            controller.play()
            _isPlaying.value = true
            startPositionPolling()

            val playedAt = System.currentTimeMillis()
            val nextCount = track.playCount + 1
            viewModelScope.launch {
                repository.recordPlayback(track.id, playedAt, nextCount)
            }
        } else {
            controller.pause()
            _isPlaying.value = false
        }
    }

    fun togglePlay() {
        val state = uiState.value
        val active = state.activeTrack
        if (active == null) {
            val first = state.filteredTracks.firstOrNull() ?: state.allTracks.firstOrNull()
            if (first != null) loadAndPlayTrack(first, true)
            return
        }

        val controller = mediaController ?: return
        if (controller.currentMediaItem == null || controller.currentMediaItem?.mediaId != active.id) {
            loadAndPlayTrack(active, true)
            return
        }

        if (controller.isPlaying) {
            controller.pause()
            _isPlaying.value = false
        } else {
            controller.play()
            _isPlaying.value = true
            startPositionPolling()
        }
    }

    fun pauseForPreview() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
            _isPlaying.value = false
        }
    }

    fun handlePlayNext(fromTrackEnded: Boolean = false) {
        val controller = mediaController
        val state = uiState.value

        if (fromTrackEnded && _repeatMode.value == RepeatMode.ONE && controller != null) {
            controller.seekTo(0L)
            controller.play()
            _isPlaying.value = true
            _playbackProgress.value = _playbackProgress.value.copy(currentTimeSec = 0.0)
            return
        }

        val queue = _manualQueueIds.value
        if (queue.isNotEmpty()) {
            val nextId = queue.first()
            _manualQueueIds.value = queue.drop(1)
            val queuedTrack = state.allTracks.firstOrNull { it.id == nextId }
            if (queuedTrack != null) {
                loadAndPlayTrack(queuedTrack, true)
                return
            }
        }

        val pool = state.filteredTracks.ifEmpty { state.allTracks }
        if (pool.isEmpty()) return

        if (_shuffle.value && pool.size > 1) {
            val candidates = pool.filter { it.id != _activeTrackId.value }
            val pick = candidates.randomOrNull() ?: pool.first()
            loadAndPlayTrack(pick, true)
            return
        }

        val currentIdx = pool.indexOfFirst { it.id == _activeTrackId.value }
        val isLast = currentIdx == pool.lastIndex

        if (fromTrackEnded && isLast && _repeatMode.value == RepeatMode.OFF) {
            _isPlaying.value = false
            _playbackProgress.value = _playbackProgress.value.copy(currentTimeSec = 0.0)
            controller?.pause()
            controller?.seekTo(0L)
            return
        }

        val nextIdx = if (currentIdx >= 0) (currentIdx + 1) % pool.size else 0
        loadAndPlayTrack(pool[nextIdx], true)
    }

    fun handlePlayPrev() {
        val controller = mediaController
        if (controller != null && controller.currentPosition > 3000L) {
            controller.seekTo(0L)
            _playbackProgress.value = _playbackProgress.value.copy(currentTimeSec = 0.0)
            return
        }

        val state = uiState.value
        val pool = state.filteredTracks.ifEmpty { state.allTracks }
        if (pool.isEmpty()) return

        val currentIdx = pool.indexOfFirst { it.id == _activeTrackId.value }
        val prevIdx = if (currentIdx > 0) currentIdx - 1 else pool.lastIndex
        loadAndPlayTrack(pool[prevIdx], true)
    }

    fun seekTo(seconds: Double) {
        val clampedSec = seconds.coerceAtLeast(0.0)
        val clampedMs = (clampedSec * 1000.0).toLong()
        mediaController?.seekTo(clampedMs)
        _playbackProgress.value = _playbackProgress.value.copy(currentTimeSec = clampedSec)
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        mediaController?.volume = clamped
        viewModelScope.launch {
            preferences.saveVolume(clamped)
        }
    }

    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
            RepeatMode.OFF -> RepeatMode.ALL
        }
    }

    fun setSleepTimer(minutes: Int?) {
        val isFa = isFaNow()
        sleepJob?.cancel()
        if (minutes == null) {
            _sleepMinutesLeft.value = null
            showNotice(if (isFa) "تایمر خواب خاموش شد." else "Sleep timer turned off.")
            return
        }

        _sleepMinutesLeft.value = minutes
        showNotice(if (isFa) "تایمر خواب روی $minutes دقیقه تنظیم شد." else "Sleep timer set for $minutes min.")

        sleepJob = viewModelScope.launch {
            var remaining = minutes
            while (remaining > 0 && isActive) {
                delay(60_000L)
                remaining -= 1
                if (remaining <= 0) {
                    _sleepMinutesLeft.value = null
                    mediaController?.pause()
                    _isPlaying.value = false
                    showNotice(
                        if (isFaNow()) "تایمر خواب به پایان رسید و پخش متوقف شد."
                        else "Sleep timer ended. Playback paused."
                    )
                } else {
                    _sleepMinutesLeft.value = remaining
                }
            }
        }
    }

    fun importFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val isFa = isFaNow()
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val imported = repository.importFromUris(uris, isFa)
                if (imported.isEmpty()) {
                    showNotice(
                        if (isFa) "فایل صوتی معتبری پیدا نشد."
                        else "No valid audio files found."
                    )
                } else {
                    showNotice(
                        if (isFa) "${imported.size} آهنگ جدید اضافه شد."
                        else "${imported.size} tracks added to library."
                    )
                    if (_activeTrackId.value == null) {
                        loadAndPlayTrack(imported.first(), false)
                    }
                }
            } catch (_: Throwable) {
                showNotice(if (isFa) "افزودن فایل‌ها با خطا مواجه شد." else "Failed to import some files.")
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun loadStarterTracks() {
        val isFa = isFaNow()
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val added = repository.loadStarterTracks(isFa)
                if (_activeTrackId.value == null && added.isNotEmpty()) {
                    _activeTrackId.value = added.first().id
                    _playbackProgress.value = PlaybackProgress(0.0, added.first().durationSec)
                }
                showNotice(
                    if (isFa) "قطعات نمونه به کتابخانه اضافه شدند."
                    else "Sample tracks loaded."
                )
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun enterSelectionWithTrack(id: String) {
        _isSelectionMode.value = true
        _selectedIds.value = setOf(id)
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleTrackSelection(id: String) {
        val current = _selectedIds.value
        _selectedIds.value = if (id in current) current - id else current + id
    }

    fun selectAllFiltered() {
        _selectedIds.value = uiState.value.filteredTracks.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun bulkDelete(idsToDelete: List<String>) {
        if (idsToDelete.isEmpty()) return
        val isFa = isFaNow()
        val idSet = idsToDelete.toSet()
        viewModelScope.launch {
            try {
                val currentAll = uiState.value.allTracks
                val remaining = currentAll.filter { it.id !in idSet }
                repository.deleteTracks(idsToDelete)

                _manualQueueIds.value = _manualQueueIds.value.filter { it !in idSet }
                _selectedIds.value = emptySet()
                _isSelectionMode.value = false

                if (_activeTrackId.value in idSet) {
                    mediaController?.stop()
                    mediaController?.clearMediaItems()
                    _isPlaying.value = false
                    _playbackProgress.value = PlaybackProgress(0.0, 0.0)

                    if (remaining.isNotEmpty()) {
                        loadAndPlayTrack(remaining.first(), false)
                    } else {
                        _activeTrackId.value = null
                    }
                }
                showNotice(
                    if (isFa) "${idsToDelete.size} آهنگ از فهرست حذف شد."
                    else "${idsToDelete.size} tracks removed."
                )
            } catch (_: Throwable) {
                showNotice(if (isFa) "حذف آهنگ‌ها با خطا مواجه شد." else "Failed to remove tracks.")
            }
        }
    }

    fun bulkQueue(ids: List<String>) {
        if (ids.isEmpty()) return
        val isFa = isFaNow()
        val existing = _manualQueueIds.value.toSet()
        val added = ids.filter { it !in existing && it != _activeTrackId.value }
        _manualQueueIds.value = _manualQueueIds.value + added
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
        showNotice(
            if (isFa) "${ids.size} آهنگ به صف پخش اضافه شد."
            else "${ids.size} tracks added to queue."
        )
    }

    fun playNextInQueue(id: String) {
        val isFa = isFaNow()
        if (id == _activeTrackId.value) {
            showNotice(if (isFa) "این آهنگ هم‌اکنون در حال پخش است." else "This song is already playing.")
            return
        }
        _manualQueueIds.value = listOf(id) + _manualQueueIds.value.filter { it != id }
        showNotice(if (isFa) "به‌عنوان آهنگ بعدی در صف قرار گرفت." else "Queued to play next.")
    }

    fun clearManualQueue() {
        _manualQueueIds.value = emptyList()
        showNotice(if (isFaNow()) "صف پخش انتخابی پاک شد." else "Custom queue cleared.")
    }

    fun playCollection(collectionTracks: List<TrackEntity>) {
        if (collectionTracks.isEmpty()) return
        val first = collectionTracks.first()
        _manualQueueIds.value = collectionTracks.drop(1).map { it.id }
        loadAndPlayTrack(first, true)
        showNotice(
            if (isFaNow()) "پخش ${collectionTracks.size} آهنگ آغاز شد."
            else "Playing ${collectionTracks.size} tracks."
        )
    }

    fun setLibraryTab(tab: LibraryTab) {
        _libraryTab.value = tab
        _selectedAlbum.value = null
        _selectedArtist.value = null
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun selectAlbum(album: String?) {
        _selectedAlbum.value = album
    }

    fun selectArtist(artist: String?) {
        _selectedArtist.value = artist
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun setMobileSheetOpen(open: Boolean) {
        _isMobileSheetOpen.value = open
    }

    fun jumpToActive() {
        if (_activeTrackId.value == null) return
        _libraryTab.value = LibraryTab.ALL
        _selectedAlbum.value = null
        _selectedArtist.value = null
        if (_searchQuery.value.isNotBlank()) {
            _searchQuery.value = ""
        }
        _isMobileSheetOpen.value = false
        viewModelScope.launch {
            delay(80L)
            _jumpSignal.value += 1
        }
    }

    fun toggleFavorite(id: String) {
        val target = uiState.value.allTracks.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repository.toggleFavorite(id, !target.isFavorite)
        }
    }

    fun saveLyrics(trackId: String, plainLyrics: String?, syncedLyrics: String?) {
        viewModelScope.launch {
            repository.saveLyrics(trackId, plainLyrics, syncedLyrics)
        }
    }

    fun saveTrackDetails(id: String, title: String, artist: String, album: String) {
        val isFa = isFaNow()
        viewModelScope.launch {
            repository.updateMetadata(id, title, artist, album)
            showNotice(if (isFa) "مشخصات آهنگ به‌روزرسانی شد." else "Track details updated.")
        }
    }

    fun selectEqPreset(preset: EqPreset) {
        if (preset == EqPreset.CUSTOM) return
        _eqPreset.value = preset
        val gains = preset.gainsDb.toList()
        _eqGains.value = gains
        sendEqCommand(gains)
        persistEq()
    }

    fun changeEqBand(index: Int, db: Int) {
        _eqPreset.value = EqPreset.CUSTOM
        val updated = _eqGains.value.mapIndexed { idx, old -> if (idx == index) db else old }
        _eqGains.value = updated
        sendEqCommand(updated)
        persistEq()
    }

    fun changePlaybackRate(rate: Float) {
        val clamped = rate.coerceIn(0.75f, 1.5f)
        _playbackRate.value = clamped
        mediaController?.setPlaybackSpeed(clamped)
        persistEq()
    }

    private fun sendEqCommand(gains: List<Int>) {
        val controller = mediaController ?: return
        val args = Bundle().apply {
            putIntArray(PlaybackService.EXTRA_EQ_GAINS, gains.toIntArray())
        }
        controller.sendCustomCommand(
            SessionCommand(PlaybackService.ACTION_SET_EQ, Bundle.EMPTY),
            args
        )
    }

    private fun persistEq() {
        viewModelScope.launch {
            preferences.saveEqSettings(_eqPreset.value, _eqGains.value, _playbackRate.value)
        }
    }

    fun openLyricsModal(trackId: String?) {
        _modalTrackId.value = trackId
        _showLyricsModal.value = true
    }

    fun closeLyricsModal() {
        _showLyricsModal.value = false
    }

    fun openRingtoneModal(trackId: String?) {
        _modalTrackId.value = trackId
        _showRingtoneModal.value = true
    }

    fun closeRingtoneModal() {
        _showRingtoneModal.value = false
    }

    fun openEqModal() {
        _showEqModal.value = true
    }

    fun closeEqModal() {
        _showEqModal.value = false
    }

    fun openEditModal(trackId: String?) {
        _modalTrackId.value = trackId
        _showEditModal.value = true
    }

    fun closeEditModal() {
        _showEditModal.value = false
    }

    fun openSettingsModal() {
        _showSettingsModal.value = true
    }

    fun closeSettingsModal() {
        _showSettingsModal.value = false
    }

    private fun filterAndSortTracks(
        tracks: List<TrackEntity>,
        filter: LibraryFilterState,
        isFa: Boolean
    ): List<TrackEntity> {
        val unknownAlbum = if (isFa) "آلبوم ناشناس" else "Unknown Album"
        val unknownArtist = if (isFa) "هنرمند ناشناس" else "Unknown Artist"
        val query = filter.searchQuery.trim().lowercase()
        val filtered = tracks.filter { track ->
            when (filter.libraryTab) {
                LibraryTab.FAVORITES -> if (!track.isFavorite) return@filter false
                LibraryTab.RECENT -> if (track.lastPlayedAt == null) return@filter false
                LibraryTab.ALBUMS -> if (filter.selectedAlbum != null) {
                    val alb = track.album.trim().ifEmpty { unknownAlbum }
                    if (alb != filter.selectedAlbum) return@filter false
                }
                LibraryTab.ARTISTS -> if (filter.selectedArtist != null) {
                    val art = track.artist.trim().ifEmpty { unknownArtist }
                    if (art != filter.selectedArtist) return@filter false
                }
                LibraryTab.ALL -> Unit
            }
            if (query.isEmpty()) true
            else {
                track.title.lowercase().contains(query) ||
                    track.artist.lowercase().contains(query) ||
                    track.album.lowercase().contains(query)
            }
        }

        if (filter.libraryTab == LibraryTab.RECENT) {
            return filtered.sortedByDescending { it.lastPlayedAt ?: 0L }
        }

        return when (filter.sortMode) {
            SortMode.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortMode.ARTIST_ASC -> filtered.sortedBy { it.artist.lowercase() }
            SortMode.DURATION_DESC -> filtered.sortedByDescending { it.durationSec }
            SortMode.DATE_DESC -> filtered.sortedByDescending { it.addedAt }
        }
    }

    private fun buildAlbumGroups(tracks: List<TrackEntity>, searchQuery: String, isFa: Boolean): List<AlbumGroup> {
        val unknownAlbum = if (isFa) "آلبوم ناشناس" else "Unknown Album"
        val unknownArtist = if (isFa) "هنرمند ناشناس" else "Unknown Artist"
        val q = searchQuery.trim().lowercase()
        val grouped = tracks.groupBy { it.album.trim().ifEmpty { unknownAlbum } }
        return grouped.mapNotNull { (name, groupTracks) ->
            val artists = groupTracks.map { it.artist.trim() }.filter { it.isNotEmpty() }.distinct()
            val artistSummary = when {
                artists.isEmpty() -> unknownArtist
                artists.size <= 2 -> artists.joinToString(if (isFa) "، " else ", ")
                else -> if (isFa) "${artists.first()} و ${artists.size - 1} هنرمند دیگر" else "${artists.first()} & ${artists.size - 1} more"
            }
            if (q.isNotEmpty() && !name.lowercase().contains(q) && !artistSummary.lowercase().contains(q)) {
                return@mapNotNull null
            }
            AlbumGroup(
                name = name,
                artistSummary = artistSummary,
                tracks = groupTracks,
                totalDurationSec = groupTracks.sumOf { it.durationSec },
                coverFilePath = groupTracks.firstOrNull { !it.coverFilePath.isNullOrBlank() }?.coverFilePath,
                accentHue = groupTracks.firstOrNull()?.accentHue ?: 218
            )
        }.sortedWith(compareByDescending<AlbumGroup> { it.tracks.size }.thenBy { it.name })
    }

    private fun buildArtistGroups(tracks: List<TrackEntity>, searchQuery: String, isFa: Boolean): List<ArtistGroup> {
        val unknownArtist = if (isFa) "هنرمند ناشناس" else "Unknown Artist"
        val unknownAlbum = if (isFa) "آلبوم ناشناس" else "Unknown Album"
        val q = searchQuery.trim().lowercase()
        val grouped = tracks.groupBy { it.artist.trim().ifEmpty { unknownArtist } }
        return grouped.mapNotNull { (name, groupTracks) ->
            if (q.isNotEmpty() && !name.lowercase().contains(q)) {
                return@mapNotNull null
            }
            val albumCount = groupTracks.map { it.album.trim().ifEmpty { unknownAlbum } }.distinct().size
            ArtistGroup(
                name = name,
                tracks = groupTracks,
                albumCount = albumCount,
                totalDurationSec = groupTracks.sumOf { it.durationSec },
                coverFilePath = groupTracks.firstOrNull { !it.coverFilePath.isNullOrBlank() }?.coverFilePath,
                accentHue = groupTracks.firstOrNull()?.accentHue ?: 218
            )
        }.sortedWith(compareByDescending<ArtistGroup> { it.tracks.size }.thenBy { it.name })
    }

    private fun buildUpNext(
        activeTrack: TrackEntity?,
        allTracks: List<TrackEntity>,
        filteredTracks: List<TrackEntity>,
        manualQueueIds: List<String>
    ): List<TrackEntity> {
        if (activeTrack == null) return emptyList()
        val byId = allTracks.associateBy { it.id }
        val queued = manualQueueIds.mapNotNull { byId[it] }.filter { it.id != activeTrack.id }
        if (queued.isNotEmpty()) return queued

        val list = filteredTracks.ifEmpty { allTracks }
        val idx = list.indexOfFirst { it.id == activeTrack.id }
        if (idx == -1) return list.take(3)
        return (list.drop(idx + 1) + list.take(idx)).take(3)
    }

    override fun onCleared() {
        stopPositionPolling()
        mediaObserverJob?.cancel()
        noticeJob?.cancel()
        sleepJob?.cancel()
        connectivityObserver.unregister()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
