# Whisper Tiny — translation stall and recovery (2026-11-20)

The owner's device showed `TRANSLATING`, 41/41 seconds and a frozen elapsed 9 seconds. Supplied log confirms native Whisper context initialized and AAC input decoded; it does not expose a translation exception or prove ASR quality. No private paths, transcripts or model data are included in this report.

## Corrective behavior

- Do not download ML Kit language models implicitly during processing. Require already installed offline language packs and emit actionable `TRANSLATION_PACK_MISSING`.
- Put a 10-second bound on model inventory checks and a 60-second bound on each translation attempt. Surface explicit timeout/failure.
- Propagate cancellation, preserve transcript segments already stored in Room, and allow opening the original transcript for later translation.
- Display cue-by-cue translation stage progress and a running elapsed clock; never leave UI falsely reporting `TRANSLATING` after a failure.
- Add failures to a sanitized device log instead of silently ignoring `Result.failure`.

No Room schema or application identity changes. The new native `.so` does not need rebuilding: this is an Android translation pipeline fix.

## Required gates

Python regression tests + Android JVM tests + verified JNI compatibility, then owner-signed Preview and Release APKs and source ZIP. Green CI is not physical-device inference validation. Do not begin model 2 until Whisper Tiny is accepted on the phone.
