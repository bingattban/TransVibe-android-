# TransVibe model acceptance gates

The project uses **one-model-at-a-time** validation; no second model is activated without the owner's explicit acceptance of the prior model's real-device run.

## Stage 1: Whisper ggml-tiny.bin

1. Validate the exact Android Kotlin/JNI signatures and sources.
2. In TransVibe-so-, run source contract tests and compile Android ARM64/Android API 26 native library on Linux GitHub-hosted runner.
3. Inspect ELF, exports, dependency closure, 16 KiB page alignment, SHA-256.
4. Transfer the verified library and manifest into an Android pull request. **Never directly alter main.**
5. Run Android Kotlin tests, Room migration tests, app build and packaging tests for the *same* commit, before handing off.
6. Export a Code On The Go-ready project. Build from **GUI Build** on a physical ARM64 phone and collect install, launch, actual ASR and log evidence.
7. Record timings, peak memory, decoded output accuracy, errors and owner acceptance before considering model 2.

Remaining model order (locked): ggml-base.bin; ggml-small.bin; ggml-medium.bin; ggml-large-v3-turbo.bin; moonshine-base-ar.zip; sensevoice-small-int8.zip.

Never bundle model weights in the source repository or pretend diagnostic/no-op JNI is real ASR.
No release/phase pass is implied by a GitHub source-level test or a native link test.
