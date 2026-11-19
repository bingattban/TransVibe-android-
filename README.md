# TransVibe Android

Public Android app: https://github.com/bingattban/TransVibe-android-
Native builder: https://github.com/bingattban/TransVibe-so-

## Build and merge contract (2026-11-19)

- Keep the Kotlin/Compose project, Room database and migrations, package `com.transvibe.app`, and Gradle 9.6.1/AGP 9.3.1/Kotlin 2.3.21.
- Test and validate **one model at a time**. The first model is `ggml-tiny.bin`. Later models remain unapproved until device logs and owner's model acceptance.
- Native ARM64 `.so` files are built in the new SO repository. Verified native binary + provenance enter this Android repository through a PR, not a direct main push.
- Source regression tests and Android JVM tests must succeed BEFORE creating distributable APKs. Real ELF64 AArch64 JNI symbol/source hash verification must also pass.
- After every required gate succeeds, create **exactly two signed APK variants**: `Preview` (`com.transvibe.app.preview`) and `Release` (`com.transvibe.app`). Both use the SAME owner's keystore, certificate and key alias.
- Fail closed if any test, native contract, signing input, APK identity, or signing certificate check fails. Only then may GitHub Actions squash-merge an internal ready PR into `main`, without bypassing branch protection. After merging, workflow_dispatch rebuilds against the merged main SHA.
- The GitHub Actions artifact also contains `TransVibe-CodeOnTheGo-source.zip`, with verified native libraries and source, for GUI Build on the owner's ARM64 Android phone. This ZIP does NOT include private signing keys; to sign a local Release/Preview on the phone, configure the same private keystore separately on the device.
- APK signing/inference quality are separate gates: creating a signed Release variant for the **first model** is not final acceptance of all seven ASR models.
- Do not commit signing secrets, release.properties, model weights, or generated caches.

**Merge automation is conditional, not unconditional.** It remains blocked while native source/provenance, CI tests or the required signing configuration are missing.
