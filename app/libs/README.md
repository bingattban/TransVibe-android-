# Verified native import directory

Files from the **TransVibe-so-** repository enter this directory only after a GitHub Actions build and verified Pull Request.

Expected layout:
- `app/libs/arm64-v8a/libtransvibe_whisper.so` (Android ARM64, API 26, real JNI).
- Optional `app/libs/*.aar` for independently validated Sherpa-ONNX Android packages.
- `app/libs/native-provenance.json`: builder run id, Android source commit, JNI source SHA-256, binary SHA-256, upstream whisper commit.

Never place model weights, secrets or phone logs in this directory. Duplicate SO copies from AAR are rejected. CI must fail closed on absent/incompatible native artifacts for production release.
