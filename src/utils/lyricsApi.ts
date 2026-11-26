export interface LyricSearchResult {
  id: number;
  trackName: string;
  artistName: string;
  albumName?: string;
  duration?: number;
  plainLyrics?: string;
  syncedLyrics?: string;
}

export interface SyncedLyricLine {
  timeSec: number;
  text: string;
}

export function parseLrcLines(lrcText: string): SyncedLyricLine[] {
  const lines = lrcText.split(/\r?\n/);
  const parsed: SyncedLyricLine[] = [];
  const timeRegex = /\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\]/g;

  for (const rawLine of lines) {
    const matches = [...rawLine.matchAll(timeRegex)];
    if (matches.length === 0) continue;

    const cleanText = rawLine.replace(timeRegex, '').trim();
    if (!cleanText) continue;

    for (const m of matches) {
      const mins = Number(m[1]);
      const secs = Number(m[2]);
      const fracRaw = m[3] || '0';
      const frac = Number(fracRaw) / Math.pow(10, fracRaw.length);
      const timeSec = mins * 60 + secs + frac;
      if (Number.isFinite(timeSec)) {
        parsed.push({ timeSec, text: cleanText });
      }
    }
  }

  parsed.sort((a, b) => a.timeSec - b.timeSec);
  return parsed;
}

export async function searchOnlineLyrics(
  queryTitle: string,
  queryArtist?: string,
  signal?: AbortSignal
): Promise<LyricSearchResult[]> {
  const cleanTitle = queryTitle.trim();
  const cleanArtist = queryArtist?.trim() || '';

  const params = new URLSearchParams();
  if (cleanTitle && cleanArtist && cleanArtist !== 'کتابخانه شخصی' && cleanArtist !== 'هنرمند ناشناس') {
    params.set('track_name', cleanTitle);
    params.set('artist_name', cleanArtist);
  } else {
    params.set('q', `${cleanTitle} ${cleanArtist}`.trim());
  }

  const response = await fetch(`https://lrclib.net/api/search?${params.toString()}`, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
    signal,
  });

  if (!response.ok) {
    throw new Error('پاسخی از سرویس آنلاین متن آهنگ دریافت نشد.');
  }

  const data = (await response.json()) as LyricSearchResult[];
  if (!Array.isArray(data)) return [];

  // If strict track+artist returned nothing, fallback to broader query `q`
  if (data.length === 0 && params.has('track_name')) {
    const fallbackParams = new URLSearchParams({
      q: `${cleanTitle} ${cleanArtist}`.trim(),
    });
    const fallbackRes = await fetch(
      `https://lrclib.net/api/search?${fallbackParams.toString()}`,
      {
        method: 'GET',
        headers: { Accept: 'application/json' },
        signal,
      }
    );
    if (fallbackRes.ok) {
      const fallbackData = (await fallbackRes.json()) as LyricSearchResult[];
      return Array.isArray(fallbackData)
        ? fallbackData.filter((item) => Boolean(item.syncedLyrics || item.plainLyrics))
        : [];
    }
  }

  return data.filter((item) => Boolean(item.syncedLyrics || item.plainLyrics));
}
