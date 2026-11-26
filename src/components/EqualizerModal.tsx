import React from 'react';
import { motion } from 'motion/react';
import { RotateCcw, Sliders, X } from 'lucide-react';
import { EqPreset } from '../types/music';

export const EQ_BANDS = [
  { label: '60Hz', desc: 'بیس عمیق' },
  { label: '230Hz', desc: 'گرمای صدا' },
  { label: '910Hz', desc: 'وکال' },
  { label: '3.6kHz', desc: 'وضوح' },
  { label: '14kHz', desc: 'درخشش' },
];

export const EQ_PRESET_VALUES: Record<Exclude<EqPreset, EqPreset.Custom>, number[]> = {
  [EqPreset.Flat]: [0, 0, 0, 0, 0],
  [EqPreset.BassBoost]: [7, 5, 0, 1, 2],
  [EqPreset.Pop]: [-1, 2, 4, 3, -1],
  [EqPreset.Rock]: [5, 2, -1, 3, 5],
  [EqPreset.Vocal]: [-3, -1, 5, 4, 1],
  [EqPreset.Acoustic]: [3, 2, 1, 3, 4],
};

const PRESET_LABELS: { key: Exclude<EqPreset, EqPreset.Custom>; label: string }[] = [
  { key: EqPreset.Flat, label: 'عادی' },
  { key: EqPreset.BassBoost, label: 'بیس قوی' },
  { key: EqPreset.Pop, label: 'پاپ' },
  { key: EqPreset.Rock, label: 'راک' },
  { key: EqPreset.Vocal, label: 'وکال شفاف' },
  { key: EqPreset.Acoustic, label: 'آکوستیک' },
];

const SPEED_OPTIONS = [0.75, 0.9, 1, 1.15, 1.25, 1.5];

interface EqualizerModalProps {
  gains: number[];
  preset: EqPreset;
  playbackRate: number;
  onSelectPreset: (preset: Exclude<EqPreset, EqPreset.Custom>) => void;
  onChangeBand: (index: number, db: number) => void;
  onChangePlaybackRate: (rate: number) => void;
  onClose: () => void;
}

export const EqualizerModal: React.FC<EqualizerModalProps> = ({
  gains,
  preset,
  playbackRate,
  onSelectPreset,
  onChangeBand,
  onChangePlaybackRate,
  onClose,
}) => {
  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="اکولایزر و سرعت پخش"
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 p-0 backdrop-blur-sm sm:items-center sm:p-4"
    >
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: 24 }}
        transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
        className="flex max-h-[88vh] w-full max-w-lg flex-col rounded-t-3xl border border-white/15 bg-zinc-900/95 text-zinc-100 backdrop-blur-xl sm:rounded-2xl"
      >
        <div className="flex items-center justify-between gap-3 border-b border-white/[0.08] px-5 py-4">
          <div className="flex items-center gap-2">
            <Sliders className="h-4 w-4 text-sky-300" aria-hidden="true" />
            <h2 className="text-sm font-semibold text-white">اکولایزر ۵ بانده و سرعت پخش</h2>
          </div>
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => {
                onSelectPreset(EqPreset.Flat);
                onChangePlaybackRate(1);
              }}
              className="inline-flex min-h-[38px] items-center gap-1 rounded-xl px-2.5 text-xs text-zinc-400 hover:bg-white/10 hover:text-white"
            >
              <RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />
              <span>بازنشانی</span>
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

        <div className="flex-1 space-y-6 overflow-y-auto p-5">
          <div>
            <p className="mb-2.5 text-xs font-medium text-zinc-400">حالت‌های آماده صدا</p>
            <div className="grid grid-cols-3 gap-2">
              {PRESET_LABELS.map((item) => (
                <button
                  key={item.key}
                  type="button"
                  onClick={() => onSelectPreset(item.key)}
                  className={`min-h-[40px] rounded-xl border px-3 text-xs font-medium transition ${
                    preset === item.key
                      ? 'border-sky-400/50 bg-sky-500/20 text-white'
                      : 'border-white/10 bg-white/[0.04] text-zinc-300 hover:bg-white/[0.08]'
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </div>

          <div className="space-y-3 rounded-2xl border border-white/10 bg-black/30 p-4">
            <p className="text-xs font-medium text-zinc-400">تنظیم دستی فرکانس‌ها (dB)</p>
            {EQ_BANDS.map((band, idx) => {
              const value = gains[idx] ?? 0;
              return (
                <div key={band.label} className="flex items-center gap-3">
                  <div className="w-20 shrink-0">
                    <p className="font-mono text-xs font-medium text-zinc-200" dir="ltr">
                      {band.label}
                    </p>
                    <p className="text-[11px] text-zinc-500">{band.desc}</p>
                  </div>
                  <input
                    type="range"
                    min={-12}
                    max={12}
                    step={1}
                    value={value}
                    dir="ltr"
                    onChange={(e) => onChangeBand(idx, Number(e.target.value))}
                    aria-label={band.label}
                    className="h-2 flex-1 cursor-pointer accent-sky-400"
                  />
                  <span
                    className="w-12 shrink-0 text-right font-mono text-xs tabular-nums text-sky-300"
                    dir="ltr"
                  >
                    {value > 0 ? `+${value}` : value} dB
                  </span>
                </div>
              );
            })}
          </div>

          <div>
            <div className="mb-2.5 flex items-center justify-between text-xs">
              <span className="font-medium text-zinc-400">سرعت پخش (Playback Speed)</span>
              <span className="font-mono tabular-nums text-sky-300" dir="ltr">
                {playbackRate}x
              </span>
            </div>
            <div className="grid grid-cols-6 gap-1.5" dir="ltr">
              {SPEED_OPTIONS.map((rate) => (
                <button
                  key={rate}
                  type="button"
                  onClick={() => onChangePlaybackRate(rate)}
                  className={`min-h-[40px] rounded-xl border font-mono text-xs tabular-nums transition ${
                    playbackRate === rate
                      ? 'border-indigo-400/50 bg-indigo-500/25 text-white'
                      : 'border-white/10 bg-white/[0.04] text-zinc-300 hover:bg-white/[0.08]'
                  }`}
                >
                  {rate}x
                </button>
              ))}
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  );
};
