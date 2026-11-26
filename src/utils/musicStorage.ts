import JSZip from 'jszip';
import { TrackMeta, TrackRecord } from '../types/music';

const DB_NAME = 'avaplayer-db';
const DB_VERSION = 1;
const STORE_NAME = 'tracks';

const AUDIO_EXTENSIONS = new Set([
  'mp3',
  'wav',
  'ogg',
  'm4a',
  'aac',
  'flac',
  'webm',
  'opus',
]);

function openMusicDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'id' });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export async function getAllTrackMetas(): Promise<TrackMeta[]> {
  const db = await openMusicDatabase();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly');
    const store = tx.objectStore(STORE_NAME);
    const request = store.getAll();
    request.onsuccess = () => {
      const records = (request.result || []) as TrackRecord[];
      const metas: TrackMeta[] = records.map(({ audioBlob: _blob, ...meta }) => meta);
      metas.sort((a, b) => b.addedAt - a.addedAt);
      resolve(metas);
    };
    request.onerror = () => reject(request.error);
  });
}

export async function getTrackBlob(id: string): Promise<Blob | null> {
  const db = await openMusicDatabase();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly');
    const store = tx.objectStore(STORE_NAME);
    const request = store.get(id);
    request.onsuccess = () => {
      const record = request.result as TrackRecord | undefined;
      resolve(record?.audioBlob ?? null);
    };
    request.onerror = () => reject(request.error);
  });
}

export async function saveTrackRecords(records: TrackRecord[]): Promise<void> {
  if (records.length === 0) return;
  const db = await openMusicDatabase();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);
    for (const rec of records) {
      store.put(rec);
    }
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function deleteTracksByIds(ids: string[]): Promise<void> {
  if (ids.length === 0) return;
  const db = await openMusicDatabase();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);
    for (const id of ids) {
      store.delete(id);
    }
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function updateTrackMeta(
  id: string,
  patch: Partial<TrackMeta>
): Promise<TrackMeta | null> {
  const db = await openMusicDatabase();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);
    const getReq = store.get(id);

    getReq.onsuccess = () => {
      const existing = getReq.result as TrackRecord | undefined;
      if (!existing) {
        resolve(null);
        return;
      }
      const updated: TrackRecord = {
        ...existing,
        ...patch,
        id: existing.id,
        audioBlob: existing.audioBlob,
      };
      const putReq = store.put(updated);
      putReq.onsuccess = () => {
        const { audioBlob: _blob, ...meta } = updated;
        resolve(meta);
      };
      putReq.onerror = () => reject(putReq.error);
    };

    getReq.onerror = () => reject(getReq.error);
  });
}

export function formatDuration(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds <= 0) return '0:00';
  const totalSec = Math.floor(seconds);
  const hrs = Math.floor(totalSec / 3600);
  const mins = Math.floor((totalSec % 3600) / 60);
  const secs = totalSec % 60;
  if (hrs > 0) {
    return `${hrs}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }
  return `${mins}:${String(secs).padStart(2, '0')}`;
}

function hashHue(seed: string): number {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = (hash * 31 + seed.charCodeAt(i)) % 360;
  }
  // Keep hues inside refined indigo/sky/violet/emerald spectrum
  const palette = [198, 218, 238, 258, 280, 174];
  return palette[Math.abs(hash) % palette.length];
}

function decodeId3Text(bytes: Uint8Array): string {
  if (bytes.length === 0) return '';
  const encoding = bytes[0];
  const payload = bytes.slice(1);
  try {
    if (encoding === 0) {
      return new TextDecoder('iso-8859-1').decode(payload).replace(/\0+$/, '').trim();
    }
    if (encoding === 1) {
      return new TextDecoder('utf-16').decode(payload).replace(/\0+$/, '').trim();
    }
    if (encoding === 2) {
      return new TextDecoder('utf-16be').decode(payload).replace(/\0+$/, '').trim();
    }
    return new TextDecoder('utf-8').decode(payload).replace(/\0+$/, '').trim();
  } catch {
    return '';
  }
}

function parseId3Picture(bytes: Uint8Array): string | undefined {
  if (bytes.length < 10) return undefined;
  const encoding = bytes[0];
  let idx = 1;
  while (idx < bytes.length && bytes[idx] !== 0) idx++;
  const mime = new TextDecoder('ascii').decode(bytes.slice(1, idx)) || 'image/jpeg';
  idx += 1; // null terminator after mime
  idx += 1; // picture type byte

  // Skip description
  if (encoding === 1 || encoding === 2) {
    while (idx + 1 < bytes.length && !(bytes[idx] === 0 && bytes[idx + 1] === 0)) {
      idx += 2;
    }
    idx += 2;
  } else {
    while (idx < bytes.length && bytes[idx] !== 0) idx++;
    idx += 1;
  }

  if (idx >= bytes.length) return undefined;
  const imgBytes = bytes.slice(idx);
  if (imgBytes.length < 16 || imgBytes.length > 2_500_000) return undefined;

  let binary = '';
  const chunkSize = 0x8000;
  for (let i = 0; i < imgBytes.length; i += chunkSize) {
    binary += String.fromCharCode(...imgBytes.subarray(i, i + chunkSize));
  }
  return `data:${mime};base64,${btoa(binary)}`;
}

async function extractId3Tags(
  blob: Blob
): Promise<{ title?: string; artist?: string; album?: string; coverDataUrl?: string }> {
  try {
    const headerBuf = await blob.slice(0, 10).arrayBuffer();
    const header = new Uint8Array(headerBuf);
    if (header[0] !== 0x49 || header[1] !== 0x44 || header[2] !== 0x33) {
      return {};
    }

    const majorVersion = header[3];
    const tagSize =
      ((header[6] & 0x7f) << 21) |
      ((header[7] & 0x7f) << 14) |
      ((header[8] & 0x7f) << 7) |
      (header[9] & 0x7f);

    const readLimit = Math.min(tagSize + 10, 2_500_000);
    const fullBuf = await blob.slice(0, readLimit).arrayBuffer();
    const data = new Uint8Array(fullBuf);

    let offset = 10;
    const result: { title?: string; artist?: string; album?: string; coverDataUrl?: string } = {};

    while (offset + 10 <= data.length) {
      const frameId = String.fromCharCode(
        data[offset],
        data[offset + 1],
        data[offset + 2],
        data[offset + 3]
      );
      if (!/^[A-Z0-9]{4}$/.test(frameId)) break;

      const frameSize =
        majorVersion >= 4
          ? ((data[offset + 4] & 0x7f) << 21) |
            ((data[offset + 5] & 0x7f) << 14) |
            ((data[offset + 6] & 0x7f) << 7) |
            (data[offset + 7] & 0x7f)
          : (data[offset + 4] << 24) |
            (data[offset + 5] << 16) |
            (data[offset + 6] << 8) |
            data[offset + 7];

      offset += 10;
      if (frameSize <= 0 || offset + frameSize > data.length) break;

      const payload = data.slice(offset, offset + frameSize);
      if (frameId === 'TIT2') result.title = decodeId3Text(payload);
      else if (frameId === 'TPE1') result.artist = decodeId3Text(payload);
      else if (frameId === 'TALB') result.album = decodeId3Text(payload);
      else if (frameId === 'APIC' && !result.coverDataUrl) {
        result.coverDataUrl = parseId3Picture(payload);
      }

      offset += frameSize;
    }

    return result;
  } catch {
    return {};
  }
}

function parseFilenameFallback(fileName: string): { title: string; artist: string } {
  const withoutExt = fileName.replace(/\.[^/.]+$/, '').trim();
  const parts = withoutExt.split(' - ');
  if (parts.length >= 2) {
    return {
      artist: parts[0].trim() || 'هنرمند ناشناس',
      title: parts.slice(1).join(' - ').trim() || withoutExt,
    };
  }
  return {
    title: withoutExt || 'قطعه بدون عنوان',
    artist: 'کتابخانه شخصی',
  };
}

function probeAudioDuration(blob: Blob): Promise<number> {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(blob);
    const audio = new Audio();
    const cleanup = () => {
      audio.src = '';
      URL.revokeObjectURL(url);
    };
    const timer = window.setTimeout(() => {
      cleanup();
      resolve(0);
    }, 3500);

    audio.preload = 'metadata';
    audio.onloadedmetadata = () => {
      window.clearTimeout(timer);
      const dur = Number.isFinite(audio.duration) ? audio.duration : 0;
      cleanup();
      resolve(dur);
    };
    audio.onerror = () => {
      window.clearTimeout(timer);
      cleanup();
      resolve(0);
    };
    audio.src = url;
  });
}

function guessMimeType(fileName: string, fallbackType?: string): string {
  if (fallbackType && fallbackType.startsWith('audio/')) return fallbackType;
  const ext = fileName.split('.').pop()?.toLowerCase() || '';
  const map: Record<string, string> = {
    mp3: 'audio/mpeg',
    wav: 'audio/wav',
    ogg: 'audio/ogg',
    m4a: 'audio/mp4',
    aac: 'audio/aac',
    flac: 'audio/flac',
    webm: 'audio/webm',
    opus: 'audio/opus',
  };
  return map[ext] || 'audio/mpeg';
}

export async function buildTrackFromBlob(
  fileName: string,
  blob: Blob,
  mimeType?: string,
  indexOffset = 0
): Promise<TrackRecord> {
  const id3 = await extractId3Tags(blob);
  const fallback = parseFilenameFallback(fileName);
  const duration = await probeAudioDuration(blob);

  const title = id3.title || fallback.title;
  const artist = id3.artist || fallback.artist;

  return {
    id: `trk_${Date.now()}_${indexOffset}_${Math.random().toString(36).slice(2, 8)}`,
    title,
    artist,
    album: id3.album || 'افزوده‌شده از دستگاه',
    duration,
    fileName,
    mimeType: guessMimeType(fileName, mimeType),
    size: blob.size,
    addedAt: Date.now() + indexOffset,
    coverDataUrl: id3.coverDataUrl,
    accentHue: hashHue(`${title}-${artist}`),
    audioBlob: blob,
  };
}

export async function importFilesToTracks(fileList: FileList | File[]): Promise<TrackRecord[]> {
  const files = Array.from(fileList);
  const imported: TrackRecord[] = [];
  let offset = 0;

  for (const file of files) {
    const lowerName = file.name.toLowerCase();
    const ext = lowerName.split('.').pop() || '';

    if (ext === 'zip' || file.type === 'application/zip') {
      try {
        const zip = await JSZip.loadAsync(await file.arrayBuffer());
        const entries: { name: string; obj: JSZip.JSZipObject }[] = [];
        zip.forEach((relPath, obj) => {
          if (obj.dir || relPath.startsWith('__MACOSX/')) return;
          const entryExt = relPath.split('.').pop()?.toLowerCase() || '';
          if (AUDIO_EXTENSIONS.has(entryExt)) {
            entries.push({ name: relPath.split('/').pop() || relPath, obj });
          }
        });
        for (const item of entries) {
          const uint8 = await item.obj.async('uint8array');
          const safeCopy = new Uint8Array(uint8);
          const blob = new Blob([safeCopy.buffer], { type: guessMimeType(item.name) });
          imported.push(await buildTrackFromBlob(item.name, blob, blob.type, offset++));
        }
      } catch {
        // Ignore invalid zip gracefully
      }
      continue;
    }

    if (file.type.startsWith('audio/') || AUDIO_EXTENSIONS.has(ext)) {
      imported.push(await buildTrackFromBlob(file.name, file, file.type, offset++));
    }
  }

  return imported;
}

function encodeAmbientWav(
  frequencies: number[],
  durationSec: number,
  pulseHz: number
): Blob {
  const sampleRate = 22050;
  const totalSamples = Math.floor(sampleRate * durationSec);
  const dataSize = totalSamples * 2;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);

  const writeStr = (pos: number, text: string) => {
    for (let i = 0; i < text.length; i++) {
      view.setUint8(pos + i, text.charCodeAt(i));
    }
  };

  writeStr(0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeStr(8, 'WAVE');
  writeStr(12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  writeStr(36, 'data');
  view.setUint32(40, dataSize, true);

  for (let i = 0; i < totalSamples; i++) {
    const t = i / sampleRate;
    const fadeIn = Math.min(1, t / 0.8);
    const fadeOut = Math.min(1, (durationSec - t) / 1.2);
    const envelope = fadeIn * fadeOut;
    const lfo = 0.65 + 0.35 * Math.sin(2 * Math.PI * pulseHz * t);

    let sample = 0;
    for (let f = 0; f < frequencies.length; f++) {
      const freq = frequencies[f];
      const weight = 1 / (f + 1.4);
      sample += Math.sin(2 * Math.PI * freq * t) * weight;
      sample += Math.sin(2 * Math.PI * (freq * 1.003) * t) * weight * 0.35;
    }

    const normalized = Math.max(-1, Math.min(1, (sample / frequencies.length) * envelope * lfo * 0.45));
    view.setInt16(44 + i * 2, Math.round(normalized * 32767), true);
  }

  return new Blob([buffer], { type: 'audio/wav' });
}

export function createStarterDemoTracks(): TrackRecord[] {
  const now = Date.now();
  const specs = [
    {
      id: 'demo_midnight_glass',
      title: 'شب شیشه‌ای',
      artist: 'آوای نیمه‌شب',
      album: 'مجموعه نمونه',
      duration: 18,
      freqs: [220, 277.18, 329.63, 440],
      pulse: 0.45,
      hue: 218,
    },
    {
      id: 'demo_rain_horizon',
      title: 'افق بارانی',
      artist: 'صدای آرامش',
      album: 'مجموعه نمونه',
      duration: 20,
      freqs: [196, 246.94, 293.66, 392],
      pulse: 0.35,
      hue: 198,
    },
    {
      id: 'demo_indigo_drift',
      title: 'موج نیلی',
      artist: 'نوای دیجیتال',
      album: 'مجموعه نمونه',
      duration: 16,
      freqs: [261.63, 329.63, 392, 493.88],
      pulse: 0.55,
      hue: 258,
    },
    {
      id: 'demo_dawn_chord',
      title: 'طلوع آکوستیک',
      artist: 'آوای صبح',
      album: 'مجموعه نمونه',
      duration: 22,
      freqs: [174.61, 220, 261.63, 349.23],
      pulse: 0.3,
      hue: 174,
    },
  ];

  return specs.map((item, idx) => {
    const wavBlob = encodeAmbientWav(item.freqs, item.duration, item.pulse);
    return {
      id: item.id,
      title: item.title,
      artist: item.artist,
      album: item.album,
      duration: item.duration,
      fileName: `${item.title}.wav`,
      mimeType: 'audio/wav',
      size: wavBlob.size,
      addedAt: now - idx * 1000,
      accentHue: item.hue,
      isDemo: true,
      audioBlob: wavBlob,
    };
  });
}
