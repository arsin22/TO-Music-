export enum RepeatMode {
  Off = 'off',
  All = 'all',
  One = 'one',
}

export enum SortMode {
  DateDesc = 'date_desc',
  TitleAsc = 'title_asc',
  ArtistAsc = 'artist_asc',
  DurationDesc = 'duration_desc',
}

export enum EqPreset {
  Flat = 'flat',
  BassBoost = 'bass_boost',
  Pop = 'pop',
  Rock = 'rock',
  Vocal = 'vocal',
  Acoustic = 'acoustic',
  Custom = 'custom',
}

export enum LibraryTab {
  All = 'all',
  Favorites = 'favorites',
  Albums = 'albums',
  Artists = 'artists',
  Recent = 'recent',
}

export enum RingtoneTarget {
  Ringtone = 'ringtone',
  Alarm = 'alarm',
  Notification = 'notification',
}

export interface TrackMeta {
  id: string;
  title: string;
  artist: string;
  album: string;
  duration: number;
  fileName: string;
  mimeType: string;
  size: number;
  addedAt: number;
  coverDataUrl?: string;
  accentHue: number;
  isDemo?: boolean;
  isFavorite?: boolean;
  lastPlayedAt?: number;
  playCount?: number;
  plainLyrics?: string;
  syncedLyrics?: string;
}

export interface TrackRecord extends TrackMeta {
  audioBlob: Blob;
}
