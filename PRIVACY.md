# Privacy & Security

- 100% offline transcription, translation (after pack download), and SRT/VTT export.
- No user accounts, no cloud media uploads, `usesCleartextTraffic="false"`.
- SAF URI grants and `FileProvider` only.
- ZIP imports validate against path traversal (`../`), duplicate entries, entry count limits, and compression-ratio bombs.
- Diagnostic exports redact raw audio, transcripts, and local filesystem paths.
