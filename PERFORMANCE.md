# Performance & Benchmarking Policy

- Audio decoding uses audio-only `MediaExtractor` + `MediaCodec` streaming into a bounded `Channel(capacity = 2)` (8 MiB PCM cap).
- Whisper Tiny includes a Debug/Preview-only interleaved thread study (`2, 3, 4` threads) computing median and p90 inference times plus PSS memory.
- No unmeasured speedups or unverified RTF targets are claimed; all baselines must be measured on the target ARM64 device before owner freeze approval.
