import React, { useState } from 'react';
import { Disc3 } from 'lucide-react';

interface TrackArtworkProps {
  title: string;
  coverDataUrl?: string;
  accentHue: number;
  size?: 'sm' | 'md' | 'lg';
  isPlaying?: boolean;
}

export const TrackArtwork: React.FC<TrackArtworkProps> = ({
  title,
  coverDataUrl,
  accentHue,
  size = 'md',
  isPlaying = false,
}) => {
  const [imgFailed, setImgFailed] = useState(false);

  const sizeClasses =
    size === 'sm'
      ? 'h-11 w-11 rounded-xl'
      : size === 'md'
        ? 'h-14 w-14 rounded-2xl'
        : 'h-56 w-56 sm:h-64 sm:w-64 rounded-3xl';

  const iconSizes = size === 'sm' ? 'h-5 w-5' : size === 'md' ? 'h-6 w-6' : 'h-20 w-20';

  if (coverDataUrl && !imgFailed) {
    return (
      <div
        className={`relative shrink-0 overflow-hidden border border-white/15 bg-zinc-900 shadow-lg ${sizeClasses}`}
      >
        <img
          src={coverDataUrl}
          alt={title}
          referrerPolicy="no-referrer"
          onError={() => setImgFailed(true)}
          className="h-full w-full object-cover"
        />
      </div>
    );
  }

  return (
    <div
      className={`relative flex shrink-0 items-center justify-center overflow-hidden border border-white/15 shadow-lg ${sizeClasses}`}
      style={{
        background: `radial-gradient(circle at 25% 20%, hsla(${accentHue}, 82%, 62%, 0.42), transparent 62%), linear-gradient(145deg, hsla(${accentHue}, 45%, 16%, 0.95), rgba(9, 9, 11, 0.98))`,
      }}
    >
      <div
        className="pointer-events-none absolute inset-0 opacity-30"
        style={{
          backgroundImage:
            'radial-gradient(circle at 75% 80%, rgba(255,255,255,0.18), transparent 55%)',
        }}
      />
      <Disc3
        className={`relative text-white/80 transition-transform duration-700 ${iconSizes} ${
          isPlaying ? 'animate-[spin_9s_linear_infinite]' : ''
        }`}
        aria-hidden="true"
      />
    </div>
  );
};
