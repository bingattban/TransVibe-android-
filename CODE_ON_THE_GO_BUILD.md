# TransVibe — Code On The Go build instructions (Phase 01, revised 19 Nov 2026)

**Use the in-app Build interface only. No terminal commands are needed.**

The former guide pinning AGP 8.7.3/Kotlin 2.0.21/Gradle 8.11.1 is superseded
because the user's current Code On The Go IDE runs Gradle 9.6.1 on ARM64.

Read the full Arabic UI-only instructions:

`phase01/COTG_UI_ONLY_AR.md`

Current declared tools: Gradle **9.6.1**, AGP **9.3.1**, Kotlin/Compose compiler
**2.3.21**, KSP **2.3.14** (published Maven Central; on-device availability
not proven), JDK **21** (observed on owner's phone), compileSdk **35**,
minSdk **26**, targetSdk **35**, ABI **arm64-v8a**.

With a network connection available for missing dependencies, open `TransVibe`
(the directory containing `settings.gradle.kts`), use the IDE Sync action if
available, select **Preview** if the variant selector supports it, then press
**Build**. If `--offline` is forced in the IDE's Additional Gradle flags,
disable it for the initial dependency download. If the interface only exposes
Debug, save the log; do not install Debug over the existing production app.

Production ID: `com.transvibe.app`.
Preview ID: `com.transvibe.app.preview` (separate application data).
Do not uninstall the production app to address a signing conflict.

**WARNING:** a successful Debug or Preview APK is only a build/install gate:
there is no verified Whisper/Sherpa native speech recognition in this source.
The Release native dependency check intentionally fails if the real runtimes
are absent. Do not label any diagnostic JNI stub as a working model.

Actual on-device build/unit-test/install/launch results: **NOT RUN** in the
source-preparation environment; these require owner's UI-generated logs.

See `gradle/wrapper/WRAPPER_PROVENANCE.md` for the authenticity and version
of the bootstrap JAR (not executed by Code On The Go's in-app Build button).
