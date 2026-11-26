import React, { useEffect, useRef, useState } from 'react';
import { motion } from 'motion/react';
import {
  AlarmClock,
  Bell,
  Download,
  Loader2,
  Pause,
  PhoneCall,
  Play,
  Share2,
  X,
} from 'lucide-react';
import { RingtoneTarget, TrackMeta } from '../types/music';
import { formatDuration, getTrackBlob } from '../utils/musicStorage';
import { triggerFileDownload, trimAudioBlobToWav } from '../utils/ringtoneExporter';

interface RingtoneMakerModalProps {
  track: TrackMeta;
  onClose: () => void;
}

const TARGET_CONFIG: Record<
  RingtoneTarget,
  { label: string; maxDuration: number; prefix: string }
> = {
  [RingtoneTarget.Ringtone]: {
    label: 'آهنگ زنگ (۳۰ ثانیه)',
    maxDuration: 30,
    prefix: 'ringtone',
  },
  [RingtoneTarget.Alarm]: {
    label: 'آلارم بیداری (۲۰ ثانیه)',
    maxDuration: 20,
    prefix: 'alarm',
  },
  [RingtoneTarget.Notification]: {
    label: 'صدای اعلان (۵ ثانیه)',
    maxDuration: 5,
    prefix: 'notification',
  },
};

export const RingtoneMakerModal: React.FC<RingtoneMakerModalProps> = ({
  track,
  onClose,
}) => {
  const totalSec = Math.max(2, Math.floor(track.duration) || 30);
  const [target, setTarget] = useState<RingtoneTarget>(RingtoneTarget.Ringtone);
  const [startSec, setStartSec] = useState(0);
  const [endSec, setEndSec] = useState(Math.min(totalSec, 30));
  const [fadeEdges, setFadeEdges] = useState(true);
  const [isExporting, setIsExporting] = useState(false);
  const [isPreviewing, setIsPreviewing] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [exportedMessage, setExportedMessage] = useState<string | null>(null);

  const previewAudioRef = useRef<HTMLAudioElement | null>(null);
  const previewUrlRef = useRef<string | null>(null);

  const stopPreview = () => {
    if (previewAudioRef.current) {
      previewAudioRef.current.pause();
      previewAudioRef.current.src = '';
    }
    if (previewUrlRef.current) {
      URL.revokeObjectURL(previewUrlRef.current);
      previewUrlRef.current = null;
    }
    setIsPreviewing(false);
  };

  useEffect(() => {
    return () => {
      stopPreview();
    };
  }, []);

  const handleSelectTarget = (nextTarget: RingtoneTarget) => {
    stopPreview();
    setTarget(nextTarget);
    const desiredLen = TARGET_CONFIG[nextTarget].maxDuration;
    const clampedEnd = Math.min(totalSec, startSec + desiredLen);
    setEndSec(Math.max(startSec + 1, clampedEnd));
  };

  const handleStartChange = (val: number) => {
    stopPreview();
    const clampedStart = Math.max(0, Math.min(val, totalSec - 1));
    setStartSec(clampedStart);
    if (endSec <= clampedStart) {
      setEndSec(Math.min(totalSec, clampedStart + TARGET_CONFIG[target].maxDuration));
    }
  };

  const handleEndChange = (val: number) => {
    stopPreview();
    const clampedEnd = Math.max(startSec + 1, Math.min(val, totalSec));
    setEndSec(clampedEnd);
  };

  const handlePreviewClip = async () => {
    if (isPreviewing) {
      stopPreview();
      return;
    }
    setErrorMsg(null);
    setIsExporting(true);
    try {
      const sourceBlob = await getTrackBlob(track.id);
      if (!sourceBlob) throw new Error('فایل صوتی پیدا نشد.');
      const { blob } = await trimAudioBlobToWav(sourceBlob, startSec, endSec, fadeEdges);
      const url = URL.createObjectURL(blob);
      previewUrlRef.current = url;
      const audio = new Audio(url);
      previewAudioRef.current = audio;
      audio.onended = () => stopPreview();
      await audio.play();
      setIsPreviewing(true);
    } catch {
      setErrorMsg('پخش پیش‌نمایش این برش صوتی ممکن نشد.');
    } finally {
      setIsExporting(false);
    }
  };

  const handleExport = async (useNativeShare = false) => {
    stopPreview();
    setErrorMsg(null);
    setExportedMessage(null);
    setIsExporting(true);

    try {
      const sourceBlob = await getTrackBlob(track.id);
      if (!sourceBlob) throw new Error('فایل صوتی این قطعه در حافظه پیدا نشد.');

      const { blob } = await trimAudioBlobToWav(sourceBlob, startSec, endSec, fadeEdges);
      const safeTitle = track.title.replace(/[^\p{L}\p{N}_-]+/gu, '-').slice(0, 32) || 'audio';
      const filename = `${TARGET_CONFIG[target].prefix}-${safeTitle}.wav`;

      if (useNativeShare && typeof navigator.share === 'function') {
        const file = new File([blob], filename, { type: 'audio/wav' });
        if (navigator.canShare?.({ files: [file] })) {
          await navigator.share({
            files: [file],
            title: filename,
          });
          setExportedMessage(`فایل «${filename}» ارسال شد.`);
          return;
        }
      }

      triggerFileDownload(blob, filename);
      setExportedMessage(
        `فایل «${filename}» ذخیره شد. حالا از تنظیمات صدا در گوشی آن را انتخاب کنید.`
      );
    } catch (err) {
      if ((err as Error).name === 'AbortError') return;
      setErrorMsg('تبدیل و برش این فایل صوتی ممکن نشد.');
    } finally {
      setIsExporting(false);
    }
  };

  const clipDuration = Math.max(1, endSec - startSec);
  const canShareFiles =
    typeof navigator !== 'undefined' && typeof navigator.share === 'function';

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="ساخت آهنگ زنگ، آلارم و صدای اعلان"
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 p-0 backdrop-blur-sm sm:items-center sm:p-4"
    >
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: 24 }}
        transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
        className="flex max-h-[90vh] w-full max-w-lg flex-col rounded-t-3xl border border-white/15 bg-zinc-900/95 text-zinc-100 backdrop-blur-xl sm:rounded-2xl"
      >
        <div className="flex items-center justify-between gap-3 border-b border-white/[0.08] px-5 py-4">
          <div className="min-w-0">
            <h2 className="truncate text-sm font-semibold text-white">
              ساخت زنگ گوشی، آلارم و اعلان
            </h2>
            <p className="mt-0.5 truncate text-xs text-zinc-400">
              {track.title} · کل زمان {formatDuration(totalSec)}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="بستن"
            className="flex h-10 w-10 items-center justify-center rounded-xl text-zinc-400 hover:bg-white/10 hover:text-white"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="flex-1 space-y-5 overflow-y-auto p-5">
          <div className="grid grid-cols-3 gap-2">
            {(
              [
                {
                  key: RingtoneTarget.Ringtone,
                  label: 'آهنگ زنگ',
                  icon: PhoneCall,
                },
                {
                  key: RingtoneTarget.Alarm,
                  label: 'آلارم بیداری',
                  icon: AlarmClock,
                },
                {
                  key: RingtoneTarget.Notification,
                  label: 'صدای اعلان',
                  icon: Bell,
                },
              ] as const
            ).map((opt) => {
              const Icon = opt.icon;
              const isSelected = target === opt.key;
              return (
                <button
                  key={opt.key}
                  type="button"
                  onClick={() => handleSelectTarget(opt.key)}
                  className={`flex min-h-[56px] flex-col items-center justify-center gap-1.5 rounded-2xl border px-2 py-2.5 text-xs font-medium transition ${
                    isSelected
                      ? 'border-sky-400/50 bg-sky-500/20 text-white'
                      : 'border-white/10 bg-white/[0.04] text-zinc-300 hover:bg-white/[0.08]'
                  }`}
                >
                  <Icon className="h-4 w-4" aria-hidden="true" />
                  <span>{opt.label}</span>
                </button>
              );
            })}
          </div>

          <div className="space-y-4 rounded-2xl border border-white/10 bg-black/30 p-4">
            <div className="flex items-center justify-between text-xs">
              <span className="text-zinc-400">بازه برش انتخاب‌شده</span>
              <span className="font-mono font-semibold tabular-nums text-sky-300" dir="ltr">
                {formatDuration(startSec)} – {formatDuration(endSec)} ({clipDuration}s)
              </span>
            </div>

            <div className="space-y-3" dir="ltr">
              <div>
                <div className="mb-1 flex justify-between text-[11px] text-zinc-400">
                  <span>Start</span>
                  <span className="font-mono tabular-nums">{formatDuration(startSec)}</span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={Math.max(0, totalSec - 1)}
                  step={1}
                  value={startSec}
                  onChange={(e) => handleStartChange(Number(e.target.value))}
                  className="h-2 w-full cursor-pointer accent-sky-400"
                />
              </div>

              <div>
                <div className="mb-1 flex justify-between text-[11px] text-zinc-400">
                  <span>End</span>
                  <span className="font-mono tabular-nums">{formatDuration(endSec)}</span>
                </div>
                <input
                  type="range"
                  min={1}
                  max={totalSec}
                  step={1}
                  value={endSec}
                  onChange={(e) => handleEndChange(Number(e.target.value))}
                  className="h-2 w-full cursor-pointer accent-indigo-400"
                />
              </div>
            </div>

            <label className="flex min-h-[38px] cursor-pointer items-center justify-between gap-3 pt-1 text-xs text-zinc-300">
              <span>محو شدن نرم صدا در ابتدا و انتها (Fade In / Out)</span>
              <input
                type="checkbox"
                checked={fadeEdges}
                onChange={(e) => setFadeEdges(e.target.checked)}
                className="h-4 w-4 rounded border-white/20 bg-zinc-800 accent-sky-400"
              />
            </label>
          </div>

          {errorMsg && (
            <div className="rounded-xl border border-red-400/30 bg-red-500/15 px-4 py-3 text-xs text-red-200">
              {errorMsg}
            </div>
          )}

          {exportedMessage && (
            <div className="rounded-xl border border-emerald-400/30 bg-emerald-500/15 px-4 py-3 text-xs leading-6 text-emerald-200">
              {exportedMessage}
            </div>
          )}

          <div className="flex flex-wrap items-center gap-2.5">
            <button
              type="button"
              disabled={isExporting}
              onClick={handlePreviewClip}
              className="inline-flex min-h-[46px] flex-1 items-center justify-center gap-2 rounded-xl border border-white/15 bg-white/[0.08] px-4 text-xs font-medium text-white hover:bg-white/[0.14] disabled:opacity-50 whitespace-nowrap"
            >
              {isExporting ? (
                <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
              ) : isPreviewing ? (
                <Pause className="h-4 w-4" aria-hidden="true" />
              ) : (
                <Play className="h-4 w-4" aria-hidden="true" />
              )}
              <span>{isPreviewing ? 'توقف پیش‌نمایش' : 'پخش همین بخش'}</span>
            </button>

            <button
              type="button"
              disabled={isExporting}
              onClick={() => handleExport(false)}
              className="inline-flex min-h-[46px] flex-1 items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 text-xs font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 whitespace-nowrap"
            >
              <Download className="h-4 w-4" aria-hidden="true" />
              <span>ذخیره در گوشی</span>
            </button>

            {canShareFiles && (
              <button
                type="button"
                disabled={isExporting}
                onClick={() => handleExport(true)}
                aria-label="اشتراک‌گذاری فایل برش‌خورده"
                className="inline-flex min-h-[46px] items-center justify-center gap-2 rounded-xl border border-white/15 bg-white/[0.06] px-4 text-xs font-medium text-zinc-200 hover:bg-white/[0.12] disabled:opacity-50"
              >
                <Share2 className="h-4 w-4" aria-hidden="true" />
              </button>
            )}
          </div>

          <p className="text-[11px] leading-5 text-zinc-400">
            مرورگرهای موبایل اجازه تغییر مستقیم زنگ سیستم را ندارند؛ پس از ذخیره فایل، در اندروید از «تنظیمات ← صدا ← آهنگ زنگ یا آلارم» و در آیفون با GarageBand فایل ذخیره‌شده را انتخاب کنید.
          </p>
        </div>
      </motion.div>
    </div>
  );
};
