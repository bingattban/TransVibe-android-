# TransVibe Android

**New public home:** https://github.com/bingattban/TransVibe-android-

The Kotlin/Jetpack Compose Android application (package `com.transvibe.app`) belongs here. Its only native builder is the new public [TransVibe-so-](https://github.com/bingattban/TransVibe-so-).

## Required workflow

- Always retain complete current source (Gradle 9.6.1, AGP 9.3.1, Kotlin 2.3.21, Room 2.8.5), Room migrations, app identity and user data.
- First model gate: `ggml-tiny.bin`. Do not move to a later model until tests, device build, real inference, quality review and the owner's explicit acceptance.
- Native builder compiles exact JNI source here and sends a SHA-256 validated binary as a **pull request**, never a direct main update.
- Run Kotlin/Room tests before building Debug and Preview APKs. The Android CI deliberately **fails** while required application files are absent; a skeleton cannot be called a successful release.
- Download the completed project ZIP from GitHub Actions, import in **Code On The Go** and build using its GUI. Run a video locally and inspect genuine logs.
- **No releases or merges without owner approval.** No model weights, secrets, keystores or private files in public GitHub.

Current status: repository bootstrap in progress. The full app source must be populated and the GitHub CI gates must pass; adding workflow files alone does not prove any Android build.
