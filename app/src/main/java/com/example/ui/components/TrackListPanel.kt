package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AlbumGroup
import com.example.data.model.ArtistGroup
import com.example.data.model.LibraryTab
import com.example.data.model.SortMode
import com.example.data.model.TrackEntity
import com.example.data.model.formatDuration
import com.example.ui.theme.Indigo600
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalStrings
import com.example.ui.theme.Red400
import com.example.ui.theme.Red600
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrackListPanel(
    allTracks: List<TrackEntity>,
    tracks: List<TrackEntity>,
    albumGroups: List<AlbumGroup>,
    artistGroups: List<ArtistGroup>,
    activeTrackId: String?,
    isPlaying: Boolean,
    libraryTab: LibraryTab,
    selectedAlbum: String?,
    selectedArtist: String?,
    searchQuery: String,
    sortMode: SortMode,
    favoriteCount: Int,
    recentCount: Int,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    jumpSignal: Int,
    isImporting: Boolean,
    hasAudioPermission: Boolean,
    showDesktopJumpButton: Boolean,
    onLibraryTabChange: (LibraryTab) -> Unit,
    onSelectAlbum: (String?) -> Unit,
    onSelectArtist: (String?) -> Unit,
    onPlayCollection: (List<TrackEntity>) -> Unit,
    onSearchChange: (String) -> Unit,
    onSortChange: (SortMode) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectTrack: (TrackEntity) -> Unit,
    onEnterSelectionWithTrack: (String) -> Unit,
    onExitSelectionMode: () -> Unit,
    onToggleTrackSelection: (String) -> Unit,
    onSelectAllFiltered: () -> Unit,
    onClearSelection: () -> Unit,
    onBulkDelete: (List<String>) -> Unit,
    onBulkQueue: (List<String>) -> Unit,
    onPlayNextInQueue: (String) -> Unit,
    onOpenTrackLyrics: (String) -> Unit,
    onOpenTrackRingtone: (String) -> Unit,
    onOpenTrackEdit: (String) -> Unit,
    onJumpToActive: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onTriggerAddFiles: () -> Unit,
    onLoadStarterTracks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val s = LocalStrings.current
    val listState = rememberLazyListState()
    var highlightedId by remember { mutableStateOf<String?>(null) }
    var confirmingBulkDelete by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var actionTrack by remember { mutableStateOf<TrackEntity?>(null) }
    var confirmSingleDelete by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIds.size, isSelectionMode) {
        confirmingBulkDelete = false
    }

    LaunchedEffect(actionTrack?.id) {
        confirmSingleDelete = false
    }

    LaunchedEffect(jumpSignal) {
        if (jumpSignal > 0 && activeTrackId != null) {
            val idx = tracks.indexOfFirst { it.id == activeTrackId }
            if (idx >= 0) {
                listState.animateScrollToItem(idx)
                highlightedId = activeTrackId
                delay(1400L)
                highlightedId = null
            }
        }
    }

    val totalTrackCount = allTracks.size
    val isAlbumsOverview = libraryTab == LibraryTab.ALBUMS && selectedAlbum == null
    val isArtistsOverview = libraryTab == LibraryTab.ARTISTS && selectedArtist == null
    val isOverviewMode = isAlbumsOverview || isArtistsOverview
    val allFilteredSelected = tracks.isNotEmpty() && tracks.all { it.id in selectedIds }

    val searchPlaceholder = when {
        isAlbumsOverview -> s.searchAlbumsPlaceholder
        isArtistsOverview -> s.searchArtistsPlaceholder
        else -> s.searchTracksPlaceholder
    }

    val activeAlbumGroup = remember(selectedAlbum, albumGroups, tracks) {
        selectedAlbum?.let { name ->
            albumGroups.firstOrNull { it.name == name } ?: AlbumGroup(
                name = name,
                artistSummary = tracks.firstOrNull()?.artist.orEmpty(),
                tracks = tracks,
                totalDurationSec = tracks.sumOf { it.durationSec },
                coverFilePath = tracks.firstOrNull { !it.coverFilePath.isNullOrBlank() }?.coverFilePath,
                accentHue = tracks.firstOrNull()?.accentHue ?: 218
            )
        }
    }

    val activeArtistGroup = remember(selectedArtist, artistGroups, tracks) {
        selectedArtist?.let { name ->
            artistGroups.firstOrNull { it.name == name } ?: ArtistGroup(
                name = name,
                tracks = tracks,
                albumCount = tracks.map { it.album }.distinct().size,
                totalDurationSec = tracks.sumOf { it.durationSec },
                coverFilePath = tracks.firstOrNull { !it.coverFilePath.isNullOrBlank() }?.coverFilePath,
                accentHue = tracks.firstOrNull()?.accentHue ?: 218
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.glassSurface)
            .border(1.dp, colors.glassBorderStrong, RoundedCornerShape(26.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Panel Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = s.libraryTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textStrong
                        )
                        Text(text = "·", fontSize = 13.sp, color = colors.textMuted)
                        Text(
                            text = s.tracksCount(totalTrackCount),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }

                    if (!isOverviewMode && libraryTab != LibraryTab.RECENT && totalTrackCount > 0) {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.glassSurfaceElevated)
                                    .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                                    .clickable { showSortMenu = true }
                                    .padding(horizontal = 12.dp)
                                    .testTag("sort_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapVert,
                                    contentDescription = s.sortDesc,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = sortMode.label(s.isFa),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textSecondary
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(colors.modalBackground)
                            ) {
                                SortMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = mode.label(s.isFa),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (sortMode == mode) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (sortMode == mode) colors.textStrong else colors.textSecondary
                                                )
                                                if (sortMode == mode) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = colors.accentSkyText,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onSortChange(mode)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 5 Library Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassInput)
                        .padding(4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        Triple(LibraryTab.ALL, Icons.Rounded.MusicNote, totalTrackCount),
                        Triple(LibraryTab.FAVORITES, Icons.Rounded.Favorite, favoriteCount),
                        Triple(LibraryTab.ALBUMS, Icons.Rounded.Album, null),
                        Triple(LibraryTab.ARTISTS, Icons.Rounded.Mic, null),
                        Triple(LibraryTab.RECENT, Icons.Rounded.History, if (recentCount > 0) recentCount else null)
                    )
                    tabs.forEach { (tab, icon, count) ->
                        val active = libraryTab == tab
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) colors.glassSurfaceSelected else Color.Transparent)
                                .clickable { onLibraryTabChange(tab) }
                                .padding(horizontal = 12.dp)
                                .testTag("library_tab_${tab.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (active) colors.textStrong else colors.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = tab.label(s.isFa),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (active) colors.textStrong else colors.textMuted,
                                maxLines = 1
                            )
                            if (count != null) {
                                Text(
                                    text = "($count)",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = if (active) colors.textPrimary.copy(alpha = 0.85f) else colors.textSubtle
                                )
                            }
                        }
                    }
                }

                // Search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = searchPlaceholder,
                            fontSize = 12.sp,
                            color = colors.textSubtle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = s.clearSearchDesc,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentIndigo.copy(alpha = 0.65f),
                        unfocusedBorderColor = colors.glassBorder,
                        focusedContainerColor = colors.glassInput,
                        unfocusedContainerColor = colors.glassInput,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input")
                )

                // Device Audio Permission Prompt Card (only if audio permission hasn't been granted yet)
                AnimatedVisibility(visible = !hasAudioPermission && totalTrackCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.accentSky.copy(alpha = 0.12f))
                            .border(1.dp, colors.accentSky.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.permissionTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textStrong
                            )
                            Text(
                                text = s.permissionDesc,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = colors.textMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestAudioPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo600,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("grant_audio_permission_banner_btn")
                        ) {
                            Text(s.grantPermissionBtn, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Multi-selection header bar
                AnimatedVisibility(visible = isSelectionMode && !isOverviewMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.accentIndigo.copy(alpha = 0.14f))
                            .border(1.dp, colors.accentIndigo.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.selectedCountLabel(selectedIds.size),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.accentIndigoText
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (allFilteredSelected) s.deselectAll else s.selectAll,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.accentSkyText,
                                modifier = Modifier
                                    .clickable {
                                        if (allFilteredSelected) onClearSelection()
                                        else onSelectAllFiltered()
                                    }
                                    .padding(vertical = 4.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.glassSurfaceElevated)
                                    .clickable { onExitSelectionMode() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(s.close, fontSize = 12.sp, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.glassBorder)

            // Main Scrollable Body
            Box(modifier = Modifier.weight(1f)) {
                when {
                    totalTrackCount == 0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(colors.glassSurfaceElevated)
                                    .border(1.dp, colors.glassBorder, RoundedCornerShape(18.dp))
                            ) {
                                Icon(
                                    imageVector = if (!hasAudioPermission) Icons.Rounded.LibraryMusic else Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (!hasAudioPermission) s.permissionTitle else s.emptyLibraryTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textStrong,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (!hasAudioPermission) s.permissionDesc else s.emptyLibraryDesc,
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                color = colors.textMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!hasAudioPermission) {
                                    Button(
                                        onClick = onRequestAudioPermission,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Indigo600,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .heightIn(min = 46.dp)
                                            .testTag("grant_audio_permission_empty_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LibraryMusic,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(s.grantPermissionBtn, fontSize = 12.sp)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = onTriggerAddFiles,
                                        enabled = !isImporting,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.heightIn(min = 46.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FolderOpen,
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(s.browseManuallyBtn, fontSize = 12.sp, color = colors.textSecondary)
                                    }
                                }

                                OutlinedButton(
                                    onClick = onLoadStarterTracks,
                                    enabled = !isImporting,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.heightIn(min = 46.dp)
                                ) {
                                    Text(s.loadStarterTracksBtn, fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    }

                    isAlbumsOverview -> {
                        if (albumGroups.isEmpty()) {
                            EmptySearchState(
                                message = s.noAlbumsSearch,
                                buttonLabel = s.showAllAlbums,
                                onAction = { onSearchChange("") }
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 148.dp),
                                contentPadding = PaddingValues(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(albumGroups, key = { it.name }) { album ->
                                    val hasActive = album.tracks.any { it.id == activeTrackId }
                                    AlbumGridCard(
                                        album = album,
                                        hasActiveTrack = hasActive,
                                        onClick = { onSelectAlbum(album.name) }
                                    )
                                }
                            }
                        }
                    }

                    isArtistsOverview -> {
                        if (artistGroups.isEmpty()) {
                            EmptySearchState(
                                message = s.noArtistsSearch,
                                buttonLabel = s.showAllArtists,
                                onAction = { onSearchChange("") }
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(artistGroups, key = { it.name }) { artist ->
                                    val hasActive = artist.tracks.any { it.id == activeTrackId }
                                    ArtistRowCard(
                                        artist = artist,
                                        hasActiveTrack = hasActive,
                                        onClick = { onSelectArtist(artist.name) }
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 76.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (libraryTab == LibraryTab.ALBUMS && activeAlbumGroup != null) {
                                item(key = "album_header") {
                                    CollectionDrillHeader(
                                        title = activeAlbumGroup.name,
                                        subtitle = "${activeAlbumGroup.artistSummary} · ${s.songsCount(tracks.size)}",
                                        coverFilePath = activeAlbumGroup.coverFilePath,
                                        accentHue = activeAlbumGroup.accentHue,
                                        playLabel = s.playAlbum,
                                        showPlayButton = tracks.isNotEmpty(),
                                        backDesc = s.backToAlbums,
                                        onBack = { onSelectAlbum(null) },
                                        onPlayAll = { onPlayCollection(tracks) }
                                    )
                                }
                            }

                            if (libraryTab == LibraryTab.ARTISTS && activeArtistGroup != null) {
                                item(key = "artist_header") {
                                    CollectionDrillHeader(
                                        title = activeArtistGroup.name,
                                        subtitle = "${s.songsCount(tracks.size)} · ${s.albumsCount(activeArtistGroup.albumCount)}",
                                        coverFilePath = activeArtistGroup.coverFilePath,
                                        accentHue = activeArtistGroup.accentHue,
                                        playLabel = s.playAll,
                                        showPlayButton = tracks.isNotEmpty(),
                                        backDesc = s.backToArtists,
                                        onBack = { onSelectArtist(null) },
                                        onPlayAll = { onPlayCollection(tracks) }
                                    )
                                }
                            }

                            if (tracks.isEmpty()) {
                                item(key = "empty_filtered") {
                                    val msg = when {
                                        libraryTab == LibraryTab.FAVORITES && searchQuery.isBlank() -> s.noFavoritesYet
                                        libraryTab == LibraryTab.RECENT && searchQuery.isBlank() -> s.noRecentYet
                                        else -> s.noTracksSearch
                                    }
                                    EmptySearchState(
                                        message = msg,
                                        buttonLabel = if (searchQuery.isNotBlank()) s.showAllTracks else s.goToAllTracks,
                                        onAction = {
                                            if (searchQuery.isNotBlank()) onSearchChange("")
                                            else onLibraryTabChange(LibraryTab.ALL)
                                        }
                                    )
                                }
                            } else {
                                items(tracks, key = { it.id }) { track ->
                                    val isActive = track.id == activeTrackId
                                    val isSelected = track.id in selectedIds
                                    val isHighlighted = track.id == highlightedId

                                    TrackRowItem(
                                        track = track,
                                        isActive = isActive,
                                        isPlaying = isPlaying,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = isSelected,
                                        isHighlighted = isHighlighted,
                                        onClick = {
                                            if (isSelectionMode) onToggleTrackSelection(track.id)
                                            else onSelectTrack(track)
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) onEnterSelectionWithTrack(track.id)
                                        },
                                        onToggleFavorite = { onToggleFavorite(track.id) },
                                        onOpenMore = { actionTrack = track }
                                    )
                                }
                            }
                        }
                    }
                }

                // Desktop/Tablet floating Jump-to-Active pill
                if (showDesktopJumpButton && activeTrackId != null && !isSelectionMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .heightIn(min = 42.dp)
                            .clip(CircleShape)
                            .background(colors.modalBackground)
                            .border(1.dp, colors.accentSky.copy(alpha = 0.35f), CircleShape)
                            .clickable(onClick = onJumpToActive)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("desktop_jump_to_active_button")
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
                }
            }

            // Bulk Action Footer
            AnimatedVisibility(visible = isSelectionMode && selectedIds.isNotEmpty() && !isOverviewMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.modalBackground)
                        .border(1.dp, colors.glassBorder)
                        .padding(12.dp)
                ) {
                    if (confirmingBulkDelete) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = s.confirmBulkDelete(selectedIds.size),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.accentRedText,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onBulkDelete(selectedIds.toList())
                                        confirmingBulkDelete = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Red600, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(s.yesDeleteAll, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { confirmingBulkDelete = false },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Text(s.cancel, fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Red600.copy(alpha = 0.18f))
                                    .border(1.dp, Red400.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .clickable { confirmingBulkDelete = true }
                                    .padding(horizontal = 12.dp)
                                    .testTag("bulk_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = colors.accentRedText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = s.deleteCount(selectedIds.size),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accentRedText
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.glassSurfaceElevated)
                                    .border(1.dp, colors.glassBorderStrong, RoundedCornerShape(14.dp))
                                    .clickable { onBulkQueue(selectedIds.toList()) }
                                    .padding(horizontal = 12.dp)
                                    .testTag("bulk_queue_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = s.addToQueue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Per-Track Quick Action Sheet
    actionTrack?.let { target ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { actionTrack = null },
            sheetState = sheetState,
            containerColor = colors.sheetBackground,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TrackArtwork(
                            title = target.title,
                            coverFilePath = target.coverFilePath,
                            accentHue = target.accentHue,
                            size = ArtworkSize.SM
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = target.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textStrong,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = target.artist,
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = { actionTrack = null }, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = s.close,
                            tint = colors.textMuted
                        )
                    }
                }

                HorizontalDivider(color = colors.glassBorder)
                Spacer(modifier = Modifier.height(4.dp))

                ActionSheetRow(
                    label = s.playNext,
                    icon = Icons.Rounded.QueueMusic,
                    iconTint = colors.accentSkyText,
                    onClick = {
                        val id = target.id
                        actionTrack = null
                        onPlayNextInQueue(id)
                    }
                )
                ActionSheetRow(
                    label = if (target.isFavorite) s.removeFromFavorites else s.addToFavorites,
                    icon = if (target.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    iconTint = colors.accentRose,
                    onClick = {
                        val id = target.id
                        actionTrack = null
                        onToggleFavorite(id)
                    }
                )
                ActionSheetRow(
                    label = s.lyrics,
                    icon = Icons.Rounded.Description,
                    iconTint = colors.accentIndigoText,
                    onClick = {
                        val id = target.id
                        actionTrack = null
                        onOpenTrackLyrics(id)
                    }
                )
                ActionSheetRow(
                    label = s.ringtoneAndAlarm,
                    icon = Icons.Rounded.NotificationsActive,
                    iconTint = colors.accentEmerald,
                    onClick = {
                        val id = target.id
                        actionTrack = null
                        onOpenTrackRingtone(id)
                    }
                )
                ActionSheetRow(
                    label = s.editDetails,
                    icon = Icons.Rounded.Edit,
                    iconTint = colors.textSecondary,
                    onClick = {
                        val id = target.id
                        actionTrack = null
                        onOpenTrackEdit(id)
                    }
                )
                ActionSheetRow(
                    label = s.multiSelect,
                    icon = Icons.Rounded.CheckBox,
                    iconTint = colors.textSecondary,
                    onClick = {
                        val id = target.id
                        actionTrack = null
                        onEnterSelectionWithTrack(id)
                    }
                )

                if (confirmSingleDelete) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Red600.copy(alpha = 0.14f))
                            .border(1.dp, Red400.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.removeFromLibraryConfirm, fontSize = 12.sp, color = colors.accentRedText)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val id = target.id
                                    actionTrack = null
                                    onBulkDelete(listOf(id))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Red600, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 38.dp)
                            ) {
                                Text(s.yesRemove, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { confirmSingleDelete = false },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 38.dp)
                            ) {
                                Text(s.cancel, fontSize = 12.sp, color = colors.textSecondary)
                            }
                        }
                    }
                } else {
                    ActionSheetRow(
                        label = s.removeSong,
                        icon = Icons.Rounded.DeleteOutline,
                        iconTint = colors.accentRed,
                        textColor = colors.accentRedText,
                        onClick = { confirmSingleDelete = true }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackRowItem(
    track: TrackEntity,
    isActive: Boolean,
    isPlaying: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenMore: () -> Unit
) {
    val colors = LocalAppColors.current
    val s = LocalStrings.current

    val bgColor = when {
        isHighlighted -> colors.accentSky.copy(alpha = 0.22f)
        isSelected -> colors.accentIndigo.copy(alpha = 0.16f)
        isActive -> colors.glassSurfaceSelected
        else -> Color.Transparent
    }
    val borderColor = when {
        isHighlighted -> colors.accentSky.copy(alpha = 0.75f)
        isSelected -> colors.accentIndigo.copy(alpha = 0.45f)
        isActive -> colors.glassBorderStrong
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("track_row_${track.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                    contentDescription = if (isSelected) s.selectedState else s.unselectedState,
                    tint = if (isSelected) colors.accentIndigo else colors.textSubtle,
                    modifier = Modifier.size(22.dp)
                )
            }

            TrackArtwork(
                title = track.title,
                coverFilePath = track.coverFilePath,
                accentHue = track.accentHue,
                size = ArtworkSize.SM,
                isPlaying = isActive && isPlaying
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isActive) colors.accentSkyText else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(track.artist)
                        if (track.album.isNotBlank()) append(" · ${track.album}")
                    },
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (isActive) {
                MiniEqualizer(
                    isPlaying = isPlaying,
                    accentHue = track.accentHue,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = formatDuration(track.durationSec),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (!isSelectionMode) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (track.isFavorite) s.removeFromFavorites else s.addToFavorites,
                        tint = if (track.isFavorite) colors.accentRose else colors.textSubtle,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenMore,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("track_more_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = s.moreOptionsFor(track.title),
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumGridCard(
    album: AlbumGroup,
    hasActiveTrack: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val s = LocalStrings.current
    val shape = RoundedCornerShape(18.dp)
    val coverModel = remember(album.coverFilePath) { resolveCoverModel(album.coverFilePath) }
    var imageError by remember(album.coverFilePath) { mutableStateOf(false) }

    val glow = Color.hsl(album.accentHue.toFloat(), 0.78f, if (colors.isDark) 0.62f else 0.66f)
        .copy(alpha = if (colors.isDark) 0.42f else 0.34f)
    val base = Color.hsl(album.accentHue.toFloat(), 0.40f, if (colors.isDark) 0.16f else 0.85f)
    val baseEnd = if (colors.isDark) Color(0xFF09090B) else Color(0xFFDDE5F4)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (hasActiveTrack) colors.accentSky.copy(alpha = 0.14f) else colors.glassSurfaceElevated)
            .border(
                1.dp,
                if (hasActiveTrack) colors.accentSky.copy(alpha = 0.35f) else colors.glassBorder,
                shape
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(base, baseEnd),
                        start = Offset.Zero,
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(glow, Color.Transparent)))
            )
            Icon(
                imageVector = Icons.Rounded.Album,
                contentDescription = album.name,
                tint = if (colors.isDark) Color.White.copy(alpha = 0.78f) else Color(0xFF1E293B).copy(alpha = 0.78f),
                modifier = Modifier.size(42.dp)
            )
            if (coverModel != null && !imageError) {
                AsyncImage(
                    model = coverModel,
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    onError = { imageError = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artistSummary,
            fontSize = 11.sp,
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${s.songsCount(album.tracks.size)} · ${formatDuration(album.totalDurationSec)}",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = colors.textSubtle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistRowCard(
    artist: ArtistGroup,
    hasActiveTrack: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val s = LocalStrings.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .clip(shape)
            .background(if (hasActiveTrack) colors.accentSky.copy(alpha = 0.14f) else colors.glassSurfaceElevated)
            .border(
                1.dp,
                if (hasActiveTrack) colors.accentSky.copy(alpha = 0.35f) else colors.glassBorder,
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrackArtwork(
                title = artist.name,
                coverFilePath = artist.coverFilePath,
                accentHue = artist.accentHue,
                size = ArtworkSize.SM
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${s.songsCount(artist.tracks.size)} · ${s.albumsCount(artist.albumCount)} · ${formatDuration(artist.totalDurationSec)}",
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = null,
            tint = colors.textSubtle,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CollectionDrillHeader(
    title: String,
    subtitle: String,
    coverFilePath: String?,
    accentHue: Int,
    playLabel: String,
    showPlayButton: Boolean,
    backDesc: String,
    onBack: () -> Unit,
    onPlayAll: () -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.glassSurfaceElevated)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(18.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.glassSurface)
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = backDesc,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            TrackArtwork(
                title = title,
                coverFilePath = coverFilePath,
                accentHue = accentHue,
                size = ArtworkSize.SM
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showPlayButton) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primaryBtnBg)
                    .clickable(onClick = onPlayAll)
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = colors.primaryBtnFg,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = playLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryBtnFg
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    message: String,
    buttonLabel: String,
    onAction: () -> Unit
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onAction,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.heightIn(min = 42.dp)
        ) {
            Text(text = buttonLabel, fontSize = 12.sp, color = colors.textSecondary)
        }
    }
}

@Composable
private fun ActionSheetRow(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    textColor: Color = LocalAppColors.current.textSecondary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
