import React, { useState } from 'react';
import { motion } from 'motion/react';
import { Check, X } from 'lucide-react';
import { TrackMeta } from '../types/music';

interface EditTrackModalProps {
  track: TrackMeta;
  onSave: (id: string, title: string, artist: string, album: string) => Promise<void>;
  onClose: () => void;
}

export const EditTrackModal: React.FC<EditTrackModalProps> = ({
  track,
  onSave,
  onClose,
}) => {
  const [title, setTitle] = useState(track.title);
  const [artist, setArtist] = useState(track.artist);
  const [album, setAlbum] = useState(track.album);
  const [isSaving, setIsSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    setIsSaving(true);
    try {
      await onSave(
        track.id,
        title.trim(),
        artist.trim() || 'هنرمند ناشناس',
        album.trim() || 'کتابخانه شخصی'
      );
      onClose();
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="ویرایش مشخصات آهنگ"
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 p-0 backdrop-blur-sm sm:items-center sm:p-4"
    >
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: 20 }}
        transition={{ duration: 0.18 }}
        className="w-full max-w-md rounded-t-3xl border border-white/15 bg-zinc-900/95 p-6 text-zinc-100 backdrop-blur-xl sm:rounded-2xl"
      >
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-sm font-semibold text-white">ویرایش اطلاعات آهنگ</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="بستن"
            className="flex h-10 w-10 items-center justify-center rounded-xl text-zinc-400 hover:bg-white/10 hover:text-white"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="mb-1.5 block text-xs text-zinc-400">نام آهنگ</label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="min-h-[44px] w-full rounded-xl border border-white/12 bg-black/35 px-3.5 text-sm text-white focus:border-sky-400/60 focus:outline-none"
            />
          </div>

          <div>
            <label className="mb-1.5 block text-xs text-zinc-400">نام خواننده / هنرمند</label>
            <input
              type="text"
              value={artist}
              onChange={(e) => setArtist(e.target.value)}
              className="min-h-[44px] w-full rounded-xl border border-white/12 bg-black/35 px-3.5 text-sm text-white focus:border-sky-400/60 focus:outline-none"
            />
          </div>

          <div>
            <label className="mb-1.5 block text-xs text-zinc-400">آلبوم</label>
            <input
              type="text"
              value={album}
              onChange={(e) => setAlbum(e.target.value)}
              className="min-h-[44px] w-full rounded-xl border border-white/12 bg-black/35 px-3.5 text-sm text-white focus:border-sky-400/60 focus:outline-none"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="min-h-[44px] rounded-xl border border-white/15 bg-white/[0.06] px-4 text-xs font-medium text-zinc-200 hover:bg-white/[0.12]"
            >
              انصراف
            </button>
            <button
              type="submit"
              disabled={isSaving || !title.trim()}
              className="inline-flex min-h-[44px] items-center gap-1.5 rounded-xl bg-indigo-600 px-5 text-xs font-semibold text-white hover:bg-indigo-500 disabled:opacity-50"
            >
              <Check className="h-4 w-4" aria-hidden="true" />
              <span>ذخیره تغییرات</span>
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  );
};
