/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, animate, motion, useMotionValue } from 'motion/react';
import { CheckCircle2, LocateFixed, Plus } from 'lucide-react';
import { EditTrackModal } from './components/EditTrackModal';
import { EQ_PRESET_VALUES, EqualizerModal } from './components/EqualizerModal';
import { LyricsModal } from './components/LyricsModal';
import { OfflineIndicator } from './components/OfflineIndicator';
import { MobileMiniPlayer, PlayerStage } from './components/PlayerStage';
import { PWAInstallButton } from './components/PWAInstallButton';
import { RingtoneMakerModal } from './components/RingtoneMakerModal';
import { TrackListPanel } from './components/TrackListPanel';
import { EqPreset, LibraryTab, RepeatMode, SortMode, TrackMeta } from './types/music';
import {
  createStarterDemoTracks,
  deleteTracksByIds,
  getAllTrackMetas,
  getTrackBlob,
  importFilesToTracks,
  saveTrackRecords,
  updateTrackMeta,
} from './utils/musicStorage';

const SEEDED_KEY = 'avaplayer_seeded_v1';
const EQ_STORAGE_KEY = 'avaplayer_eq_v1';
const EQ_FREQUENCIES = [60, 230, 910, 3600, 14000];

export default function App() {
  const [tracks, setTracks] = useState<TrackMeta[]>([]);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [isImporting, setIsImporting] = useState(false);

  const [activeTrackId, setActiveTrackId] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(0.85);
  const [shuffle, setShuffle] = useState(false);
  const [repeatMode, setRepeatMode] = useState<RepeatMode>(RepeatMode.All);
  const [manualQueueIds, setManualQueueIds] = useState<string[]>([]);
  const [sleepMinutesLeft, setSleepMinutesLeft] = useState<number | null>(null);

  const [libraryTab, setLibraryTab] = useState<LibraryTab>(LibraryTab.All);
  const [selectedAlbum, setSelectedAlbum] = useState<string | null>(null);
  const [selectedArtist, setSelectedArtist] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortMode, setSortMode] = useState<SortMode>(SortMode.DateDesc);
  const [isSelectionMode, setIsSelectionMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [jumpSignal, setJumpSignal] = useState(0);
  const [isMobileSheetOpen, setIsMobileSheetOpen] = useState(false);
  const [statusNotice, setStatusNotice] = useState<string | null>(null);

  const [modalTrackId, setModalTrackId] = useState<string | null>(null);
  const [showLyricsModal, setShowLyricsModal] = useState(false);
  const [showRingtoneModal, setShowRingtoneModal] = useState(false);
  const [showEqModal, setShowEqModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);

  const [eqPreset, setEqPreset] = useState<EqPreset>(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(EQ_STORAGE_KEY) || '{}');
      return saved.preset || EqPreset.Flat;
    } catch {
      return EqPreset.Flat;
    }
  });
  const [eqGains, setEqGains] = useState<number[]>(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(EQ_STORAGE_KEY) || '{}');
      return Array.isArray(saved.gains) && saved.gains.length === 5
        ? saved.gains
        : [0, 0, 0, 0, 0];
    } catch {
      return [0, 0, 0, 0, 0];
    }
  });
  const [playbackRate, setPlaybackRate] = useState<number>(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(EQ_STORAGE_KEY) || '{}');
      return typeof saved.playbackRate === 'number' ? saved.playbackRate : 1;
    } catch {
      return 1;
    }
  });

  const audioRef = useRef<HTMLAudioElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const activeObjectUrlRef = useRef<string | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const eqFiltersRef = useRef<BiquadFilterNode[]>([]);
  const sourceConnectedRef = useRef(false);
  const noticeTimerRef = useRef<number | null>(null);
  const sleepTimerRef = useRef<number | null>(null);
  const playerSectionRef = useRef<HTMLElement | null>(null);
  const sheetDragY = useMotionValue(0);
  const sheetSwipeRef = useRef<{
    startX: number;
    startY: number;
    startTime: number;
    lastDy: number;
    locked: boolean;
    aborted: boolean;
  } | null>(null);

  useEffect(() => {
    if (isMobileSheetOpen) {
      sheetDragY.set(0);
    }
  }, [isMobileSheetOpen, sheetDragY]);

  const showNotice = useCallback((msg: string) => {
    setStatusNotice(msg);
    if (noticeTimerRef.current !== null) {
      window.clearTimeout(noticeTimerRef.current);
    }
    noticeTimerRef.current = window.setTimeout(() => {
      setStatusNotice(null);
    }, 3000);
  }, []);

  const ensureAudioGraph = useCallback(() => {
    const audio = audioRef.current;
    if (!audio || sourceConnectedRef.current) return;
    try {
      const AudioCtx =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!AudioCtx) return;

      const ctx = new AudioCtx();
      const analyser = ctx.createAnalyser();
      analyser.fftSize = 128;
      analyser.smoothingTimeConstant = 0.82;

      const source = ctx.createMediaElementSource(audio);
      const filters = EQ_FREQUENCIES.map((freq, idx) => {
        const filter = ctx.createBiquadFilter();
        filter.type =
          idx === 0
            ? 'lowshelf'
            : idx === EQ_FREQUENCIES.length - 1
              ? 'highshelf'
              : 'peaking';
        filter.frequency.value = freq;
        filter.Q.value = 1.1;
        filter.gain.value = eqGains[idx] ?? 0;
        return filter;
      });

      let head: AudioNode = source;
      for (const filter of filters) {
        head.connect(filter);
        head = filter;
      }
      head.connect(analyser);
      analyser.connect(ctx.destination);

      audioContextRef.current = ctx;
      analyserRef.current = analyser;
      eqFiltersRef.current = filters;
      sourceConnectedRef.current = true;
    } catch {
      // Visualizer gracefully falls back to harmonic motion
    }
  }, [eqGains]);

  useEffect(() => {
    eqFiltersRef.current.forEach((filter, idx) => {
      filter.gain.value = eqGains[idx] ?? 0;
    });
    if (audioRef.current) {
      audioRef.current.playbackRate = playbackRate;
    }
    try {
      localStorage.setItem(
        EQ_STORAGE_KEY,
        JSON.stringify({ preset: eqPreset, gains: eqGains, playbackRate })
      );
    } catch {
      // Ignore storage quota errors
    }
  }, [eqGains, eqPreset, playbackRate]);

  const revokeCurrentAudioUrl = useCallback(() => {
    if (activeObjectUrlRef.current) {
      URL.revokeObjectURL(activeObjectUrlRef.current);
      activeObjectUrlRef.current = null;
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    const bootstrap = async () => {
      try {
        let loaded = await getAllTrackMetas();
        const alreadySeeded = localStorage.getItem(SEEDED_KEY) === '1';

        if (loaded.length === 0 && !alreadySeeded) {
          const demoRecords = createStarterDemoTracks();
          await saveTrackRecords(demoRecords);
          localStorage.setItem(SEEDED_KEY, '1');
          loaded = demoRecords.map(({ audioBlob: _blob, ...meta }) => meta);
        }

        if (!cancelled) {
          setTracks(loaded);
          const params = new URLSearchParams(window.location.search);
          const shortcut = params.get('shortcut');

          if (shortcut === 'favorites') {
            setLibraryTab(LibraryTab.Favorites);
          } else if (shortcut === 'recent') {
            setLibraryTab(LibraryTab.Recent);
          } else if (shortcut === 'albums') {
            setLibraryTab(LibraryTab.Albums);
          }

          if (loaded.length > 0) {
            const recentTrack =
              [...loaded].sort((a, b) => (b.lastPlayedAt || 0) - (a.lastPlayedAt || 0))[0] ||
              loaded[0];
            setActiveTrackId(recentTrack.id);
            setDuration(recentTrack.duration);

            if (shortcut === 'play') {
              setIsMobileSheetOpen(true);
            }
          }

          if (shortcut) {
            window.history.replaceState({}, '', '/');
          }
        }
      } catch {
        if (!cancelled) {
          showNotice('خطا در بارگذاری حافظه محلی آهنگ‌ها.');
        }
      } finally {
        if (!cancelled) {
          setIsInitialLoading(false);
        }
      }
    };

    bootstrap();

    return () => {
      cancelled = true;
      revokeCurrentAudioUrl();
      if (noticeTimerRef.current !== null) window.clearTimeout(noticeTimerRef.current);
      if (sleepTimerRef.current !== null) window.clearInterval(sleepTimerRef.current);
    };
  }, [revokeCurrentAudioUrl, showNotice]);

  const favoriteCount = useMemo(
    () => tracks.filter((t) => Boolean(t.isFavorite)).length,
    [tracks]
  );

  const recentCount = useMemo(
    () => tracks.filter((t) => Boolean(t.lastPlayedAt)).length,
    [tracks]
  );

  const filteredAndSortedTracks = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    const filtered = tracks.filter((track) => {
      if (libraryTab === LibraryTab.Favorites && !track.isFavorite) return false;
      if (libraryTab === LibraryTab.Recent && !track.lastPlayedAt) return false;
      if (libraryTab === LibraryTab.Albums && selectedAlbum !== null) {
        const alb = (track.album || '').trim() || 'آلبوم ناشناس';
        if (alb !== selectedAlbum) return false;
      }
      if (libraryTab === LibraryTab.Artists && selectedArtist !== null) {
        const art = (track.artist || '').trim() || 'هنرمند ناشناس';
        if (art !== selectedArtist) return false;
      }
      if (!query) return true;
      return (
        track.title.toLowerCase().includes(query) ||
        track.artist.toLowerCase().includes(query) ||
        track.album.toLowerCase().includes(query)
      );
    });

    const sorted = [...filtered];
    if (libraryTab === LibraryTab.Recent) {
      sorted.sort((a, b) => (b.lastPlayedAt || 0) - (a.lastPlayedAt || 0));
      return sorted;
    }

    sorted.sort((a, b) => {
      switch (sortMode) {
        case SortMode.TitleAsc:
          return a.title.localeCompare(b.title, 'fa');
        case SortMode.ArtistAsc:
          return a.artist.localeCompare(b.artist, 'fa');
        case SortMode.DurationDesc:
          return b.duration - a.duration;
        case SortMode.DateDesc:
        default:
          return b.addedAt - a.addedAt;
      }
    });
    return sorted;
  }, [tracks, searchQuery, sortMode, libraryTab, selectedAlbum, selectedArtist]);

  const activeTrack = useMemo(
    () => tracks.find((t) => t.id === activeTrackId) || null,
    [tracks, activeTrackId]
  );

  const modalTrack = useMemo(
    () => tracks.find((t) => t.id === modalTrackId) || activeTrack,
    [tracks, modalTrackId, activeTrack]
  );

  const upNextTracks = useMemo(() => {
    if (!activeTrack) return [];
    const byId = new Map(tracks.map((t) => [t.id, t]));
    const queued = manualQueueIds
      .map((id) => byId.get(id))
      .filter((t): t is TrackMeta => Boolean(t && t.id !== activeTrack.id));

    if (queued.length > 0) return queued;

    const list = filteredAndSortedTracks.length > 0 ? filteredAndSortedTracks : tracks;
    const idx = list.findIndex((t) => t.id === activeTrack.id);
    if (idx === -1) return list.slice(0, 3);
    return [...list.slice(idx + 1), ...list.slice(0, idx)].slice(0, 3);
  }, [activeTrack, tracks, manualQueueIds, filteredAndSortedTracks]);

  const loadAndPlayTrack = useCallback(
    async (track: TrackMeta, shouldPlay: boolean) => {
      const audio = audioRef.current;
      if (!audio) return;

      setActiveTrackId(track.id);
      setCurrentTime(0);
      setDuration(track.duration);

      try {
        const blob = await getTrackBlob(track.id);
        if (!blob) {
          showNotice('فایل صوتی این قطعه در حافظه پیدا نشد.');
          return;
        }

        revokeCurrentAudioUrl();
        const url = URL.createObjectURL(blob);
        activeObjectUrlRef.current = url;
        audio.src = url;
        audio.volume = volume;
        audio.playbackRate = playbackRate;
        audio.load();

        if (shouldPlay) {
          ensureAudioGraph();
          if (audioContextRef.current?.state === 'suspended') {
            await audioContextRef.current.resume();
          }
          await audio.play();
          setIsPlaying(true);

          const playedAt = Date.now();
          const nextCount = (track.playCount || 0) + 1;
          setTracks((prev) =>
            prev.map((t) =>
              t.id === track.id
                ? { ...t, lastPlayedAt: playedAt, playCount: nextCount }
                : t
            )
          );
          updateTrackMeta(track.id, { lastPlayedAt: playedAt, playCount: nextCount }).catch(
            () => {}
          );
        } else {
          setIsPlaying(false);
        }
      } catch {
        setIsPlaying(false);
        showNotice('پخش این فایل صوتی ممکن نشد.');
      }
    },
    [ensureAudioGraph, playbackRate, revokeCurrentAudioUrl, showNotice, volume]
  );

  const handleTogglePlay = useCallback(async () => {
    const audio = audioRef.current;
    if (!audio) return;

    if (!activeTrack) {
      const first = filteredAndSortedTracks[0] || tracks[0];
      if (first) await loadAndPlayTrack(first, true);
      return;
    }

    if (!audio.src) {
      await loadAndPlayTrack(activeTrack, true);
      return;
    }

    if (audio.paused) {
      ensureAudioGraph();
      if (audioContextRef.current?.state === 'suspended') {
        await audioContextRef.current.resume();
      }
      try {
        await audio.play();
        setIsPlaying(true);
      } catch {
        showNotice('مرورگر اجازه پخش خودکار نداد؛ دوباره دکمه پخش را بزنید.');
      }
    } else {
      audio.pause();
      setIsPlaying(false);
    }
  }, [activeTrack, ensureAudioGraph, filteredAndSortedTracks, loadAndPlayTrack, showNotice, tracks]);

  const handlePlayNext = useCallback(
    async (fromTrackEnded = false) => {
      const audio = audioRef.current;
      if (fromTrackEnded && repeatMode === RepeatMode.One && audio) {
        audio.currentTime = 0;
        await audio.play();
        setIsPlaying(true);
        return;
      }

      if (manualQueueIds.length > 0) {
        const [nextQueuedId, ...restQueue] = manualQueueIds;
        setManualQueueIds(restQueue);
        const queuedTrack = tracks.find((t) => t.id === nextQueuedId);
        if (queuedTrack) {
          await loadAndPlayTrack(queuedTrack, true);
          return;
        }
      }

      const pool = filteredAndSortedTracks.length > 0 ? filteredAndSortedTracks : tracks;
      if (pool.length === 0) return;

      if (shuffle && pool.length > 1) {
        const candidates = pool.filter((t) => t.id !== activeTrackId);
        const pick = candidates[Math.floor(Math.random() * candidates.length)] || pool[0];
        await loadAndPlayTrack(pick, true);
        return;
      }

      const currentIdx = pool.findIndex((t) => t.id === activeTrackId);
      const isLast = currentIdx === pool.length - 1;

      if (fromTrackEnded && isLast && repeatMode === RepeatMode.Off) {
        setIsPlaying(false);
        setCurrentTime(0);
        return;
      }

      const nextIdx = currentIdx >= 0 ? (currentIdx + 1) % pool.length : 0;
      await loadAndPlayTrack(pool[nextIdx], true);
    },
    [
      activeTrackId,
      filteredAndSortedTracks,
      loadAndPlayTrack,
      manualQueueIds,
      repeatMode,
      shuffle,
      tracks,
    ]
  );

  const handlePlayPrev = useCallback(async () => {
    const audio = audioRef.current;
    if (audio && audio.currentTime > 3) {
      audio.currentTime = 0;
      setCurrentTime(0);
      return;
    }

    const pool = filteredAndSortedTracks.length > 0 ? filteredAndSortedTracks : tracks;
    if (pool.length === 0) return;

    const currentIdx = pool.findIndex((t) => t.id === activeTrackId);
    const prevIdx = currentIdx > 0 ? currentIdx - 1 : pool.length - 1;
    await loadAndPlayTrack(pool[prevIdx], true);
  }, [activeTrackId, filteredAndSortedTracks, loadAndPlayTrack, tracks]);

  useEffect(() => {
    if (!('mediaSession' in navigator) || !activeTrack) return;

    navigator.mediaSession.metadata = new MediaMetadata({
      title: activeTrack.title,
      artist: activeTrack.artist,
      album: activeTrack.album || 'TO Music',
      artwork: activeTrack.coverDataUrl
        ? [{ src: activeTrack.coverDataUrl, sizes: '512x512', type: 'image/png' }]
        : [{ src: '/pwa-512x512.png', sizes: '512x512', type: 'image/png' }],
    });

    navigator.mediaSession.setActionHandler('play', () => {
      handleTogglePlay();
    });
    navigator.mediaSession.setActionHandler('pause', () => {
      handleTogglePlay();
    });
    navigator.mediaSession.setActionHandler('nexttrack', () => {
      handlePlayNext(false);
    });
    navigator.mediaSession.setActionHandler('previoustrack', () => {
      handlePlayPrev();
    });
    navigator.mediaSession.setActionHandler('seekto', (details) => {
      if (typeof details.seekTime === 'number' && audioRef.current) {
        audioRef.current.currentTime = details.seekTime;
        setCurrentTime(details.seekTime);
      }
    });
  }, [activeTrack, handlePlayNext, handlePlayPrev, handleTogglePlay]);

  const handleSeek = useCallback((time: number) => {
    const audio = audioRef.current;
    if (!audio) return;
    audio.currentTime = time;
    setCurrentTime(time);
  }, []);

  const handleVolumeChange = useCallback((val: number) => {
    setVolume(val);
    if (audioRef.current) {
      audioRef.current.volume = val;
    }
  }, []);

  const handleCycleRepeat = useCallback(() => {
    setRepeatMode((prev) => {
      if (prev === RepeatMode.All) return RepeatMode.One;
      if (prev === RepeatMode.One) return RepeatMode.Off;
      return RepeatMode.All;
    });
  }, []);

  const handleSetSleepTimer = useCallback(
    (minutes: number | null) => {
      if (sleepTimerRef.current !== null) {
        window.clearInterval(sleepTimerRef.current);
        sleepTimerRef.current = null;
      }
      if (minutes === null) {
        setSleepMinutesLeft(null);
        showNotice('تایمر خواب خاموش شد.');
        return;
      }

      setSleepMinutesLeft(minutes);
      showNotice(`تایمر خواب روی ${minutes} دقیقه تنظیم شد.`);

      let remaining = minutes;
      sleepTimerRef.current = window.setInterval(() => {
        remaining -= 1;
        if (remaining <= 0) {
          if (sleepTimerRef.current !== null) {
            window.clearInterval(sleepTimerRef.current);
            sleepTimerRef.current = null;
          }
          setSleepMinutesLeft(null);
          if (audioRef.current && !audioRef.current.paused) {
            audioRef.current.pause();
            setIsPlaying(false);
          }
          showNotice('تایمر خواب به پایان رسید و پخش متوقف شد.');
        } else {
          setSleepMinutesLeft(remaining);
        }
      }, 60_000);
    },
    [showNotice]
  );

  const handleFilesSelected = useCallback(
    async (fileList: FileList | null) => {
      if (!fileList || fileList.length === 0) return;
      setIsImporting(true);
      try {
        const importedRecords = await importFilesToTracks(fileList);
        if (importedRecords.length === 0) {
          showNotice('فایل صوتی معتبری پیدا نشد. فرمت‌های MP3، WAV، M4A و OGG پشتیبانی می‌شوند.');
          return;
        }
        await saveTrackRecords(importedRecords);
        localStorage.setItem(SEEDED_KEY, '1');
        const newMetas: TrackMeta[] = importedRecords.map(
          ({ audioBlob: _blob, ...meta }) => meta
        );
        setTracks((prev) => [...newMetas, ...prev]);
        showNotice(`${newMetas.length} آهنگ جدید به کتابخانه اضافه شد.`);

        if (!activeTrackId) {
          await loadAndPlayTrack(newMetas[0], false);
        }
      } catch {
        showNotice('افزودن برخی فایل‌ها با خطا مواجه شد.');
      } finally {
        setIsImporting(false);
      }
    },
    [activeTrackId, loadAndPlayTrack, showNotice]
  );

  const handleLoadStarterTracks = useCallback(async () => {
    setIsImporting(true);
    try {
      const starter = createStarterDemoTracks();
      await saveTrackRecords(starter);
      localStorage.setItem(SEEDED_KEY, '1');
      const metas = starter.map(({ audioBlob: _blob, ...meta }) => meta);
      setTracks((prev) => {
        const existingIds = new Set(prev.map((t) => t.id));
        const fresh = metas.filter((m) => !existingIds.has(m.id));
        return [...fresh, ...prev];
      });
      if (!activeTrackId && metas[0]) {
        setActiveTrackId(metas[0].id);
        setDuration(metas[0].duration);
      }
      showNotice('آهنگ‌های نمونه به کتابخانه اضافه شدند.');
    } finally {
      setIsImporting(false);
    }
  }, [activeTrackId, showNotice]);

  const handleEnterSelectionWithTrack = useCallback((id: string) => {
    setIsSelectionMode(true);
    setSelectedIds(new Set([id]));
  }, []);

  const handleExitSelectionMode = useCallback(() => {
    setIsSelectionMode(false);
    setSelectedIds(new Set());
  }, []);

  const handleToggleTrackSelection = useCallback((id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }, []);

  const handleSelectAllFiltered = useCallback(() => {
    setSelectedIds(new Set(filteredAndSortedTracks.map((t) => t.id)));
  }, [filteredAndSortedTracks]);

  const handleClearSelection = useCallback(() => {
    setSelectedIds(new Set());
  }, []);

  const handleBulkDelete = useCallback(
    async (idsToDelete: string[]) => {
      if (idsToDelete.length === 0) return;
      const idSet = new Set(idsToDelete);

      try {
        await deleteTracksByIds(idsToDelete);
        const remaining = tracks.filter((t) => !idSet.has(t.id));
        setTracks(remaining);
        setManualQueueIds((prev) => prev.filter((id) => !idSet.has(id)));
        setSelectedIds(new Set());
        setIsSelectionMode(false);

        if (activeTrackId && idSet.has(activeTrackId)) {
          if (audioRef.current) {
            audioRef.current.pause();
            audioRef.current.src = '';
          }
          revokeCurrentAudioUrl();
          setIsPlaying(false);
          setCurrentTime(0);

          if (remaining.length > 0) {
            await loadAndPlayTrack(remaining[0], false);
          } else {
            setActiveTrackId(null);
            setDuration(0);
          }
        }

        showNotice(`${idsToDelete.length} آهنگ حذف شد.`);
      } catch {
        showNotice('حذف آهنگ‌ها با خطا مواجه شد.');
      }
    },
    [activeTrackId, loadAndPlayTrack, revokeCurrentAudioUrl, showNotice, tracks]
  );

  const handleBulkQueue = useCallback(
    (ids: string[]) => {
      if (ids.length === 0) return;
      setManualQueueIds((prev) => {
        const existing = new Set(prev);
        const added = ids.filter((id) => !existing.has(id) && id !== activeTrackId);
        return [...prev, ...added];
      });
      setIsSelectionMode(false);
      setSelectedIds(new Set());
      showNotice(`${ids.length} آهنگ به صف پخش بعدی اضافه شد.`);
    },
    [activeTrackId, showNotice]
  );

  const handlePlayNextInQueue = useCallback(
    (id: string) => {
      if (id === activeTrackId) {
        showNotice('این آهنگ هم‌اکنون در حال پخش است.');
        return;
      }
      setManualQueueIds((prev) => [id, ...prev.filter((item) => item !== id)]);
      showNotice('به‌عنوان آهنگ بعدی در صف قرار گرفت.');
    },
    [activeTrackId, showNotice]
  );

  const handlePlayCollection = useCallback(
    async (collectionTracks: TrackMeta[]) => {
      if (collectionTracks.length === 0) return;
      const [first, ...rest] = collectionTracks;
      setManualQueueIds(rest.map((t) => t.id));
      await loadAndPlayTrack(first, true);
      showNotice(`پخش ${collectionTracks.length} آهنگ آغاز شد.`);
    },
    [loadAndPlayTrack, showNotice]
  );

  const handleLibraryTabChange = useCallback((tab: LibraryTab) => {
    setLibraryTab(tab);
    setSelectedAlbum(null);
    setSelectedArtist(null);
    setIsSelectionMode(false);
    setSelectedIds(new Set());
  }, []);

  const handleJumpToActive = useCallback(() => {
    if (!activeTrackId) return;
    setLibraryTab(LibraryTab.All);
    setSelectedAlbum(null);
    setSelectedArtist(null);
    if (searchQuery.trim().length > 0) {
      setSearchQuery('');
    }
    setIsMobileSheetOpen(false);
    window.setTimeout(() => {
      setJumpSignal((n) => n + 1);
    }, 60);
  }, [activeTrackId, searchQuery]);

  const handleToggleFavorite = useCallback(
    async (id: string) => {
      const target = tracks.find((t) => t.id === id);
      if (!target) return;
      const nextFav = !target.isFavorite;
      setTracks((prev) =>
        prev.map((t) => (t.id === id ? { ...t, isFavorite: nextFav } : t))
      );
      await updateTrackMeta(id, { isFavorite: nextFav });
    },
    [tracks]
  );

  const handleSaveLyrics = useCallback(
    async (trackId: string, plainLyrics?: string, syncedLyrics?: string) => {
      setTracks((prev) =>
        prev.map((t) =>
          t.id === trackId ? { ...t, plainLyrics, syncedLyrics } : t
        )
      );
      await updateTrackMeta(trackId, { plainLyrics, syncedLyrics });
    },
    []
  );

  const handleSaveTrackDetails = useCallback(
    async (id: string, title: string, artist: string, album: string) => {
      setTracks((prev) =>
        prev.map((t) => (t.id === id ? { ...t, title, artist, album } : t))
      );
      await updateTrackMeta(id, { title, artist, album });
      showNotice('مشخصات آهنگ به‌روزرسانی شد.');
    },
    [showNotice]
  );

  const handleSelectEqPreset = useCallback((preset: Exclude<EqPreset, EqPreset.Custom>) => {
    setEqPreset(preset);
    setEqGains([...EQ_PRESET_VALUES[preset]]);
  }, []);

  const handleChangeEqBand = useCallback((index: number, db: number) => {
    setEqPreset(EqPreset.Custom);
    setEqGains((prev) => prev.map((v, i) => (i === index ? db : v)));
  }, []);

  return (
    <div className="relative flex min-h-screen flex-col bg-zinc-950 text-zinc-100">
      <div
        aria-hidden="true"
        className="pointer-events-none fixed inset-0 overflow-hidden"
      >
        <div
          className="absolute -top-40 start-1/4 h-96 w-96 rounded-full opacity-25 blur-3xl transition-colors duration-700"
          style={{
            backgroundColor: activeTrack
              ? `hsl(${activeTrack.accentHue}, 80%, 45%)`
              : '#4f46e5',
          }}
        />
        <div className="absolute -bottom-40 end-1/4 h-96 w-96 rounded-full bg-sky-600/15 blur-3xl" />
      </div>

      <input
        ref={fileInputRef}
        type="file"
        multiple
        accept="audio/*,.mp3,.wav,.ogg,.m4a,.aac,.flac,.zip"
        onChange={(e) => {
          handleFilesSelected(e.target.files);
          e.target.value = '';
        }}
        className="sr-only"
        aria-label="افزودن فایل‌های صوتی"
      />

      <audio
        ref={audioRef}
        playsInline
        onTimeUpdate={() => {
          if (audioRef.current) {
            setCurrentTime(audioRef.current.currentTime);
          }
        }}
        onLoadedMetadata={() => {
          if (audioRef.current && Number.isFinite(audioRef.current.duration)) {
            setDuration(audioRef.current.duration);
          }
        }}
        onEnded={() => handlePlayNext(true)}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
      />

      <header className="sticky top-0 z-30 flex h-14 items-center justify-between gap-4 border-b border-white/[0.08] bg-zinc-950/80 px-4 sm:px-6 backdrop-blur-xl">
        <span className="text-base font-bold tracking-tight text-white whitespace-nowrap shrink-0" dir="ltr">
          TO Music
        </span>

        <div className="flex items-center gap-2 shrink-0">
          <PWAInstallButton />
          <button
            type="button"
            disabled={isImporting}
            onClick={() => fileInputRef.current?.click()}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl bg-white px-3.5 py-2 text-xs font-semibold text-zinc-950 hover:bg-zinc-200 active:scale-[0.97] disabled:opacity-60 transition whitespace-nowrap shrink-0"
          >
            <Plus className="h-4 w-4" aria-hidden="true" />
            <span>{isImporting ? 'در حال افزودن...' : 'افزودن آهنگ'}</span>
          </button>
        </div>
      </header>

      <OfflineIndicator />

      <AnimatePresence>
        {statusNotice && (
          <motion.div
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
            transition={{ duration: 0.18 }}
            role="status"
            className="fixed inset-x-4 top-16 z-40 mx-auto max-w-md rounded-2xl border border-emerald-400/25 bg-zinc-900/95 px-4 py-3 text-xs font-medium text-emerald-200 shadow-2xl backdrop-blur-xl"
          >
            <div className="flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" aria-hidden="true" />
              <span>{statusNotice}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <main className="relative z-10 mx-auto flex w-full max-w-7xl flex-1 flex-col p-3 pb-36 sm:p-5 lg:pb-5">
        {isInitialLoading ? (
          <div className="grid flex-1 grid-cols-1 gap-5 lg:grid-cols-[1.15fr_1fr]">
            <div className="space-y-3 rounded-3xl border border-white/[0.08] bg-white/[0.04] p-5">
              <div className="h-10 w-full animate-pulse rounded-xl bg-white/[0.06]" />
              <div className="h-14 w-full animate-pulse rounded-2xl bg-white/[0.05]" />
              <div className="h-14 w-full animate-pulse rounded-2xl bg-white/[0.05]" />
              <div className="h-14 w-full animate-pulse rounded-2xl bg-white/[0.05]" />
            </div>
            <div className="hidden rounded-3xl border border-white/[0.08] bg-white/[0.04] lg:block" />
          </div>
        ) : (
          <div className="grid flex-1 grid-cols-1 gap-5 lg:grid-cols-[1.15fr_1fr]">
            <div className="min-h-[420px] lg:h-[calc(100vh-6rem)]">
              <TrackListPanel
                allTracks={tracks}
                tracks={filteredAndSortedTracks}
                totalTrackCount={tracks.length}
                activeTrackId={activeTrackId}
                isPlaying={isPlaying}
                libraryTab={libraryTab}
                selectedAlbum={selectedAlbum}
                selectedArtist={selectedArtist}
                searchQuery={searchQuery}
                sortMode={sortMode}
                favoriteCount={favoriteCount}
                recentCount={recentCount}
                isSelectionMode={isSelectionMode}
                selectedIds={selectedIds}
                jumpSignal={jumpSignal}
                onLibraryTabChange={handleLibraryTabChange}
                onSelectAlbum={setSelectedAlbum}
                onSelectArtist={setSelectedArtist}
                onPlayCollection={handlePlayCollection}
                onSearchChange={setSearchQuery}
                onSortChange={setSortMode}
                onToggleFavorite={handleToggleFavorite}
                onSelectTrack={(track) => loadAndPlayTrack(track, true)}
                onEnterSelectionWithTrack={handleEnterSelectionWithTrack}
                onExitSelectionMode={handleExitSelectionMode}
                onToggleTrackSelection={handleToggleTrackSelection}
                onSelectAllFiltered={handleSelectAllFiltered}
                onClearSelection={handleClearSelection}
                onBulkDelete={handleBulkDelete}
                onBulkQueue={handleBulkQueue}
                onPlayNextInQueue={handlePlayNextInQueue}
                onOpenTrackLyrics={(id) => {
                  setModalTrackId(id);
                  setShowLyricsModal(true);
                }}
                onOpenTrackRingtone={(id) => {
                  setModalTrackId(id);
                  setShowRingtoneModal(true);
                }}
                onOpenTrackEdit={(id) => {
                  setModalTrackId(id);
                  setShowEditModal(true);
                }}
                onJumpToActive={handleJumpToActive}
                onTriggerAddFiles={() => fileInputRef.current?.click()}
                onLoadStarterTracks={handleLoadStarterTracks}
                isImporting={isImporting}
              />
            </div>

            <div className="hidden lg:block lg:h-[calc(100vh-6rem)]">
              <PlayerStage
                activeTrack={activeTrack}
                isPlaying={isPlaying}
                currentTime={currentTime}
                duration={duration}
                volume={volume}
                shuffle={shuffle}
                repeatMode={repeatMode}
                sleepMinutesLeft={sleepMinutesLeft}
                eqPreset={eqPreset}
                playbackRate={playbackRate}
                analyser={analyserRef.current}
                upNextTracks={upNextTracks}
                manualQueueCount={manualQueueIds.length}
                onTogglePlay={handleTogglePlay}
                onNext={() => handlePlayNext(false)}
                onPrev={handlePlayPrev}
                onSeek={handleSeek}
                onVolumeChange={handleVolumeChange}
                onToggleShuffle={() => setShuffle((s) => !s)}
                onCycleRepeat={handleCycleRepeat}
                onSetSleepTimer={handleSetSleepTimer}
                onSelectTrack={(track) => loadAndPlayTrack(track, true)}
                onClearManualQueue={() => {
                  setManualQueueIds([]);
                  showNotice('صف پخش انتخابی پاک شد.');
                }}
                onJumpToActiveInList={handleJumpToActive}
                onToggleFavorite={handleToggleFavorite}
                onOpenLyrics={() => {
                  setModalTrackId(activeTrack?.id ?? null);
                  setShowLyricsModal(true);
                }}
                onOpenRingtoneMaker={() => {
                  setModalTrackId(activeTrack?.id ?? null);
                  setShowRingtoneModal(true);
                }}
                onOpenEqualizer={() => setShowEqModal(true)}
                onOpenEditTrack={() => {
                  setModalTrackId(activeTrack?.id ?? null);
                  setShowEditModal(true);
                }}
              />
            </div>
          </div>
        )}
      </main>

      {/* Mobile floating jump-to-active button: bottom-left in Persian (RTL), bottom-right in English (LTR) */}
      <AnimatePresence>
        {activeTrack && !isSelectionMode && !isMobileSheetOpen && (
          <motion.button
            type="button"
            initial={{ opacity: 0, scale: 0.88, y: 10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.88, y: 10 }}
            transition={{ duration: 0.16 }}
            onClick={handleJumpToActive}
            aria-label="پرش به آهنگ در حال پخش"
            className="fixed bottom-[86px] end-4 z-30 inline-flex min-h-[42px] items-center gap-1.5 rounded-full border border-sky-400/35 bg-zinc-900/90 px-3.5 text-xs font-medium text-sky-200 shadow-xl shadow-black/50 backdrop-blur-xl active:scale-95 transition whitespace-nowrap lg:hidden"
          >
            <LocateFixed className="h-4 w-4 text-sky-300" aria-hidden="true" />
            <span>آهنگ فعلی</span>
          </motion.button>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {activeTrack && !isSelectionMode && !isMobileSheetOpen && (
          <MobileMiniPlayer
            activeTrack={activeTrack}
            isPlaying={isPlaying}
            currentTime={currentTime}
            duration={duration}
            onTogglePlay={handleTogglePlay}
            onNext={() => handlePlayNext(false)}
            onExpand={() => setIsMobileSheetOpen(true)}
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {isMobileSheetOpen && (
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={{ type: 'spring', stiffness: 300, damping: 32 }}
            className="fixed inset-0 z-50 bg-zinc-950 p-3 lg:hidden"
          >
            <motion.div
              style={{ y: sheetDragY }}
              onTouchStart={(e) => {
                if (e.touches.length !== 1) return;
                const tag = (e.target as HTMLElement | null)?.tagName;
                if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') return;
                sheetSwipeRef.current = {
                  startX: e.touches[0].clientX,
                  startY: e.touches[0].clientY,
                  startTime: performance.now(),
                  lastDy: 0,
                  locked: false,
                  aborted: false,
                };
              }}
              onTouchMove={(e) => {
                const state = sheetSwipeRef.current;
                if (!state || state.aborted || e.touches.length !== 1) return;
                const dx = e.touches[0].clientX - state.startX;
                const dy = e.touches[0].clientY - state.startY;
                if (!state.locked) {
                  const scrollTop = playerSectionRef.current?.scrollTop ?? 0;
                  if (dy > 10 && dy > Math.abs(dx) * 1.15 && scrollTop <= 2) {
                    state.locked = true;
                  } else if (Math.abs(dx) > 12 || dy < -8) {
                    state.aborted = true;
                    return;
                  }
                }
                if (state.locked) {
                  const clamped = Math.max(0, dy);
                  state.lastDy = clamped;
                  sheetDragY.set(clamped);
                }
              }}
              onTouchEnd={() => {
                const state = sheetSwipeRef.current;
                sheetSwipeRef.current = null;
                if (!state || !state.locked) return;
                const elapsed = Math.max(1, performance.now() - state.startTime);
                const vy = state.lastDy / elapsed;
                if (state.lastDy > 90 || vy > 0.45) {
                  setIsMobileSheetOpen(false);
                } else {
                  animate(sheetDragY, 0, { type: 'spring', stiffness: 360, damping: 32 });
                }
              }}
              onTouchCancel={() => {
                sheetSwipeRef.current = null;
                animate(sheetDragY, 0, { type: 'spring', stiffness: 360, damping: 32 });
              }}
              onPointerDown={(e) => {
                if (e.pointerType === 'touch') return;
                const el = e.target as HTMLElement | null;
                if (el?.closest('button, input, select, textarea, a')) return;
                sheetSwipeRef.current = {
                  startX: e.clientX,
                  startY: e.clientY,
                  startTime: performance.now(),
                  lastDy: 0,
                  locked: false,
                  aborted: false,
                };
              }}
              onPointerMove={(e) => {
                if (e.pointerType === 'touch') return;
                const state = sheetSwipeRef.current;
                if (!state || state.aborted) return;
                const dx = e.clientX - state.startX;
                const dy = e.clientY - state.startY;
                if (!state.locked) {
                  const scrollTop = playerSectionRef.current?.scrollTop ?? 0;
                  if (dy > 10 && dy > Math.abs(dx) * 1.15 && scrollTop <= 2) {
                    state.locked = true;
                  } else if (Math.abs(dx) > 12 || dy < -8) {
                    state.aborted = true;
                    return;
                  }
                }
                if (state.locked) {
                  const clamped = Math.max(0, dy);
                  state.lastDy = clamped;
                  sheetDragY.set(clamped);
                }
              }}
              onPointerUp={(e) => {
                if (e.pointerType === 'touch') return;
                const state = sheetSwipeRef.current;
                sheetSwipeRef.current = null;
                if (!state || !state.locked) return;
                const elapsed = Math.max(1, performance.now() - state.startTime);
                const vy = state.lastDy / elapsed;
                if (state.lastDy > 90 || vy > 0.45) {
                  setIsMobileSheetOpen(false);
                } else {
                  animate(sheetDragY, 0, { type: 'spring', stiffness: 360, damping: 32 });
                }
              }}
              className="h-full w-full"
            >
              <PlayerStage
                activeTrack={activeTrack}
                isPlaying={isPlaying}
                currentTime={currentTime}
                duration={duration}
                volume={volume}
                shuffle={shuffle}
                repeatMode={repeatMode}
                sleepMinutesLeft={sleepMinutesLeft}
                eqPreset={eqPreset}
                playbackRate={playbackRate}
                analyser={analyserRef.current}
                upNextTracks={upNextTracks}
                manualQueueCount={manualQueueIds.length}
                onTogglePlay={handleTogglePlay}
                onNext={() => handlePlayNext(false)}
                onPrev={handlePlayPrev}
                onSeek={handleSeek}
                onVolumeChange={handleVolumeChange}
                onToggleShuffle={() => setShuffle((s) => !s)}
                onCycleRepeat={handleCycleRepeat}
                onSetSleepTimer={handleSetSleepTimer}
                onSelectTrack={(track) => loadAndPlayTrack(track, true)}
                onClearManualQueue={() => {
                  setManualQueueIds([]);
                  showNotice('صف پخش انتخابی پاک شد.');
                }}
                onJumpToActiveInList={handleJumpToActive}
                onToggleFavorite={handleToggleFavorite}
                onOpenLyrics={() => {
                  setModalTrackId(activeTrack?.id ?? null);
                  setShowLyricsModal(true);
                }}
                onOpenRingtoneMaker={() => {
                  setModalTrackId(activeTrack?.id ?? null);
                  setShowRingtoneModal(true);
                }}
                onOpenEqualizer={() => setShowEqModal(true)}
                onOpenEditTrack={() => {
                  setModalTrackId(activeTrack?.id ?? null);
                  setShowEditModal(true);
                }}
                onCloseMobileSheet={() => setIsMobileSheetOpen(false)}
                scrollContainerRef={playerSectionRef}
              />
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showLyricsModal && modalTrack && (
          <LyricsModal
            track={modalTrack}
            currentTime={modalTrack.id === activeTrack?.id ? currentTime : 0}
            duration={modalTrack.id === activeTrack?.id ? duration : modalTrack.duration}
            isPlaying={modalTrack.id === activeTrack?.id ? isPlaying : false}
            onTogglePlay={
              modalTrack.id === activeTrack?.id
                ? handleTogglePlay
                : () => loadAndPlayTrack(modalTrack, true)
            }
            onSeek={(t) => {
              if (modalTrack.id === activeTrack?.id) {
                handleSeek(t);
              }
            }}
            onSaveLyrics={handleSaveLyrics}
            onClose={() => setShowLyricsModal(false)}
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showRingtoneModal && modalTrack && (
          <RingtoneMakerModal
            track={modalTrack}
            onClose={() => setShowRingtoneModal(false)}
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showEqModal && (
          <EqualizerModal
            gains={eqGains}
            preset={eqPreset}
            playbackRate={playbackRate}
            onSelectPreset={handleSelectEqPreset}
            onChangeBand={handleChangeEqBand}
            onChangePlaybackRate={setPlaybackRate}
            onClose={() => setShowEqModal(false)}
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showEditModal && modalTrack && (
          <EditTrackModal
            track={modalTrack}
            onSave={handleSaveTrackDetails}
            onClose={() => setShowEditModal(false)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
