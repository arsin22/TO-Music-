function encodeAudioBufferToWav(
  audioBuffer: AudioBuffer,
  startSec: number,
  endSec: number,
  fadeEdges: boolean
): Blob {
  const sampleRate = audioBuffer.sampleRate;
  const numChannels = Math.min(2, audioBuffer.numberOfChannels);

  const clampedStart = Math.max(0, Math.min(startSec, audioBuffer.duration));
  const clampedEnd = Math.max(clampedStart + 0.5, Math.min(endSec, audioBuffer.duration));

  const startSample = Math.floor(clampedStart * sampleRate);
  const endSample = Math.floor(clampedEnd * sampleRate);
  const frameCount = Math.max(1, endSample - startSample);

  const bytesPerSample = 2;
  const blockAlign = numChannels * bytesPerSample;
  const dataSize = frameCount * blockAlign;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);

  const writeStr = (pos: number, str: string) => {
    for (let i = 0; i < str.length; i++) {
      view.setUint8(pos + i, str.charCodeAt(i));
    }
  };

  writeStr(0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeStr(8, 'WAVE');
  writeStr(12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, numChannels, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * blockAlign, true);
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, 16, true);
  writeStr(36, 'data');
  view.setUint32(40, dataSize, true);

  const channels: Float32Array[] = [];
  for (let ch = 0; ch < numChannels; ch++) {
    channels.push(audioBuffer.getChannelData(ch));
  }

  const fadeSamples = fadeEdges ? Math.min(Math.floor(sampleRate * 0.6), Math.floor(frameCount * 0.15)) : 0;

  let offset = 44;
  for (let i = 0; i < frameCount; i++) {
    let gain = 1;
    if (fadeSamples > 0) {
      if (i < fadeSamples) {
        gain = i / fadeSamples;
      } else if (i > frameCount - fadeSamples) {
        gain = (frameCount - i) / fadeSamples;
      }
    }

    for (let ch = 0; ch < numChannels; ch++) {
      const raw = channels[ch][startSample + i] ?? 0;
      const sample = Math.max(-1, Math.min(1, raw * gain));
      view.setInt16(offset, Math.round(sample * 32767), true);
      offset += 2;
    }
  }

  return new Blob([buffer], { type: 'audio/wav' });
}

export async function trimAudioBlobToWav(
  sourceBlob: Blob,
  startSec: number,
  endSec: number,
  fadeEdges = true
): Promise<{ blob: Blob; totalDuration: number }> {
  const AudioCtx =
    window.AudioContext ||
    (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;

  const ctx = new AudioCtx();
  try {
    const arrayBuffer = await sourceBlob.arrayBuffer();
    const decoded = await ctx.decodeAudioData(arrayBuffer);
    const wavBlob = encodeAudioBufferToWav(decoded, startSec, endSec, fadeEdges);
    return {
      blob: wavBlob,
      totalDuration: decoded.duration,
    };
  } finally {
    await ctx.close().catch(() => undefined);
  }
}

export function triggerFileDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.setTimeout(() => URL.revokeObjectURL(url), 1500);
}
