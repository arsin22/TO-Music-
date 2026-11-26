# TO Music (آواپلیر) — Android

Native Android music player rewritten in **Kotlin** and **Jetpack Compose** with Material 3, Persian RTL dark-glassmorphic styling, and Media3 background playback.

## Features
- **Media3 ExoPlayer & Background Service**: Full lock-screen / notification media controls (`MediaSessionService`), shuffle, repeat modes, queue management, and sleep timer.
- **Local Music Library (Room + SAF)**: Import audio files or `.zip` archives with automatic ID3 metadata & cover art extraction, plus starter ambient tracks.
- **60fps Synced Lyrics**: Supports standard (`[mm:ss.xx]`) and enhanced word-level (`<mm:ss.xx>`) LRC timestamps, 60fps `withFrameNanos` word illumination scaled by playback speed, fine timing offset (`-5s` to `+5s`), online `lrclib.net` lookup, and manual LRC editor.
- **5-Band Equalizer & Playback Speed**: Hardware-backed Android `Equalizer` audio effect with presets and adjustable playback speed (`0.75x`–`1.5x`).
- **Ringtone Maker**: Visual waveform range selector with fade-in/fade-out WAV export saved to device `Ringtones` via `MediaStore` or shared via `FileProvider`.
- **Live Audio Visualizer**: Real-time reactive spectrum bar visualizer and adaptive layouts for phones and tablets.
