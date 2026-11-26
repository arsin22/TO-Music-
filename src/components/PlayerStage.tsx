import React, { useRef, useState } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import {
  BellRing,
  ChevronDown,
  Edit3,
  FileText,
  Heart,
  LocateFixed,
  Pause,
  Play,
  Repeat,
  Repeat1,
  Shuffle,
  SkipBack,
  SkipForward,
  Sliders,
  Timer,
  Volume2,
  VolumeX,
} from 'lucide-react';
import { EqPreset, RepeatMode, TrackMeta } from '../types/music';
import { parseLrcLines } from '../utils/lyricsApi';
import { formatDuration } from '../utils/musicStorage';
import { LiveVisualizer, MiniEqualizer } from './LiveVisualizer';
import { TrackArtwork } from './TrackArtwork';

interface PlayerStageProps {
  activeTrack: TrackMeta | null;
  isPlaying: boolean;
  currentTime: number;
  duration: number;
  volume: number;
  shuffle: boolean;
  repeatMode: RepeatMode;
  sleepMinutesLeft: number | null;
  eqPreset: EqPreset;
  playbackRate: number;
  analyser: AnalyserNode | null;
  upNextTracks: TrackMeta[];
  manualQueueCount: number;
  onTogglePlay: () => void;
  onNext: () => void;
  onPrev: () => void;
  onSeek: (time: number) => void;
  onVolumeChange: (val: number) => void;
  onToggleShuffle: () => void;
  onCycleRepeat: () => void;
  onSetSleepTimer: (minutes: number | null) => void;
  onSelectTrack: (track: TrackMeta) => void;
  onClearManualQueue: () => void;
  onJumpToActiveInList: () => void;
  onToggleFavorite: (id: string) => void;
  onOpenLyrics: () => void;
  onOpenRingtoneMaker: () => void;
  onOpenEqualizer: () => void;
  onOpenEditTrack: () => void;
  onCloseMobileSheet?: () => void;
  scrollContainerRef?: React.RefObject<HTMLElement | null>;
}

export const PlayerStage: React.FC<PlayerStageProps> = ({
  activeTrack,
  isPlaying,
  currentTime,
  duration,
  volume,
  shuffle,
  repeatMode,
  sleepMinutesLeft,
  eqPreset,
  playbackRate,
  analyser,
  upNextTracks,
  manualQueueCount,
  onTogglePlay,
  onNext,
  onPrev,
  onSeek,
  onVolumeChange,
  onToggleShuffle,
  onCycleRepeat,
  onSetSleepTimer,
  onSelectTrack,
  onClearManualQueue,
  onJumpToActiveInList,
  onToggleFavorite,
  onOpenLyrics,
  onOpenRingtoneMaker,
  onOpenEqualizer,
  onOpenEditTrack,
  onCloseMobileSheet,
  scrollContainerRef,
}) => {
  const [showSleepMenu, setShowSleepMenu] = useState(false);
  const artSwipeRef = useRef<{ x: number; y: number } | null>(null);

  const liveLyricLine = React.useMemo(() => {
    if (!activeTrack?.syncedLyrics) return null;
    const lines = parseLrcLines(activeTrack.syncedLyrics);
    if (lines.length === 0) return null;
    let current = lines[0]?.text ?? null;
    for (let i = 0; i < lines.length; i++) {
      if (lines[i].timeSec <= currentTime + 0.2) {
        current = lines[i].text;
      } else {
        break;
      }
    }
    return current;
  }, [activeTrack?.syncedLyrics, currentTime]);

  if (!activeTrack) {
    return (
      <section className="flex h-full flex-col items-center justify-center rounded-3xl border border-white/[0.09] bg-white/[0.04] p-8 text-center backdrop-blur-xl">
        <div className="h-32 w-32 rounded-3xl border border-white/10 bg-white/[0.03]" />
        <h2 className="mt-6 text-base font-semibold text-white">آماده پخش موسیقی</h2>
        <p className="mt-2 max-w-xs text-xs leading-6 text-zinc-400">
          یکی از آهنگ‌های فهرست را انتخاب کنید تا پخش با انیمیشن و اکولایزر زنده آغاز شود.
        </p>
      </section>
    );
  }

  const effectiveDuration = duration > 0 ? duration : activeTrack.duration;
  const progressPercent =
    effectiveDuration > 0 ? Math.min(100, (currentTime / effectiveDuration) * 100) : 0;

  return (
    <section
      ref={scrollContainerRef}
      className="relative flex h-full flex-col justify-between overflow-y-auto overscroll-y-contain rounded-3xl border border-white/[0.09] bg-white/[0.04] p-5 sm:p-7 backdrop-blur-xl"
      style={{
        backgroundImage: `radial-gradient(circle at 50% 0%, hsla(${activeTrack.accentHue}, 82%, 52%, ${
          isPlaying ? '0.22' : '0.11'
        }), transparent 58%)`,
      }}
    >
      {onCloseMobileSheet && (
        <div
          aria-hidden="true"
          className="mx-auto mb-2.5 h-1.5 w-10 shrink-0 rounded-full bg-white/25"
        />
      )}
      <div className="flex items-center justify-between gap-3">
        {onCloseMobileSheet ? (
          <button
            type="button"
            onClick={onCloseMobileSheet}
            aria-label="بستن نمای تمام‌صفحه پخش‌کننده"
            className="inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl border border-white/10 bg-white/[0.06] text-zinc-200 hover:bg-white/[0.12] active:scale-95 transition"
          >
            <ChevronDown className="h-5 w-5" />
          </button>
        ) : (
          <span className="text-xs font-medium text-zinc-400">در حال پخش</span>
        )}

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => {
              onJumpToActiveInList();
              onCloseMobileSheet?.();
            }}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-white/12 bg-white/[0.06] px-3 py-1.5 text-xs font-medium text-zinc-200 hover:bg-white/[0.12] active:scale-95 transition whitespace-nowrap"
          >
            <LocateFixed className="h-3.5 w-3.5 text-sky-300" aria-hidden="true" />
            <span>نمایش در لیست</span>
          </button>

          <div className="relative">
            <button
              type="button"
              onClick={() => setShowSleepMenu((s) => !s)}
              className={`inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border px-3 py-1.5 text-xs font-medium transition whitespace-nowrap ${
                sleepMinutesLeft !== null
                  ? 'border-sky-400/40 bg-sky-500/20 text-sky-200'
                  : 'border-white/12 bg-white/[0.06] text-zinc-300 hover:bg-white/[0.12]'
              }`}
            >
              <Timer className="h-3.5 w-3.5" aria-hidden="true" />
              <span className="tabular-nums">
                {sleepMinutesLeft !== null ? `${sleepMinutesLeft} دقیقه` : 'تایمر خواب'}
              </span>
            </button>

            <AnimatePresence>
              {showSleepMenu && (
                <motion.div
                  initial={{ opacity: 0, y: 6, scale: 0.96 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: 6, scale: 0.96 }}
                  transition={{ duration: 0.15 }}
                  className="absolute end-0 top-12 z-30 w-40 rounded-2xl border border-white/15 bg-zinc-900/95 p-1.5 shadow-2xl backdrop-blur-xl"
                >
                  {[
                    { label: 'خاموش', value: null },
                    { label: '۱۵ دقیقه', value: 15 },
                    { label: '۳۰ دقیقه', value: 30 },
                    { label: '۴۵ دقیقه', value: 45 },
                    { label: '۶۰ دقیقه', value: 60 },
                  ].map((item) => (
                    <button
                      key={item.label}
                      type="button"
                      onClick={() => {
                        onSetSleepTimer(item.value);
                        setShowSleepMenu(false);
                      }}
                      className="flex min-h-[38px] w-full items-center justify-between rounded-xl px-3 text-xs text-zinc-200 hover:bg-white/10"
                    >
                      <span>{item.label}</span>
                    </button>
                  ))}
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
      </div>

      <div className="my-5 flex flex-col items-center text-center">
        <div
          onTouchStart={(e) => {
            if (e.touches.length === 1) {
              artSwipeRef.current = {
                x: e.touches[0].clientX,
                y: e.touches[0].clientY,
              };
            }
          }}
          onTouchEnd={(e) => {
            if (!artSwipeRef.current || e.changedTouches.length === 0) return;
            const dx = e.changedTouches[0].clientX - artSwipeRef.current.x;
            const dy = e.changedTouches[0].clientY - artSwipeRef.current.y;
            artSwipeRef.current = null;
            if (Math.abs(dx) > 52 && Math.abs(dx) > Math.abs(dy) * 1.4) {
              if (dx < 0) onNext();
              else onPrev();
            }
          }}
        >
          <AnimatePresence mode="wait">
            <motion.div
              key={activeTrack.id}
              initial={{ opacity: 0, scale: 0.92, y: 12 }}
              animate={{
                opacity: 1,
                scale: isPlaying ? 1 : 0.96,
                y: 0,
              }}
              exit={{ opacity: 0, scale: 0.92, y: -12 }}
              transition={{ type: 'spring', stiffness: 260, damping: 22 }}
              className="relative"
            >
              {isPlaying && (
                <motion.div
                  aria-hidden="true"
                  animate={{ scale: [1, 1.07, 1], opacity: [0.32, 0.55, 0.32] }}
                  transition={{ duration: 3.2, repeat: Infinity, ease: 'easeInOut' }}
                  className="pointer-events-none absolute -inset-3 rounded-[2rem] blur-2xl"
                  style={{
                    backgroundColor: `hsla(${activeTrack.accentHue}, 85%, 56%, 0.45)`,
                  }}
                />
              )}
              <div className="relative">
                <TrackArtwork
                  title={activeTrack.title}
                  coverDataUrl={activeTrack.coverDataUrl}
                  accentHue={activeTrack.accentHue}
                  size="lg"
                  isPlaying={isPlaying}
                />
              </div>
            </motion.div>
          </AnimatePresence>
        </div>

        <div className="mt-5 flex w-full justify-center">
          <LiveVisualizer
            analyser={analyser}
            isPlaying={isPlaying}
            accentHue={activeTrack.accentHue}
          />
        </div>

        <AnimatePresence mode="wait">
          <motion.div
            key={`${activeTrack.id}-meta`}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            transition={{ duration: 0.18 }}
            className="mt-4 flex w-full max-w-md items-center justify-between gap-3 text-start"
          >
            <div className="min-w-0 flex-1">
              <h2 className="truncate text-xl font-bold text-white">{activeTrack.title}</h2>
              <p className="mt-1 truncate text-sm text-zinc-400">
                {activeTrack.artist}
                {activeTrack.album ? ` · ${activeTrack.album}` : ''}
              </p>
            </div>
            <motion.button
              type="button"
              whileTap={{ scale: 0.85 }}
              onClick={() => onToggleFavorite(activeTrack.id)}
              aria-label={
                activeTrack.isFavorite ? 'حذف از علاقه‌مندی‌ها' : 'افزودن به علاقه‌مندی‌ها'
              }
              className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl border transition ${
                activeTrack.isFavorite
                  ? 'border-rose-400/40 bg-rose-500/20 text-rose-300'
                  : 'border-white/10 bg-white/[0.05] text-zinc-400 hover:bg-white/[0.11] hover:text-white'
              }`}
            >
              <Heart className={`h-5 w-5 ${activeTrack.isFavorite ? 'fill-current' : ''}`} />
            </motion.button>
          </motion.div>
        </AnimatePresence>

        {liveLyricLine && (
          <button
            type="button"
            onClick={onOpenLyrics}
            className="mt-3 w-full max-w-md rounded-2xl border border-white/[0.09] bg-white/[0.05] px-4 py-2 text-center backdrop-blur-xl hover:bg-white/[0.09] transition"
          >
            <AnimatePresence mode="wait">
              <motion.p
                key={liveLyricLine}
                initial={{ opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -4 }}
                transition={{ duration: 0.2 }}
                className="truncate text-xs font-medium text-sky-200"
              >
                {liveLyricLine}
              </motion.p>
            </AnimatePresence>
          </button>
        )}

        <div className="mt-5 w-full max-w-md" dir="ltr">
          <div className="relative flex h-6 items-center">
            <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/10">
              <div
                className="h-full rounded-full transition-[width] duration-100"
                style={{
                  width: `${progressPercent}%`,
                  background: `linear-gradient(to right, hsl(${activeTrack.accentHue}, 85%, 60%), hsl(${(activeTrack.accentHue + 35) % 360}, 90%, 68%))`,
                }}
              />
            </div>
            <input
              type="range"
              min={0}
              max={effectiveDuration || 100}
              step={0.1}
              value={Math.min(currentTime, effectiveDuration || 100)}
              onChange={(e) => onSeek(Number(e.target.value))}
              aria-label="نوار زمان پخش"
              className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
            />
          </div>
          <div className="mt-1 flex items-center justify-between font-mono text-xs tabular-nums text-zinc-400">
            <span>{formatDuration(currentTime)}</span>
            <span>{formatDuration(effectiveDuration)}</span>
          </div>
        </div>

        <div className="mt-3 flex w-full max-w-md items-center justify-between gap-2" dir="ltr">
          <motion.button
            type="button"
            whileTap={{ scale: 0.88 }}
            onClick={onToggleShuffle}
            aria-label="پخش تصادفی"
            className={`flex h-11 w-11 items-center justify-center rounded-xl transition-colors ${
              shuffle
                ? 'bg-sky-500/20 text-sky-300'
                : 'text-zinc-400 hover:bg-white/[0.07] hover:text-zinc-200'
            }`}
          >
            <Shuffle className="h-4 w-4" />
          </motion.button>

          <motion.button
            type="button"
            whileTap={{ scale: 0.88 }}
            onClick={onPrev}
            aria-label="آهنگ قبلی"
            className="flex h-12 w-12 items-center justify-center rounded-2xl border border-white/10 bg-white/[0.06] text-white hover:bg-white/[0.12] transition-colors"
          >
            <SkipBack className="h-5 w-5" />
          </motion.button>

          <motion.button
            type="button"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.92 }}
            transition={{ type: 'spring', stiffness: 420, damping: 24 }}
            onClick={onTogglePlay}
            aria-label={isPlaying ? 'توقف' : 'پخش'}
            className="flex h-16 w-16 items-center justify-center rounded-full bg-white text-zinc-950 shadow-xl shadow-black/40"
          >
            {isPlaying ? (
              <Pause className="h-7 w-7 fill-current" />
            ) : (
              <Play className="h-7 w-7 translate-x-0.5 fill-current" />
            )}
          </motion.button>

          <motion.button
            type="button"
            whileTap={{ scale: 0.88 }}
            onClick={onNext}
            aria-label="آهنگ بعدی"
            className="flex h-12 w-12 items-center justify-center rounded-2xl border border-white/10 bg-white/[0.06] text-white hover:bg-white/[0.12] transition-colors"
          >
            <SkipForward className="h-5 w-5" />
          </motion.button>

          <motion.button
            type="button"
            whileTap={{ scale: 0.88 }}
            onClick={onCycleRepeat}
            aria-label="حالت تکرار"
            className={`flex h-11 w-11 items-center justify-center rounded-xl transition-colors ${
              repeatMode !== RepeatMode.Off
                ? 'bg-sky-500/20 text-sky-300'
                : 'text-zinc-400 hover:bg-white/[0.07] hover:text-zinc-200'
            }`}
          >
            {repeatMode === RepeatMode.One ? (
              <Repeat1 className="h-4 w-4" />
            ) : (
              <Repeat className="h-4 w-4" />
            )}
          </motion.button>
        </div>

        <div className="mt-5 grid w-full max-w-md grid-cols-4 gap-2">
          <button
            type="button"
            onClick={onOpenLyrics}
            className={`flex min-h-[54px] flex-col items-center justify-center gap-1 rounded-2xl border px-2 py-2 text-[11px] font-medium transition whitespace-nowrap ${
              activeTrack.syncedLyrics || activeTrack.plainLyrics
                ? 'border-sky-400/35 bg-sky-500/15 text-sky-200'
                : 'border-white/10 bg-white/[0.04] text-zinc-300 hover:bg-white/[0.09]'
            }`}
          >
            <FileText className="h-4 w-4 shrink-0" aria-hidden="true" />
            <span>متن آهنگ</span>
          </button>

          <button
            type="button"
            onClick={onOpenEqualizer}
            className={`flex min-h-[54px] flex-col items-center justify-center gap-1 rounded-2xl border px-2 py-2 text-[11px] font-medium transition whitespace-nowrap ${
              eqPreset !== EqPreset.Flat || playbackRate !== 1
                ? 'border-indigo-400/35 bg-indigo-500/15 text-indigo-100'
                : 'border-white/10 bg-white/[0.04] text-zinc-300 hover:bg-white/[0.09]'
            }`}
          >
            <Sliders className="h-4 w-4 shrink-0" aria-hidden="true" />
            <span>اکولایزر</span>
          </button>

          <button
            type="button"
            onClick={onOpenRingtoneMaker}
            className="flex min-h-[54px] flex-col items-center justify-center gap-1 rounded-2xl border border-white/10 bg-white/[0.04] px-2 py-2 text-[11px] font-medium text-zinc-300 hover:bg-white/[0.09] transition whitespace-nowrap"
          >
            <BellRing className="h-4 w-4 shrink-0" aria-hidden="true" />
            <span>زنگ و آلارم</span>
          </button>

          <button
            type="button"
            onClick={onOpenEditTrack}
            className="flex min-h-[54px] flex-col items-center justify-center gap-1 rounded-2xl border border-white/10 bg-white/[0.04] px-2 py-2 text-[11px] font-medium text-zinc-300 hover:bg-white/[0.09] transition whitespace-nowrap"
          >
            <Edit3 className="h-4 w-4 shrink-0" aria-hidden="true" />
            <span>ویرایش</span>
          </button>
        </div>

        <div className="mt-4 flex w-full max-w-xs items-center gap-3" dir="ltr">
          <button
            type="button"
            onClick={() => onVolumeChange(volume > 0 ? 0 : 0.85)}
            aria-label="قطع یا وصل صدا"
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-zinc-400 hover:bg-white/10 hover:text-white"
          >
            {volume === 0 ? <VolumeX className="h-4 w-4" /> : <Volume2 className="h-4 w-4" />}
          </button>
          <input
            type="range"
            min={0}
            max={1}
            step={0.02}
            value={volume}
            onChange={(e) => onVolumeChange(Number(e.target.value))}
            aria-label="تنظیم صدا"
            className="h-1.5 flex-1 cursor-pointer accent-sky-400"
          />
          <span className="w-9 text-right font-mono text-[11px] tabular-nums text-zinc-400">
            {Math.round(volume * 100)}%
          </span>
        </div>
      </div>

      {upNextTracks.length > 0 && (
        <div className="border-t border-white/[0.08] pt-4">
          <div className="flex items-center justify-between text-xs text-zinc-400">
            <span className="font-medium text-zinc-300">
              {manualQueueCount > 0 ? 'صف پخش انتخابی شما' : 'در صف پخش بعدی'}
            </span>
            <div className="flex items-center gap-3">
              {manualQueueCount > 0 && (
                <button
                  type="button"
                  onClick={onClearManualQueue}
                  className="text-[11px] font-medium text-sky-300 hover:text-sky-200"
                >
                  پاک کردن صف
                </button>
              )}
              <span className="font-mono tabular-nums">{upNextTracks.length} قطعه</span>
            </div>
          </div>
          <div className="mt-2.5 space-y-1.5">
            {upNextTracks.slice(0, 5).map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => onSelectTrack(item)}
                className="flex min-h-[44px] w-full items-center justify-between gap-3 rounded-xl border border-white/[0.06] bg-white/[0.03] px-3 py-1.5 text-start hover:bg-white/[0.08] transition-colors"
              >
                <div className="min-w-0">
                  <p className="truncate text-xs font-medium text-zinc-200">{item.title}</p>
                  <p className="truncate text-[11px] text-zinc-500">{item.artist}</p>
                </div>
                <span className="shrink-0 font-mono text-[11px] tabular-nums text-zinc-500" dir="ltr">
                  {formatDuration(item.duration)}
                </span>
              </button>
            ))}
          </div>
        </div>
      )}
    </section>
  );
};

interface MobileMiniPlayerProps {
  activeTrack: TrackMeta;
  isPlaying: boolean;
  currentTime: number;
  duration: number;
  onTogglePlay: () => void;
  onNext: () => void;
  onExpand: () => void;
}

export const MobileMiniPlayer: React.FC<MobileMiniPlayerProps> = ({
  activeTrack,
  isPlaying,
  currentTime,
  duration,
  onTogglePlay,
  onNext,
  onExpand,
}) => {
  const effectiveDuration = duration > 0 ? duration : activeTrack.duration;
  const progressPercent =
    effectiveDuration > 0 ? Math.min(100, (currentTime / effectiveDuration) * 100) : 0;
  const draggedRef = React.useRef(false);

  return (
    <motion.div
      initial={{ y: 72, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      exit={{ y: 72, opacity: 0 }}
      transition={{ type: 'spring', stiffness: 320, damping: 28 }}
      className="fixed bottom-2 inset-x-3 z-40 lg:hidden"
    >
      <motion.div
        drag="y"
        dragConstraints={{ top: -90, bottom: 0 }}
        dragElastic={{ top: 0.35, bottom: 0.05 }}
        dragSnapToOrigin
        dragMomentum={false}
        onDragStart={() => {
          draggedRef.current = true;
        }}
        onDragEnd={(_, info) => {
          if (info.offset.y < -30 || info.velocity.y < -220) {
            onExpand();
          }
          window.setTimeout(() => {
            draggedRef.current = false;
          }, 80);
        }}
        className="overflow-hidden rounded-2xl border border-white/15 bg-zinc-900/90 shadow-2xl shadow-black/60 backdrop-blur-xl touch-none"
      >
        <div className="h-0.5 w-full bg-white/10" dir="ltr">
          <div
            className="h-full transition-[width] duration-150"
            style={{
              width: `${progressPercent}%`,
              backgroundColor: `hsl(${activeTrack.accentHue}, 85%, 64%)`,
            }}
          />
        </div>

        <div aria-hidden="true" className="mx-auto mt-1.5 h-1 w-8 rounded-full bg-white/25" />

        <div className="flex h-14 items-center justify-between gap-2 px-3 pb-1">
          <button
            type="button"
            onClick={() => {
              if (!draggedRef.current) onExpand();
            }}
            className="flex min-w-0 flex-1 items-center gap-3 text-start focus-visible:outline-2 focus-visible:outline-sky-400 rounded-xl"
          >
            <TrackArtwork
              title={activeTrack.title}
              coverDataUrl={activeTrack.coverDataUrl}
              accentHue={activeTrack.accentHue}
              size="sm"
              isPlaying={isPlaying}
            />
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <p className="truncate text-sm font-semibold text-white">{activeTrack.title}</p>
                <MiniEqualizer isPlaying={isPlaying} accentHue={activeTrack.accentHue} />
              </div>
              <p className="truncate text-xs text-zinc-400">{activeTrack.artist}</p>
            </div>
          </button>

          <div className="flex shrink-0 items-center gap-1" dir="ltr">
            <button
              type="button"
              onClick={onTogglePlay}
              aria-label={isPlaying ? 'توقف' : 'پخش'}
              className="flex h-11 w-11 items-center justify-center rounded-xl bg-white text-zinc-950 active:scale-90 transition"
            >
              {isPlaying ? (
                <Pause className="h-5 w-5 fill-current" />
              ) : (
                <Play className="h-5 w-5 translate-x-0.5 fill-current" />
              )}
            </button>

            <button
              type="button"
              onClick={onNext}
              aria-label="بعدی"
              className="flex h-11 w-11 items-center justify-center rounded-xl text-zinc-200 hover:bg-white/10 active:scale-90 transition"
            >
              <SkipForward className="h-4 w-4" />
            </button>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
};
