import React, { useEffect, useRef } from 'react';

interface LiveVisualizerProps {
  analyser: AnalyserNode | null;
  isPlaying: boolean;
  accentHue: number;
}

const BAR_COUNT = 28;

export const LiveVisualizer: React.FC<LiveVisualizerProps> = ({
  analyser,
  isPlaying,
  accentHue,
}) => {
  const barsRef = useRef<(HTMLDivElement | null)[]>([]);
  const rafRef = useRef<number | null>(null);
  const phaseRef = useRef(0);

  useEffect(() => {
    const freqData = analyser ? new Uint8Array(analyser.frequencyBinCount) : null;

    const tick = () => {
      phaseRef.current += isPlaying ? 0.085 : 0.025;
      let hasLiveEnergy = false;

      if (analyser && freqData && isPlaying) {
        analyser.getByteFrequencyData(freqData);
        for (let i = 0; i < Math.min(16, freqData.length); i++) {
          if (freqData[i] > 2) {
            hasLiveEnergy = true;
            break;
          }
        }
      }

      for (let i = 0; i < BAR_COUNT; i++) {
        const el = barsRef.current[i];
        if (!el) continue;

        let level = 0.1;
        if (isPlaying) {
          if (hasLiveEnergy && freqData) {
            const binIndex = Math.min(
              freqData.length - 1,
              Math.floor((i / BAR_COUNT) * Math.min(freqData.length, 48))
            );
            const raw = freqData[binIndex] / 255;
            level = 0.14 + raw * 0.86;
          } else {
            const centerWeight = 1 - Math.abs(i - BAR_COUNT / 2) / (BAR_COUNT / 1.6);
            const waveA = Math.sin(phaseRef.current + i * 0.42);
            const waveB = Math.cos(phaseRef.current * 1.4 - i * 0.27);
            level = Math.max(0.14, Math.min(0.95, 0.35 + (waveA * 0.32 + waveB * 0.22) * (0.65 + centerWeight * 0.45)));
          }
        } else {
          level = 0.08 + 0.04 * Math.sin(i * 0.5);
        }

        el.style.transform = `scaleY(${level.toFixed(3)})`;
      }

      if (isPlaying) {
        rafRef.current = requestAnimationFrame(tick);
      }
    };

    tick();

    return () => {
      if (rafRef.current !== null) {
        cancelAnimationFrame(rafRef.current);
      }
    };
  }, [analyser, isPlaying]);

  return (
    <div
      className="flex h-12 w-full max-w-xs items-end justify-center gap-1 px-2"
      aria-hidden="true"
      dir="ltr"
    >
      {Array.from({ length: BAR_COUNT }).map((_, i) => (
        <div
          key={i}
          ref={(el) => {
            barsRef.current[i] = el;
          }}
          className="h-full w-1 origin-bottom rounded-full transition-opacity duration-200"
          style={{
            transform: 'scaleY(0.1)',
            opacity: isPlaying ? 0.92 : 0.32,
            background: `linear-gradient(to top, hsla(${accentHue}, 85%, 60%, 0.75), hsla(${(accentHue + 32) % 360}, 92%, 72%, 0.95))`,
          }}
        />
      ))}
    </div>
  );
};

export const MiniEqualizer: React.FC<{ isPlaying: boolean; accentHue: number }> = ({
  isPlaying,
  accentHue,
}) => {
  return (
    <div className="flex h-4 items-end gap-0.5" aria-hidden="true" dir="ltr">
      {[0.65, 1, 0.5, 0.85].map((height, idx) => (
        <span
          key={idx}
          className={`w-0.5 rounded-full transition-all ${
            isPlaying ? 'animate-pulse' : ''
          }`}
          style={{
            height: isPlaying ? `${Math.round(height * 14)}px` : '4px',
            backgroundColor: `hsl(${accentHue}, 86%, 68%)`,
            animationDuration: `${450 + idx * 140}ms`,
          }}
        />
      ))}
    </div>
  );
};
