import React, { useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import {
  ArrowRight,
  ArrowUpDown,
  BellRing,
  Check,
  CheckSquare,
  ChevronLeft,
  Clock,
  Disc3,
  Edit3,
  FileText,
  Heart,
  ListPlus,
  LocateFixed,
  Mic2,
  MoreVertical,
  Music2,
  Play,
  Plus,
  Search,
  Square,
  Trash2,
  X,
} from 'lucide-react';
import { LibraryTab, SortMode, TrackMeta } from '../types/music';
import { formatDuration } from '../utils/musicStorage';
import { MiniEqualizer } from './LiveVisualizer';
import { TrackArtwork } from './TrackArtwork';

interface TrackListPanelProps {
  allTracks: TrackMeta[];
  tracks: TrackMeta[];
  totalTrackCount: number;
  activeTrackId: string | null;
  isPlaying: boolean;
  libraryTab: LibraryTab;
  selectedAlbum: string | null;
  selectedArtist: string | null;
  searchQuery: string;
  sortMode: SortMode;
  favoriteCount: number;
  recentCount: number;
  isSelectionMode: boolean;
  selectedIds: Set<string>;
  jumpSignal: number;
  onLibraryTabChange: (tab: LibraryTab) => void;
  onSelectAlbum: (album: string | null) => void;
  onSelectArtist: (artist: string | null) => void;
  onPlayCollection: (tracks: TrackMeta[]) => void;
  onSearchChange: (q: string) => void;
  onSortChange: (mode: SortMode) => void;
  onToggleFavorite: (id: string) => void;
  onSelectTrack: (track: TrackMeta) => void;
  onEnterSelectionWithTrack: (id: string) => void;
  onExitSelectionMode: () => void;
  onToggleTrackSelection: (id: string) => void;
  onSelectAllFiltered: () => void;
  onClearSelection: () => void;
  onBulkDelete: (ids: string[]) => void;
  onBulkQueue: (ids: string[]) => void;
  onPlayNextInQueue: (id: string) => void;
  onOpenTrackLyrics: (id: string) => void;
  onOpenTrackRingtone: (id: string) => void;
  onOpenTrackEdit: (id: string) => void;
  onJumpToActive: () => void;
  onTriggerAddFiles: () => void;
  onLoadStarterTracks: () => void;
  isImporting: boolean;
}

interface AlbumGroup {
  name: string;
  artistSummary: string;
  tracks: TrackMeta[];
  totalDuration: number;
  coverDataUrl?: string;
  accentHue: number;
}

interface ArtistGroup {
  name: string;
  tracks: TrackMeta[];
  albumCount: number;
  totalDuration: number;
  coverDataUrl?: string;
  accentHue: number;
}

const SORT_LABELS: Record<SortMode, string> = {
  [SortMode.DateDesc]: 'جدیدترین',
  [SortMode.TitleAsc]: 'نام آهنگ',
  [SortMode.ArtistAsc]: 'هنرمند',
  [SortMode.DurationDesc]: 'طولانی‌ترین',
};

export const TrackListPanel: React.FC<TrackListPanelProps> = ({
  allTracks,
  tracks,
  totalTrackCount,
  activeTrackId,
  isPlaying,
  libraryTab,
  selectedAlbum,
  selectedArtist,
  searchQuery,
  sortMode,
  favoriteCount,
  recentCount,
  isSelectionMode,
  selectedIds,
  jumpSignal,
  onLibraryTabChange,
  onSelectAlbum,
  onSelectArtist,
  onPlayCollection,
  onSearchChange,
  onSortChange,
  onToggleFavorite,
  onSelectTrack,
  onEnterSelectionWithTrack,
  onExitSelectionMode,
  onToggleTrackSelection,
  onSelectAllFiltered,
  onClearSelection,
  onBulkDelete,
  onBulkQueue,
  onPlayNextInQueue,
  onOpenTrackLyrics,
  onOpenTrackRingtone,
  onOpenTrackEdit,
  onJumpToActive,
  onTriggerAddFiles,
  onLoadStarterTracks,
  isImporting,
}) => {
  const rowRefs = useRef<Map<string, HTMLDivElement>>(new Map());
  const longPressTimerRef = useRef<number | null>(null);
  const longPressTriggeredRef = useRef(false);
  const pointerOriginRef = useRef<{ x: number; y: number } | null>(null);

  const [highlightedId, setHighlightedId] = useState<string | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [showSortMenu, setShowSortMenu] = useState(false);
  const [actionTrack, setActionTrack] = useState<TrackMeta | null>(null);
  const [confirmSingleDelete, setConfirmSingleDelete] = useState(false);

  useEffect(() => {
    setConfirmingDelete(false);
  }, [selectedIds.size, isSelectionMode]);

  useEffect(() => {
    setConfirmSingleDelete(false);
  }, [actionTrack?.id]);

  useEffect(() => {
    if (jumpSignal === 0 || !activeTrackId) return;
    const el = rowRefs.current.get(activeTrackId);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      setHighlightedId(activeTrackId);
      const timer = window.setTimeout(() => setHighlightedId(null), 1400);
      return () => window.clearTimeout(timer);
    }
    return undefined;
  }, [jumpSignal, activeTrackId]);

  const clearLongPress = () => {
    if (longPressTimerRef.current !== null) {
      window.clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
    }
    pointerOriginRef.current = null;
  };

  const handlePointerDown = (e: React.PointerEvent, trackId: string) => {
    if (isSelectionMode) return;
    longPressTriggeredRef.current = false;
    pointerOriginRef.current = { x: e.clientX, y: e.clientY };
    if (longPressTimerRef.current !== null) {
      window.clearTimeout(longPressTimerRef.current);
    }
    longPressTimerRef.current = window.setTimeout(() => {
      longPressTriggeredRef.current = true;
      onEnterSelectionWithTrack(trackId);
    }, 450);
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!pointerOriginRef.current || longPressTimerRef.current === null) return;
    const dx = Math.abs(e.clientX - pointerOriginRef.current.x);
    const dy = Math.abs(e.clientY - pointerOriginRef.current.y);
    if (dx > 10 || dy > 10) {
      clearLongPress();
    }
  };

  const handleRowClick = (track: TrackMeta) => {
    if (longPressTriggeredRef.current) {
      longPressTriggeredRef.current = false;
      return;
    }
    if (isSelectionMode) {
      onToggleTrackSelection(track.id);
    } else {
      onSelectTrack(track);
    }
  };

  const albumGroups = useMemo<AlbumGroup[]>(() => {
    const map = new Map<string, TrackMeta[]>();
    for (const track of allTracks) {
      const key = (track.album || '').trim() || 'آلبوم ناشناس';
      const arr = map.get(key);
      if (arr) arr.push(track);
      else map.set(key, [track]);
    }
    const q = searchQuery.trim().toLowerCase();
    const list: AlbumGroup[] = [];
    for (const [name, groupTracks] of map.entries()) {
      const artists = Array.from(
        new Set(groupTracks.map((t) => (t.artist || '').trim()).filter(Boolean))
      );
      const artistSummary =
        artists.length === 0
          ? 'هنرمند ناشناس'
          : artists.length <= 2
            ? artists.join('، ')
            : `${artists[0]} و ${artists.length - 1} هنرمند دیگر`;
      if (
        q &&
        !name.toLowerCase().includes(q) &&
        !artistSummary.toLowerCase().includes(q)
      ) {
        continue;
      }
      const totalDuration = groupTracks.reduce((sum, t) => sum + (t.duration || 0), 0);
      const coverDataUrl = groupTracks.find((t) => Boolean(t.coverDataUrl))?.coverDataUrl;
      list.push({
        name,
        artistSummary,
        tracks: groupTracks,
        totalDuration,
        coverDataUrl,
        accentHue: groupTracks[0]?.accentHue ?? 218,
      });
    }
    list.sort((a, b) => b.tracks.length - a.tracks.length || a.name.localeCompare(b.name, 'fa'));
    return list;
  }, [allTracks, searchQuery]);

  const artistGroups = useMemo<ArtistGroup[]>(() => {
    const map = new Map<string, TrackMeta[]>();
    for (const track of allTracks) {
      const key = (track.artist || '').trim() || 'هنرمند ناشناس';
      const arr = map.get(key);
      if (arr) arr.push(track);
      else map.set(key, [track]);
    }
    const q = searchQuery.trim().toLowerCase();
    const list: ArtistGroup[] = [];
    for (const [name, groupTracks] of map.entries()) {
      if (q && !name.toLowerCase().includes(q)) {
        continue;
      }
      const albumCount = new Set(
        groupTracks.map((t) => (t.album || '').trim() || 'آلبوم ناشناس')
      ).size;
      const totalDuration = groupTracks.reduce((sum, t) => sum + (t.duration || 0), 0);
      const coverDataUrl = groupTracks.find((t) => Boolean(t.coverDataUrl))?.coverDataUrl;
      list.push({
        name,
        tracks: groupTracks,
        albumCount,
        totalDuration,
        coverDataUrl,
        accentHue: groupTracks[0]?.accentHue ?? 218,
      });
    }
    list.sort((a, b) => b.tracks.length - a.tracks.length || a.name.localeCompare(b.name, 'fa'));
    return list;
  }, [allTracks, searchQuery]);

  const isAlbumsOverview = libraryTab === LibraryTab.Albums && selectedAlbum === null;
  const isArtistsOverview = libraryTab === LibraryTab.Artists && selectedArtist === null;
  const isOverviewMode = isAlbumsOverview || isArtistsOverview;

  const allFilteredSelected =
    tracks.length > 0 && tracks.every((track) => selectedIds.has(track.id));

  const searchPlaceholder = isAlbumsOverview
    ? 'جستجو در آلبوم‌ها...'
    : isArtistsOverview
      ? 'جستجو در نام هنرمندان...'
      : 'جستجو در نام آهنگ یا خواننده...';

  const sectionTabs = [
    {
      id: LibraryTab.All,
      label: 'آهنگ‌ها',
      count: totalTrackCount,
      icon: Music2,
    },
    {
      id: LibraryTab.Favorites,
      label: 'علاقه‌مندی',
      count: favoriteCount,
      icon: Heart,
    },
    {
      id: LibraryTab.Albums,
      label: 'آلبوم‌ها',
      count: null,
      icon: Disc3,
    },
    {
      id: LibraryTab.Artists,
      label: 'هنرمندان',
      count: null,
      icon: Mic2,
    },
    {
      id: LibraryTab.Recent,
      label: 'پخش اخیر',
      count: recentCount > 0 ? recentCount : null,
      icon: Clock,
    },
  ];

  const activeAlbumGroup = useMemo(
    () =>
      selectedAlbum
        ? albumGroups.find((a) => a.name === selectedAlbum) || {
            name: selectedAlbum,
            artistSummary: tracks[0]?.artist || '',
            tracks,
            totalDuration: tracks.reduce((s, t) => s + (t.duration || 0), 0),
            coverDataUrl: tracks.find((t) => Boolean(t.coverDataUrl))?.coverDataUrl,
            accentHue: tracks[0]?.accentHue ?? 218,
          }
        : null,
    [albumGroups, selectedAlbum, tracks]
  );

  const activeArtistGroup = useMemo(
    () =>
      selectedArtist
        ? artistGroups.find((a) => a.name === selectedArtist) || {
            name: selectedArtist,
            tracks,
            albumCount: new Set(tracks.map((t) => t.album)).size,
            totalDuration: tracks.reduce((s, t) => s + (t.duration || 0), 0),
            coverDataUrl: tracks.find((t) => Boolean(t.coverDataUrl))?.coverDataUrl,
            accentHue: tracks[0]?.accentHue ?? 218,
          }
        : null,
    [artistGroups, selectedArtist, tracks]
  );

  return (
    <section className="relative flex h-full flex-col rounded-3xl border border-white/[0.09] bg-white/[0.04] backdrop-blur-xl">
      <div className="border-b border-white/[0.08] p-4 sm:p-5">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-xs text-zinc-400">
            <h2 className="text-sm font-semibold text-white">کتابخانه موسیقی</h2>
            <span aria-hidden="true">·</span>
            <span className="font-mono tabular-nums">{totalTrackCount} قطعه</span>
          </div>

          {!isOverviewMode && libraryTab !== LibraryTab.Recent && totalTrackCount > 0 && (
            <div className="relative">
              <button
                type="button"
                onClick={() => setShowSortMenu((v) => !v)}
                aria-label="مرتب‌سازی"
                className="inline-flex min-h-[38px] items-center gap-1.5 rounded-xl border border-white/10 bg-white/[0.05] px-3 py-1.5 text-xs font-medium text-zinc-200 hover:bg-white/[0.11] transition whitespace-nowrap"
              >
                <ArrowUpDown className="h-3.5 w-3.5 text-zinc-400" aria-hidden="true" />
                <span>{SORT_LABELS[sortMode]}</span>
              </button>

              <AnimatePresence>
                {showSortMenu && (
                  <motion.div
                    initial={{ opacity: 0, y: 6, scale: 0.96 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 6, scale: 0.96 }}
                    transition={{ duration: 0.14 }}
                    className="absolute end-0 top-11 z-30 w-44 rounded-2xl border border-white/15 bg-zinc-900/95 p-1.5 shadow-2xl backdrop-blur-xl"
                  >
                    {(Object.keys(SORT_LABELS) as SortMode[]).map((mode) => (
                      <button
                        key={mode}
                        type="button"
                        onClick={() => {
                          onSortChange(mode);
                          setShowSortMenu(false);
                        }}
                        className={`flex min-h-[38px] w-full items-center justify-between rounded-xl px-3 text-xs transition ${
                          sortMode === mode
                            ? 'bg-white/12 font-semibold text-white'
                            : 'text-zinc-300 hover:bg-white/10'
                        }`}
                      >
                        <span>{SORT_LABELS[mode]}</span>
                        {sortMode === mode && (
                          <Check className="h-3.5 w-3.5 text-sky-300" aria-hidden="true" />
                        )}
                      </button>
                    ))}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          )}
        </div>

        <div className="mt-3 flex items-center gap-1 overflow-x-auto no-scrollbar rounded-xl bg-black/30 p-1">
          {sectionTabs.map((tab) => {
            const Icon = tab.icon;
            const active = libraryTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => onLibraryTabChange(tab.id)}
                className={`inline-flex min-h-[38px] shrink-0 items-center justify-center gap-1.5 rounded-lg px-3 text-xs font-medium transition whitespace-nowrap sm:flex-1 ${
                  active
                    ? 'bg-white/14 text-white shadow-sm'
                    : 'text-zinc-400 hover:text-zinc-200'
                }`}
              >
                <Icon className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                <span>{tab.label}</span>
                {tab.count !== null && (
                  <span className="font-mono text-[11px] tabular-nums opacity-75">
                    ({tab.count})
                  </span>
                )}
              </button>
            );
          })}
        </div>

        <div className="relative mt-2.5">
          <Search
            className="pointer-events-none absolute start-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400"
            aria-hidden="true"
          />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder={searchPlaceholder}
            className="min-h-[42px] w-full rounded-xl border border-white/10 bg-black/30 py-2 pe-9 ps-9 text-xs text-zinc-100 placeholder:text-zinc-500 focus:border-indigo-400/60 focus:outline-none"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => onSearchChange('')}
              aria-label="پاک کردن جستجو"
              className="absolute end-1.5 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-lg text-zinc-400 hover:bg-white/10 hover:text-white"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>

        <AnimatePresence>
          {isSelectionMode && !isOverviewMode && (
            <motion.div
              initial={{ opacity: 0, y: -6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.16 }}
              className="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-xl border border-indigo-400/25 bg-indigo-500/12 px-3 py-2 text-xs"
            >
              <span className="font-medium text-indigo-100 tabular-nums">
                {selectedIds.size} آهنگ انتخاب‌شده
              </span>
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={allFilteredSelected ? onClearSelection : onSelectAllFiltered}
                  className="font-medium text-sky-300 hover:text-sky-200 whitespace-nowrap"
                >
                  {allFilteredSelected ? 'لغو انتخاب همه' : 'انتخاب همه'}
                </button>
                <button
                  type="button"
                  onClick={onExitSelectionMode}
                  className="inline-flex items-center gap-1 rounded-lg bg-white/10 px-2.5 py-1 font-medium text-zinc-200 hover:bg-white/20 whitespace-nowrap"
                >
                  <X className="h-3.5 w-3.5" aria-hidden="true" />
                  <span>بستن</span>
                </button>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      <div className="flex-1 overflow-y-auto px-2.5 py-2.5 pb-16 sm:px-3.5">
        {totalTrackCount === 0 ? (
          <div className="flex h-full min-h-[280px] flex-col items-center justify-center px-6 py-12 text-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl border border-white/10 bg-white/[0.05] text-zinc-400">
              <Music2 className="h-7 w-7" aria-hidden="true" />
            </div>
            <h3 className="mt-4 text-base font-semibold text-white">کتابخانه موسیقی خالی است</h3>
            <p className="mt-1.5 max-w-xs text-xs leading-6 text-zinc-400">
              آهنگ‌های خود را از گوشی انتخاب کنید تا برای پخش آفلاین ذخیره شوند، یا قطعات نمونه را بارگذاری کنید.
            </p>
            <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
              <button
                type="button"
                disabled={isImporting}
                onClick={onTriggerAddFiles}
                className="inline-flex min-h-[44px] items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-medium text-white hover:bg-indigo-500 active:scale-[0.98] transition whitespace-nowrap"
              >
                <Plus className="h-4 w-4" aria-hidden="true" />
                <span>افزودن آهنگ از گوشی</span>
              </button>
              <button
                type="button"
                disabled={isImporting}
                onClick={onLoadStarterTracks}
                className="inline-flex min-h-[44px] items-center gap-2 rounded-xl border border-white/15 bg-white/[0.06] px-4 py-2 text-xs font-medium text-zinc-200 hover:bg-white/[0.11] active:scale-[0.98] transition whitespace-nowrap"
              >
                <span>بارگذاری آهنگ‌های نمونه</span>
              </button>
            </div>
          </div>
        ) : isAlbumsOverview ? (
          albumGroups.length === 0 ? (
            <div className="flex min-h-[220px] flex-col items-center justify-center px-4 py-10 text-center">
              <p className="text-sm font-medium text-zinc-300">آلبومی با این جستجو پیدا نشد</p>
              <button
                type="button"
                onClick={() => onSearchChange('')}
                className="mt-3 min-h-[40px] rounded-xl border border-white/12 bg-white/[0.06] px-4 py-2 text-xs font-medium text-zinc-200 hover:bg-white/[0.11]"
              >
                نمایش همه آلبوم‌ها
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3">
              {albumGroups.map((album) => {
                const hasActiveTrack = album.tracks.some((t) => t.id === activeTrackId);
                return (
                  <button
                    key={album.name}
                    type="button"
                    onClick={() => onSelectAlbum(album.name)}
                    className={`group flex flex-col rounded-2xl border p-2.5 text-start transition active:scale-[0.98] ${
                      hasActiveTrack
                        ? 'border-sky-400/35 bg-sky-500/10'
                        : 'border-white/[0.08] bg-white/[0.03] hover:bg-white/[0.07]'
                    }`}
                  >
                    <div className="relative aspect-square w-full overflow-hidden rounded-xl border border-white/10 bg-zinc-900">
                      {album.coverDataUrl ? (
                        <img
                          src={album.coverDataUrl}
                          alt={album.name}
                          referrerPolicy="no-referrer"
                          className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                        />
                      ) : (
                        <div
                          className="flex h-full w-full items-center justify-center"
                          style={{
                            background: `radial-gradient(circle at 25% 20%, hsla(${album.accentHue}, 82%, 62%, 0.42), transparent 62%), linear-gradient(145deg, hsla(${album.accentHue}, 45%, 16%, 0.95), rgba(9, 9, 11, 0.98))`,
                          }}
                        >
                          <Disc3 className="h-10 w-10 text-white/75" aria-hidden="true" />
                        </div>
                      )}
                    </div>
                    <p className="mt-2.5 truncate text-xs font-semibold text-white">
                      {album.name}
                    </p>
                    <p className="mt-0.5 truncate text-[11px] text-zinc-400">
                      {album.artistSummary}
                    </p>
                    <p className="mt-1 truncate font-mono text-[11px] tabular-nums text-zinc-500">
                      {album.tracks.length} آهنگ · {formatDuration(album.totalDuration)}
                    </p>
                  </button>
                );
              })}
            </div>
          )
        ) : isArtistsOverview ? (
          artistGroups.length === 0 ? (
            <div className="flex min-h-[220px] flex-col items-center justify-center px-4 py-10 text-center">
              <p className="text-sm font-medium text-zinc-300">هنرمندی با این جستجو پیدا نشد</p>
              <button
                type="button"
                onClick={() => onSearchChange('')}
                className="mt-3 min-h-[40px] rounded-xl border border-white/12 bg-white/[0.06] px-4 py-2 text-xs font-medium text-zinc-200 hover:bg-white/[0.11]"
              >
                نمایش همه هنرمندان
              </button>
            </div>
          ) : (
            <div className="space-y-1.5">
              {artistGroups.map((artist) => {
                const hasActiveTrack = artist.tracks.some((t) => t.id === activeTrackId);
                return (
                  <button
                    key={artist.name}
                    type="button"
                    onClick={() => onSelectArtist(artist.name)}
                    className={`flex min-h-[64px] w-full items-center justify-between gap-3 rounded-2xl border px-3 py-2 text-start transition active:scale-[0.99] ${
                      hasActiveTrack
                        ? 'border-sky-400/35 bg-sky-500/10'
                        : 'border-white/[0.06] bg-white/[0.03] hover:bg-white/[0.07]'
                    }`}
                  >
                    <div className="flex min-w-0 flex-1 items-center gap-3">
                      <TrackArtwork
                        title={artist.name}
                        coverDataUrl={artist.coverDataUrl}
                        accentHue={artist.accentHue}
                        size="sm"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-semibold text-white">
                          {artist.name}
                        </p>
                        <p className="mt-0.5 truncate text-xs text-zinc-400 tabular-nums">
                          {artist.tracks.length} آهنگ · {artist.albumCount} آلبوم ·{' '}
                          <span className="font-mono">{formatDuration(artist.totalDuration)}</span>
                        </p>
                      </div>
                    </div>
                    <ChevronLeft className="h-4 w-4 shrink-0 text-zinc-500" aria-hidden="true" />
                  </button>
                );
              })}
            </div>
          )
        ) : (
          <>
            {libraryTab === LibraryTab.Albums && activeAlbumGroup && (
              <div className="mb-3 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-white/10 bg-white/[0.05] p-3">
                <div className="flex min-w-0 items-center gap-3">
                  <button
                    type="button"
                    onClick={() => onSelectAlbum(null)}
                    aria-label="بازگشت به آلبوم‌ها"
                    className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-white/10 bg-white/[0.06] text-zinc-200 hover:bg-white/[0.12]"
                  >
                    <ArrowRight className="h-4 w-4" />
                  </button>
                  <TrackArtwork
                    title={activeAlbumGroup.name}
                    coverDataUrl={activeAlbumGroup.coverDataUrl}
                    accentHue={activeAlbumGroup.accentHue}
                    size="sm"
                  />
                  <div className="min-w-0">
                    <h3 className="truncate text-sm font-semibold text-white">
                      {activeAlbumGroup.name}
                    </h3>
                    <p className="truncate text-xs text-zinc-400 tabular-nums">
                      {activeAlbumGroup.artistSummary} · {tracks.length} آهنگ
                    </p>
                  </div>
                </div>
                {tracks.length > 0 && (
                  <button
                    type="button"
                    onClick={() => onPlayCollection(tracks)}
                    className="inline-flex min-h-[40px] shrink-0 items-center gap-1.5 rounded-xl bg-white px-3.5 py-2 text-xs font-semibold text-zinc-950 hover:bg-zinc-200 active:scale-[0.97] transition whitespace-nowrap"
                  >
                    <Play className="h-3.5 w-3.5 fill-current" aria-hidden="true" />
                    <span>پخش آلبوم</span>
                  </button>
                )}
              </div>
            )}

            {libraryTab === LibraryTab.Artists && activeArtistGroup && (
              <div className="mb-3 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-white/10 bg-white/[0.05] p-3">
                <div className="flex min-w-0 items-center gap-3">
                  <button
                    type="button"
                    onClick={() => onSelectArtist(null)}
                    aria-label="بازگشت به هنرمندان"
                    className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-white/10 bg-white/[0.06] text-zinc-200 hover:bg-white/[0.12]"
                  >
                    <ArrowRight className="h-4 w-4" />
                  </button>
                  <TrackArtwork
                    title={activeArtistGroup.name}
                    coverDataUrl={activeArtistGroup.coverDataUrl}
                    accentHue={activeArtistGroup.accentHue}
                    size="sm"
                  />
                  <div className="min-w-0">
                    <h3 className="truncate text-sm font-semibold text-white">
                      {activeArtistGroup.name}
                    </h3>
                    <p className="truncate text-xs text-zinc-400 tabular-nums">
                      {tracks.length} آهنگ · {activeArtistGroup.albumCount} آلبوم
                    </p>
                  </div>
                </div>
                {tracks.length > 0 && (
                  <button
                    type="button"
                    onClick={() => onPlayCollection(tracks)}
                    className="inline-flex min-h-[40px] shrink-0 items-center gap-1.5 rounded-xl bg-white px-3.5 py-2 text-xs font-semibold text-zinc-950 hover:bg-zinc-200 active:scale-[0.97] transition whitespace-nowrap"
                  >
                    <Play className="h-3.5 w-3.5 fill-current" aria-hidden="true" />
                    <span>پخش همه</span>
                  </button>
                )}
              </div>
            )}

            {tracks.length === 0 ? (
              <div className="flex min-h-[220px] flex-col items-center justify-center px-4 py-10 text-center">
                <p className="text-sm font-medium text-zinc-300">
                  {libraryTab === LibraryTab.Favorites && !searchQuery
                    ? 'هنوز آهنگی به علاقه‌مندی‌ها اضافه نشده است'
                    : libraryTab === LibraryTab.Recent && !searchQuery
                      ? 'هنوز آهنگی پخش نشده است'
                      : 'آهنگی با این جستجو پیدا نشد'}
                </p>
                <button
                  type="button"
                  onClick={() => {
                    if (searchQuery) {
                      onSearchChange('');
                    } else {
                      onLibraryTabChange(LibraryTab.All);
                    }
                  }}
                  className="mt-3 min-h-[40px] rounded-xl border border-white/12 bg-white/[0.06] px-4 py-2 text-xs font-medium text-zinc-200 hover:bg-white/[0.11]"
                >
                  {searchQuery ? 'نمایش همه آهنگ‌ها' : 'رفتن به همه آهنگ‌ها'}
                </button>
              </div>
            ) : (
              <div className="space-y-1">
                <AnimatePresence initial={false}>
                  {tracks.map((track) => {
                    const isActive = track.id === activeTrackId;
                    const isSelected = selectedIds.has(track.id);
                    const isHighlighted = track.id === highlightedId;

                    return (
                      <motion.div
                        layout
                        key={track.id}
                        ref={(el) => {
                          if (el) rowRefs.current.set(track.id, el);
                          else rowRefs.current.delete(track.id);
                        }}
                        initial={{ opacity: 0, y: 8 }}
                        animate={{
                          opacity: 1,
                          y: 0,
                          scale: isHighlighted ? [1, 1.015, 1] : 1,
                        }}
                        exit={{ opacity: 0, scale: 0.96 }}
                        transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
                        whileTap={{ scale: 0.988 }}
                        onPointerDown={(e) => handlePointerDown(e, track.id)}
                        onPointerMove={handlePointerMove}
                        onPointerUp={clearLongPress}
                        onPointerCancel={clearLongPress}
                        onPointerLeave={clearLongPress}
                        onClick={() => handleRowClick(track)}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            handleRowClick(track);
                          }
                        }}
                        className={`group flex min-h-[60px] cursor-pointer items-center justify-between gap-2.5 rounded-2xl px-3 py-2 transition-colors select-none focus-visible:outline-2 focus-visible:outline-sky-400 ${
                          isHighlighted
                            ? 'border border-sky-400/70 bg-sky-500/20 ring-2 ring-sky-400/35'
                            : isSelected
                              ? 'border border-indigo-400/40 bg-indigo-500/15'
                              : isActive
                                ? 'border border-white/15 bg-white/[0.09]'
                                : 'border border-transparent hover:bg-white/[0.05]'
                        }`}
                      >
                        <div className="flex min-w-0 flex-1 items-center gap-3">
                          {isSelectionMode && (
                            <div className="flex h-6 w-6 shrink-0 items-center justify-center">
                              {isSelected ? (
                                <span className="flex h-5 w-5 items-center justify-center rounded-md bg-indigo-500 text-white">
                                  <Check className="h-3.5 w-3.5" aria-hidden="true" />
                                </span>
                              ) : (
                                <Square className="h-5 w-5 text-zinc-500" aria-hidden="true" />
                              )}
                            </div>
                          )}

                          <TrackArtwork
                            title={track.title}
                            coverDataUrl={track.coverDataUrl}
                            accentHue={track.accentHue}
                            size="sm"
                            isPlaying={isActive && isPlaying}
                          />

                          <div className="min-w-0 flex-1">
                            <p
                              className={`truncate text-sm font-medium ${
                                isActive ? 'text-sky-300' : 'text-zinc-100'
                              }`}
                            >
                              {track.title}
                            </p>
                            <div className="mt-0.5 flex items-center gap-1.5 truncate text-xs text-zinc-400">
                              <span className="truncate">{track.artist}</span>
                              {track.album && (
                                <>
                                  <span aria-hidden="true">·</span>
                                  <span className="truncate text-zinc-500">{track.album}</span>
                                </>
                              )}
                            </div>
                          </div>
                        </div>

                        <div className="flex shrink-0 items-center gap-1">
                          {isActive && (
                            <MiniEqualizer isPlaying={isPlaying} accentHue={track.accentHue} />
                          )}
                          <span
                            className="px-1 font-mono text-xs tabular-nums text-zinc-400"
                            dir="ltr"
                          >
                            {formatDuration(track.duration)}
                          </span>
                          {!isSelectionMode && (
                            <>
                              <button
                                type="button"
                                onPointerDown={(e) => e.stopPropagation()}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  onToggleFavorite(track.id);
                                }}
                                aria-label={
                                  track.isFavorite
                                    ? 'حذف از علاقه‌مندی‌ها'
                                    : 'افزودن به علاقه‌مندی‌ها'
                                }
                                className={`flex h-9 w-9 items-center justify-center rounded-xl transition ${
                                  track.isFavorite
                                    ? 'text-rose-400 hover:bg-rose-500/15'
                                    : 'text-zinc-500 hover:bg-white/10 hover:text-zinc-200'
                                }`}
                              >
                                <Heart
                                  className={`h-4 w-4 ${track.isFavorite ? 'fill-current' : ''}`}
                                />
                              </button>
                              <button
                                type="button"
                                onPointerDown={(e) => e.stopPropagation()}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  clearLongPress();
                                  setActionTrack(track);
                                }}
                                aria-label={`گزینه‌های بیشتر برای ${track.title}`}
                                className="flex h-9 w-9 items-center justify-center rounded-xl text-zinc-400 hover:bg-white/10 hover:text-white transition"
                              >
                                <MoreVertical className="h-4 w-4" />
                              </button>
                            </>
                          )}
                        </div>
                      </motion.div>
                    );
                  })}
                </AnimatePresence>
              </div>
            )}
          </>
        )}
      </div>

      {/* Desktop floating jump-to-active button positioned at bottom-end (bottom-left in RTL, bottom-right in LTR) */}
      <AnimatePresence>
        {activeTrackId && !isSelectionMode && (
          <motion.button
            type="button"
            initial={{ opacity: 0, scale: 0.9, y: 8 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: 8 }}
            transition={{ duration: 0.16 }}
            onClick={onJumpToActive}
            aria-label="پرش به آهنگ در حال پخش"
            className="hidden lg:inline-flex absolute bottom-4 end-4 z-20 min-h-[42px] items-center gap-1.5 rounded-full border border-sky-400/35 bg-zinc-900/90 px-3.5 text-xs font-medium text-sky-200 shadow-xl shadow-black/50 backdrop-blur-xl hover:bg-zinc-800 active:scale-95 transition whitespace-nowrap"
          >
            <LocateFixed className="h-4 w-4 text-sky-300" aria-hidden="true" />
            <span>آهنگ فعلی</span>
          </motion.button>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {isSelectionMode && selectedIds.size > 0 && !isOverviewMode && (
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 12 }}
            transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
            className="border-t border-white/[0.1] bg-zinc-900/90 p-3 backdrop-blur-xl rounded-b-3xl"
          >
            {confirmingDelete ? (
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="text-xs font-medium text-red-200 tabular-nums">
                  حذف {selectedIds.size} آهنگ از حافظه دستگاه؟
                </span>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      onBulkDelete(Array.from(selectedIds));
                      setConfirmingDelete(false);
                    }}
                    className="inline-flex min-h-[44px] items-center gap-1.5 rounded-xl bg-red-600 px-4 py-2 text-xs font-medium text-white hover:bg-red-500 active:scale-[0.97] transition whitespace-nowrap"
                  >
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                    <span>بله، حذف شوند</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setConfirmingDelete(false)}
                    className="inline-flex min-h-[44px] items-center rounded-xl border border-white/15 bg-white/[0.06] px-4 py-2 text-xs font-medium text-zinc-200 hover:bg-white/[0.12] transition whitespace-nowrap"
                  >
                    انصراف
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setConfirmingDelete(true)}
                  className="inline-flex min-h-[46px] flex-1 items-center justify-center gap-2 rounded-xl bg-red-500/20 border border-red-400/35 px-4 py-2 text-xs font-semibold text-red-200 hover:bg-red-500/30 active:scale-[0.98] transition whitespace-nowrap"
                >
                  <Trash2 className="h-4 w-4" aria-hidden="true" />
                  <span className="tabular-nums">حذف ({selectedIds.size})</span>
                </button>

                <button
                  type="button"
                  onClick={() => onBulkQueue(Array.from(selectedIds))}
                  className="inline-flex min-h-[46px] flex-1 items-center justify-center gap-2 rounded-xl border border-white/15 bg-white/[0.08] px-4 py-2 text-xs font-medium text-zinc-100 hover:bg-white/[0.14] active:scale-[0.98] transition whitespace-nowrap"
                >
                  <ListPlus className="h-4 w-4" aria-hidden="true" />
                  <span>افزودن به صف پخش</span>
                </button>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Per-track quick action sheet */}
      <AnimatePresence>
        {actionTrack && (
          <div
            role="dialog"
            aria-modal="true"
            aria-label={`عملیات آهنگ ${actionTrack.title}`}
            onClick={() => setActionTrack(null)}
            className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 p-0 backdrop-blur-sm sm:items-center sm:p-4"
          >
            <motion.div
              initial={{ opacity: 0, y: 24, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 24, scale: 0.98 }}
              transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-sm rounded-t-3xl border border-white/15 bg-zinc-950/95 p-4 text-zinc-100 shadow-2xl backdrop-blur-2xl sm:rounded-3xl"
            >
              <div className="mb-3 flex items-center justify-between gap-3 border-b border-white/[0.08] pb-3">
                <div className="flex min-w-0 items-center gap-3">
                  <TrackArtwork
                    title={actionTrack.title}
                    coverDataUrl={actionTrack.coverDataUrl}
                    accentHue={actionTrack.accentHue}
                    size="sm"
                  />
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-white">
                      {actionTrack.title}
                    </p>
                    <p className="truncate text-xs text-zinc-400">{actionTrack.artist}</p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setActionTrack(null)}
                  aria-label="بستن"
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-zinc-400 hover:bg-white/10 hover:text-white"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <div className="space-y-1">
                <button
                  type="button"
                  onClick={() => {
                    onPlayNextInQueue(actionTrack.id);
                    setActionTrack(null);
                  }}
                  className="flex min-h-[44px] w-full items-center gap-3 rounded-xl px-3 text-xs font-medium text-zinc-200 hover:bg-white/[0.08] transition"
                >
                  <ListPlus className="h-4 w-4 text-sky-300" aria-hidden="true" />
                  <span>پخش بعدی در صف</span>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    const id = actionTrack.id;
                    setActionTrack(null);
                    onOpenTrackLyrics(id);
                  }}
                  className="flex min-h-[44px] w-full items-center gap-3 rounded-xl px-3 text-xs font-medium text-zinc-200 hover:bg-white/[0.08] transition"
                >
                  <FileText className="h-4 w-4 text-indigo-300" aria-hidden="true" />
                  <span>متن آهنگ</span>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    const id = actionTrack.id;
                    setActionTrack(null);
                    onOpenTrackRingtone(id);
                  }}
                  className="flex min-h-[44px] w-full items-center gap-3 rounded-xl px-3 text-xs font-medium text-zinc-200 hover:bg-white/[0.08] transition"
                >
                  <BellRing className="h-4 w-4 text-emerald-300" aria-hidden="true" />
                  <span>ساخت زنگ و آلارم</span>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    const id = actionTrack.id;
                    setActionTrack(null);
                    onOpenTrackEdit(id);
                  }}
                  className="flex min-h-[44px] w-full items-center gap-3 rounded-xl px-3 text-xs font-medium text-zinc-200 hover:bg-white/[0.08] transition"
                >
                  <Edit3 className="h-4 w-4 text-zinc-300" aria-hidden="true" />
                  <span>ویرایش مشخصات</span>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    const id = actionTrack.id;
                    setActionTrack(null);
                    onEnterSelectionWithTrack(id);
                  }}
                  className="flex min-h-[44px] w-full items-center gap-3 rounded-xl px-3 text-xs font-medium text-zinc-200 hover:bg-white/[0.08] transition"
                >
                  <CheckSquare className="h-4 w-4 text-zinc-300" aria-hidden="true" />
                  <span>انتخاب چندتایی</span>
                </button>

                {confirmSingleDelete ? (
                  <div className="mt-2 flex items-center justify-between gap-2 rounded-xl border border-red-400/25 bg-red-500/10 p-2.5">
                    <span className="text-xs text-red-200">حذف از حافظه؟</span>
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          const id = actionTrack.id;
                          setActionTrack(null);
                          onBulkDelete([id]);
                        }}
                        className="rounded-lg bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-500"
                      >
                        بله، حذف شود
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmSingleDelete(false)}
                        className="rounded-lg bg-white/10 px-3 py-1.5 text-xs text-zinc-200"
                      >
                        انصراف
                      </button>
                    </div>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={() => setConfirmSingleDelete(true)}
                    className="flex min-h-[44px] w-full items-center gap-3 rounded-xl px-3 text-xs font-medium text-red-300 hover:bg-red-500/10 transition"
                  >
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                    <span>حذف آهنگ</span>
                  </button>
                )}
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </section>
  );
};
