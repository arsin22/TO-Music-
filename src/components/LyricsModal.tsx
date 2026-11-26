import React, { useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import {
  Check,
  Edit3,
  Globe,
  Loader2,
  Pause,
  Play,
  Search,
  Sparkles,
  Trash2,
  X,
} from 'lucide-react';
import { TrackMeta } from '../types/music';
import {
  LyricSearchResult,
  parseLrcLines,
  searchOnlineLyrics,
} from '../utils/lyricsApi';
import { formatDuration } from '../utils/musicStorage';
import { TrackArtwork } from './TrackArtwork';

interface LyricsModalProps {
  track: TrackMeta;
  currentTime: number;
  duration: number;
  isPlaying: boolean;
  onTogglePlay: () => void;
  onSeek: (timeSec: number) => void;
  onSaveLyrics: (trackId: string, plainLyrics?: string, syncedLyrics?: string) => Promise<void>;
  onClose: () => void;
}

function isRtlLine(text: string): boolean {
  return /[\u0600-\u06FF]/.test(text);
}

export const LyricsModal: React.FC<LyricsModalProps> = ({
  track,
  currentTime,
  duration,
  isPlaying,
  onTogglePlay,
  onSeek,
  onSaveLyrics,
  onClose,
}) => {
  const [queryTitle, setQueryTitle] = useState(track.title);
  const [queryArtist, setQueryArtist] = useState(
    track.artist === 'کتابخانه شخصی' || track.artist === 'هنرمند ناشناس' ? '' : track.artist
  );
  const [results, setResults] = useState<LyricSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [isEditingManual, setIsEditingManual] = useState(false);
  const [showSearchBar, setShowSearchBar] = useState(false);
  const [manualText, setManualText] = useState(
    track.syncedLyrics || track.plainLyrics || ''
  );
  const [savedNotice, setSavedNotice] = useState(false);
  const [smoothTime, setSmoothTime] = useState(currentTime);
  const [syncOffsetSec, setSyncOffsetSec] = useState(0);

  const activeLineRef = useRef<HTMLButtonElement | null>(null);
  const abortRef = useRef<AbortController | null>(null);
  const anchorRef = useRef({ audioTime: currentTime, perfTime: performance.now() });

  useEffect(() => {
    anchorRef.current = { audioTime: currentTime, perfTime: performance.now() };
    setSmoothTime(currentTime);
  }, [currentTime]);

  useEffect(() => {
    if (!isPlaying) return undefined;
    let rafId = 0;
    const step = () => {
      const elapsedSec = (performance.now() - anchorRef.current.perfTime) / 1000;
      setSmoothTime(anchorRef.current.audioTime + Math.min(elapsedSec, 0.45));
      rafId = requestAnimationFrame(step);
    };
    rafId = requestAnimationFrame(step);
    return () => cancelAnimationFrame(rafId);
  }, [isPlaying]);

  useEffect(() => {
    setQueryTitle(track.title);
    setQueryArtist(
      track.artist === 'کتابخانه شخصی' || track.artist === 'هنرمند ناشناس'
        ? ''
        : track.artist
    );
    setManualText(track.syncedLyrics || track.plainLyrics || '');
    setResults([]);
    setSearchError(null);
    setSyncOffsetSec(0);
    setShowSearchBar(!track.syncedLyrics && !track.plainLyrics);
  }, [track.id, track.title, track.artist, track.plainLyrics, track.syncedLyrics]);

  useEffect(() => {
    return () => {
      abortRef.current?.abort();
    };
  }, []);

  const syncedLines = useMemo(() => {
    if (!track.syncedLyrics) return [];
    return parseLrcLines(track.syncedLyrics);
  }, [track.syncedLyrics]);

  const effectiveTime = Math.max(0, smoothTime + syncOffsetSec);

  const activeLineIndex = useMemo(() => {
    if (syncedLines.length === 0) return -1;
    let idx = -1;
    for (let i = 0; i < syncedLines.length; i++) {
      if (syncedLines[i].timeSec <= effectiveTime + 0.12) {
        idx = i;
      } else {
        break;
      }
    }
    return idx;
  }, [syncedLines, effectiveTime]);

  useEffect(() => {
    if (activeLineIndex >= 0 && activeLineRef.current) {
      activeLineRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, [activeLineIndex]);

  const handleFetchOnline = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!queryTitle.trim()) return;

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    setIsSearching(true);
    setSearchError(null);
    setResults([]);

    try {
      const found = await searchOnlineLyrics(queryTitle, queryArtist, controller.signal);
      if (found.length === 0) {
        setSearchError(
          'متن این آهنگ در سرویس آنلاین پیدا نشد. نام آهنگ یا خواننده را دقیق‌تر جستجو کنید یا متن را دستی وارد نمایید.'
        );
      } else {
        setResults(found.slice(0, 6));
      }
    } catch (err) {
      if ((err as Error).name === 'AbortError') return;
      setSearchError(
        err instanceof Error
          ? err.message
          : 'خطا در اتصال به سرویس آنلاین متن آهنگ.'
      );
    } finally {
      setIsSearching(false);
    }
  };

  useEffect(() => {
    if (!track.plainLyrics && !track.syncedLyrics && !track.isDemo) {
      handleFetchOnline();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [track.id]);

  const applySearchResult = async (item: LyricSearchResult) => {
    await onSaveLyrics(track.id, item.plainLyrics, item.syncedLyrics);
    setManualText(item.syncedLyrics || item.plainLyrics || '');
    setResults([]);
    setShowSearchBar(false);
    setSavedNotice(true);
    window.setTimeout(() => setSavedNotice(false), 1800);
  };

  const handleSaveManual = async () => {
    const trimmed = manualText.trim();
    const hasTimestamps = /\[\d{1,2}:\d{2}/.test(trimmed);
    await onSaveLyrics(
      track.id,
      hasTimestamps ? undefined : trimmed || undefined,
      hasTimestamps ? trimmed : undefined
    );
    setIsEditingManual(false);
    setShowSearchBar(false);
    setSavedNotice(true);
    window.setTimeout(() => setSavedNotice(false), 1800);
  };

  const handleClearLyrics = async () => {
    await onSaveLyrics(track.id, undefined, undefined);
    setManualText('');
  };

  const effectiveDuration = duration > 0 ? duration : track.duration;
  const progressPercent =
    effectiveDuration > 0 ? Math.min(100, (smoothTime / effectiveDuration) * 100) : 0;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="متن آهنگ"
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/75 p-0 backdrop-blur-md sm:items-center sm:p-4"
    >
      <motion.div
        initial={{ opacity: 0, y: 28, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: 28, scale: 0.98 }}
        transition={{ duration: 0.24, ease: [0.16, 1, 0.3, 1] }}
        className="relative flex h-[92vh] w-full max-w-2xl flex-col overflow-hidden rounded-t-3xl border border-white/15 bg-zinc-950/90 text-zinc-100 shadow-2xl backdrop-blur-2xl sm:h-[85vh] sm:rounded-3xl"
      >
        <motion.div
          aria-hidden="true"
          animate={
            isPlaying
              ? { scale: [1, 1.12, 1], opacity: [0.22, 0.34, 0.22] }
              : { scale: 1, opacity: 0.14 }
          }
          transition={{ duration: 4.5, repeat: Infinity, ease: 'easeInOut' }}
          className="pointer-events-none absolute -top-24 start-1/2 h-80 w-80 -translate-x-1/2 rounded-full blur-3xl"
          style={{
            backgroundColor: `hsl(${track.accentHue}, 82%, 56%)`,
          }}
        />

        <div className="relative z-10 flex items-center justify-between gap-3 border-b border-white/[0.08] bg-white/[0.03] px-5 py-3.5 backdrop-blur-xl">
          <div className="flex min-w-0 items-center gap-3">
            <TrackArtwork
              title={track.title}
              coverDataUrl={track.coverDataUrl}
              accentHue={track.accentHue}
              size="sm"
              isPlaying={isPlaying}
            />
            <div className="min-w-0">
              <h2 className="truncate text-sm font-semibold text-white">{track.title}</h2>
              <p className="mt-0.5 truncate text-xs text-zinc-400">
                {track.artist}
                {syncedLines.length > 0 ? ' · همگام‌سازی زنده' : ''}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1.5 shrink-0">
            {savedNotice && (
              <span className="inline-flex items-center gap-1 text-xs font-medium text-emerald-300">
                <Check className="h-3.5 w-3.5" aria-hidden="true" />
                <span>ذخیره شد</span>
              </span>
            )}
            <button
              type="button"
              onClick={() => setShowSearchBar((v) => !v)}
              aria-label="جستجوی آنلاین متن"
              className={`inline-flex min-h-[38px] items-center gap-1.5 rounded-xl border px-2.5 py-1.5 text-xs font-medium transition ${
                showSearchBar
                  ? 'border-sky-400/40 bg-sky-500/20 text-sky-200'
                  : 'border-white/10 bg-white/[0.06] text-zinc-200 hover:bg-white/[0.12]'
              }`}
            >
              <Search className="h-3.5 w-3.5" aria-hidden="true" />
              <span className="hidden sm:inline">جستجو</span>
            </button>
            <button
              type="button"
              onClick={() => setIsEditingManual((v) => !v)}
              className="inline-flex min-h-[38px] items-center gap-1.5 rounded-xl border border-white/10 bg-white/[0.06] px-2.5 py-1.5 text-xs font-medium text-zinc-200 hover:bg-white/[0.12]"
            >
              <Edit3 className="h-3.5 w-3.5" aria-hidden="true" />
              <span className="hidden sm:inline">{isEditingManual ? 'نمایش' : 'ویرایش'}</span>
            </button>
            <button
              type="button"
              onClick={onClose}
              aria-label="بستن"
              className="flex h-10 w-10 items-center justify-center rounded-xl text-zinc-400 hover:bg-white/10 hover:text-white"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        <AnimatePresence>
          {showSearchBar && (
            <motion.form
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.18 }}
              onSubmit={handleFetchOnline}
              className="relative z-10 overflow-hidden border-b border-white/[0.08] bg-black/30"
            >
              <div className="flex flex-col gap-2 px-5 py-3 sm:flex-row">
                <input
                  type="text"
                  value={queryTitle}
                  onChange={(e) => setQueryTitle(e.target.value)}
                  placeholder="نام آهنگ..."
                  className="min-h-[42px] flex-1 rounded-xl border border-white/10 bg-white/[0.05] px-3 text-xs text-white placeholder:text-zinc-500 focus:border-sky-400/60 focus:outline-none"
                />
                <input
                  type="text"
                  value={queryArtist}
                  onChange={(e) => setQueryArtist(e.target.value)}
                  placeholder="نام خواننده (اختیاری)"
                  className="min-h-[42px] flex-1 rounded-xl border border-white/10 bg-white/[0.05] px-3 text-xs text-white placeholder:text-zinc-500 focus:border-sky-400/60 focus:outline-none"
                />
                <button
                  type="submit"
                  disabled={isSearching || !queryTitle.trim()}
                  className="inline-flex min-h-[42px] items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 text-xs font-medium text-white hover:bg-indigo-500 disabled:opacity-50 whitespace-nowrap shrink-0"
                >
                  {isSearching ? (
                    <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
                  ) : (
                    <Globe className="h-4 w-4" aria-hidden="true" />
                  )}
                  <span>{isSearching ? 'در حال جستجو...' : 'دریافت آنلاین'}</span>
                </button>
              </div>
            </motion.form>
          )}
        </AnimatePresence>

        <div className="relative z-10 flex-1 overflow-y-auto px-5 py-8 sm:px-8">
          {results.length > 0 && (
            <div className="mb-6 space-y-2 rounded-2xl border border-indigo-400/25 bg-indigo-500/10 p-3.5 backdrop-blur-xl">
              <div className="flex items-center justify-between text-xs text-indigo-200">
                <span className="font-medium">نتایج آنلاین یافت‌شده (برای ذخیره انتخاب کنید):</span>
                <button
                  type="button"
                  onClick={() => setResults([])}
                  className="text-zinc-400 hover:text-white"
                >
                  بستن
                </button>
              </div>
              {results.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => applySearchResult(item)}
                  className="flex min-h-[48px] w-full items-center justify-between gap-3 rounded-xl border border-white/10 bg-black/35 px-3.5 py-2 text-start hover:bg-white/10 transition"
                >
                  <div className="min-w-0">
                    <p className="truncate text-xs font-semibold text-white">{item.trackName}</p>
                    <p className="mt-0.5 truncate text-[11px] text-zinc-400">
                      {item.artistName}
                      {item.albumName ? ` · ${item.albumName}` : ''}
                      {item.syncedLyrics ? ' · متن همگام (Synced)' : ' · متن ساده'}
                    </p>
                  </div>
                  <span className="shrink-0 rounded-lg bg-indigo-600 px-2.5 py-1 text-[11px] font-medium text-white">
                    انتخاب
                  </span>
                </button>
              ))}
            </div>
          )}

          {searchError && (
            <div className="mb-5 rounded-xl border border-amber-400/25 bg-amber-500/10 px-4 py-3 text-xs leading-6 text-amber-200">
              {searchError}
            </div>
          )}

          {isEditingManual ? (
            <div className="space-y-3">
              <textarea
                value={manualText}
                onChange={(e) => setManualText(e.target.value)}
                rows={10}
                placeholder="متن آهنگ یا فرمت [00:15.20] LRC را اینجا وارد کنید..."
                className="w-full rounded-2xl border border-white/12 bg-white/[0.04] p-4 text-sm leading-7 text-zinc-100 placeholder:text-zinc-500 backdrop-blur-xl focus:border-sky-400/60 focus:outline-none"
              />
              <div className="flex flex-wrap items-center justify-between gap-2">
                <button
                  type="button"
                  onClick={handleClearLyrics}
                  className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-red-400/25 bg-red-500/10 px-3 text-xs font-medium text-red-200 hover:bg-red-500/20"
                >
                  <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                  <span>پاک کردن متن</span>
                </button>
                <button
                  type="button"
                  onClick={handleSaveManual}
                  className="inline-flex min-h-[42px] items-center gap-1.5 rounded-xl bg-emerald-600 px-5 text-xs font-medium text-white hover:bg-emerald-500"
                >
                  <Check className="h-4 w-4" aria-hidden="true" />
                  <span>ذخیره آفلاین متن</span>
                </button>
              </div>
            </div>
          ) : syncedLines.length > 0 ? (
            <div className="space-y-3 py-12">
              {syncedLines.map((line, index) => {
                const isCurrent = index === activeLineIndex;
                const isPast = activeLineIndex >= 0 && index < activeLineIndex;
                const distance =
                  activeLineIndex >= 0 ? Math.abs(index - activeLineIndex) : 0;

                const lineStart = line.timeSec;
                const rawNextGap =
                  syncedLines[index + 1]?.timeSec !== undefined
                    ? syncedLines[index + 1].timeSec - lineStart
                    : 4.5;
                // Cap overly long instrumental gaps so words finish illuminating at natural singing pace
                const estimatedSingDur = Math.max(1.6, line.text.length / 13 + 0.6);
                const lineDur = Math.max(
                  0.65,
                  Math.min(rawNextGap, Math.max(estimatedSingDur, Math.min(rawNextGap, 6)))
                );

                const progress = isCurrent
                  ? Math.max(0, Math.min(1, (effectiveTime - lineStart) / lineDur))
                  : isPast
                    ? 1
                    : 0;

                const rtl = isRtlLine(line.text);
                const blurStyle =
                  activeLineIndex < 0 || isCurrent
                    ? 'blur(0px)'
                    : distance === 1
                      ? 'blur(0.4px)'
                      : 'blur(1.2px)';
                const opacityValue =
                  activeLineIndex < 0
                    ? 0.72
                    : isCurrent
                      ? 1
                      : distance === 1
                        ? 0.54
                        : isPast
                          ? 0.28
                          : 0.34;

                const tokens = line.text.split(/(\s+)/);
                const totalChars = Math.max(
                  1,
                  tokens.reduce(
                    (acc, tok) => (/^\s+$/.test(tok) ? acc : acc + tok.length),
                    0
                  )
                );
                let consumedChars = 0;

                return (
                  <motion.button
                    key={`${line.timeSec}-${index}`}
                    ref={isCurrent ? activeLineRef : undefined}
                    type="button"
                    onClick={() => onSeek(Math.max(0, line.timeSec - syncOffsetSec))}
                    animate={{
                      scale: isCurrent ? 1.02 : 0.985,
                      opacity: opacityValue,
                    }}
                    transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
                    style={{ filter: blurStyle }}
                    dir={rtl ? 'rtl' : 'ltr'}
                    className={`group relative block w-full rounded-2xl px-4 py-3 text-start transition-colors ${
                      isCurrent
                        ? 'border border-white/15 bg-white/[0.08] shadow-[0_12px_36px_rgba(0,0,0,0.38)] backdrop-blur-2xl'
                        : 'border border-transparent hover:bg-white/[0.04]'
                    }`}
                  >
                    <div className="relative flex items-baseline justify-between gap-4">
                      <div
                        className={`flex-1 leading-relaxed tracking-tight ${
                          isCurrent
                            ? 'text-xl font-bold sm:text-2xl'
                            : 'text-base font-semibold text-zinc-200 sm:text-lg'
                        }`}
                      >
                        {isCurrent ? (
                          tokens.map((tok, tokIdx) => {
                            if (/^\s+$/.test(tok)) {
                              return <React.Fragment key={tokIdx}>{tok}</React.Fragment>;
                            }
                            const startRatio = consumedChars / totalChars;
                            consumedChars += tok.length;
                            const endRatio = consumedChars / totalChars;
                            const wordSpan = Math.max(0.01, endRatio - startRatio);
                            const wordProgress = Math.max(
                              0,
                              Math.min(1, (progress - startRatio) / wordSpan)
                            );
                            const pct = wordProgress * 100;
                            const gradDir = rtl ? 'to left' : 'to right';
                            const stopA = Math.max(0, pct - 8);
                            const stopB = Math.min(100, pct + 8);

                            return (
                              <span
                                key={tokIdx}
                                className="inline-block"
                                style={{
                                  backgroundImage:
                                    wordProgress >= 1
                                      ? 'linear-gradient(to right, #ffffff, #ffffff)'
                                      : wordProgress <= 0
                                        ? 'linear-gradient(to right, rgba(255,255,255,0.28), rgba(255,255,255,0.28))'
                                        : `linear-gradient(${gradDir}, #ffffff ${stopA.toFixed(1)}%, rgba(255,255,255,0.28) ${stopB.toFixed(1)}%)`,
                                  WebkitBackgroundClip: 'text',
                                  backgroundClip: 'text',
                                  color: 'transparent',
                                  WebkitTextFillColor: 'transparent',
                                  filter:
                                    wordProgress > 0.15
                                      ? `drop-shadow(0 0 10px hsla(${track.accentHue}, 90%, 70%, ${(0.45 * wordProgress).toFixed(2)}))`
                                      : 'none',
                                }}
                              >
                                {tok}
                              </span>
                            );
                          })
                        ) : (
                          <span>{line.text}</span>
                        )}
                      </div>

                      <span
                        className="shrink-0 font-mono text-[11px] tabular-nums text-zinc-500 opacity-0 transition-opacity group-hover:opacity-100"
                        dir="ltr"
                      >
                        {formatDuration(line.timeSec)}
                      </span>
                    </div>
                  </motion.button>
                );
              })}
            </div>
          ) : track.plainLyrics ? (
            <div className="whitespace-pre-wrap rounded-3xl border border-white/12 bg-white/[0.05] p-6 text-base leading-9 text-zinc-100 backdrop-blur-xl">
              {track.plainLyrics}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <Sparkles className="h-8 w-8 text-zinc-500" aria-hidden="true" />
              <p className="mt-3 text-sm font-medium text-zinc-300">
                هنوز متنی برای این آهنگ ذخیره نشده است
              </p>
              <p className="mt-1 max-w-sm text-xs leading-6 text-zinc-500">
                دکمه «دریافت آنلاین» را بزنید یا از گزینه «ویرایش» متن خودتان را ذخیره کنید.
              </p>
              <button
                type="button"
                onClick={() => {
                  setShowSearchBar(true);
                  handleFetchOnline();
                }}
                className="mt-5 inline-flex min-h-[42px] items-center gap-2 rounded-xl border border-white/15 bg-white/[0.07] px-4 text-xs font-medium text-white hover:bg-white/[0.12]"
              >
                <Search className="h-3.5 w-3.5" aria-hidden="true" />
                <span>جستجوی آنلاین متن</span>
              </button>
            </div>
          )}
        </div>

        <div className="relative z-10 border-t border-white/[0.08] bg-white/[0.04] px-5 py-3 backdrop-blur-2xl">
          {syncedLines.length > 0 && (
            <div className="mb-2 flex items-center justify-between gap-2 text-[11px] text-zinc-400">
              <span>تنظیم هماهنگی زمان (در صورت وجود تاخیر)</span>
              <div className="flex items-center gap-1" dir="ltr">
                <button
                  type="button"
                  onClick={() => setSyncOffsetSec((s) => Math.round((s - 0.5) * 10) / 10)}
                  className="rounded-lg border border-white/10 bg-white/[0.05] px-2 py-1 font-mono text-[11px] text-zinc-200 hover:bg-white/10"
                >
                  -0.5s
                </button>
                <button
                  type="button"
                  onClick={() => setSyncOffsetSec(0)}
                  className="min-w-[48px] rounded-lg px-2 py-1 text-center font-mono text-[11px] tabular-nums text-sky-300 hover:bg-white/10"
                >
                  {syncOffsetSec > 0 ? `+${syncOffsetSec.toFixed(1)}s` : `${syncOffsetSec.toFixed(1)}s`}
                </button>
                <button
                  type="button"
                  onClick={() => setSyncOffsetSec((s) => Math.round((s + 0.5) * 10) / 10)}
                  className="rounded-lg border border-white/10 bg-white/[0.05] px-2 py-1 font-mono text-[11px] text-zinc-200 hover:bg-white/10"
                >
                  +0.5s
                </button>
              </div>
            </div>
          )}

          <div className="flex items-center gap-3" dir="ltr">
            <button
              type="button"
              onClick={onTogglePlay}
              aria-label={isPlaying ? 'توقف' : 'پخش'}
              className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-white text-zinc-950 active:scale-90 transition"
            >
              {isPlaying ? (
                <Pause className="h-4 w-4 fill-current" />
              ) : (
                <Play className="h-4 w-4 translate-x-0.5 fill-current" />
              )}
            </button>

            <span className="w-10 font-mono text-[11px] tabular-nums text-zinc-400">
              {formatDuration(smoothTime)}
            </span>

            <div className="relative flex h-6 flex-1 items-center">
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/12">
                <div
                  className="h-full rounded-full"
                  style={{
                    width: `${progressPercent}%`,
                    background: `linear-gradient(to right, hsl(${track.accentHue}, 85%, 60%), hsl(${(track.accentHue + 32) % 360}, 92%, 70%))`,
                  }}
                />
              </div>
              <input
                type="range"
                min={0}
                max={effectiveDuration || 100}
                step={0.1}
                value={Math.min(smoothTime, effectiveDuration || 100)}
                onChange={(e) => onSeek(Number(e.target.value))}
                aria-label="نوار زمان پخش در متن آهنگ"
                className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
              />
            </div>

            <span className="w-10 text-right font-mono text-[11px] tabular-nums text-zinc-400">
              {formatDuration(effectiveDuration)}
            </span>
          </div>
        </div>
      </motion.div>
    </div>
  );
};
