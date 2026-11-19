# TransVibe — Phase 01 follow-up: Code On The Go GUI build compatibility

Date: 2026-11-19. Scope: **Phase 01 ONLY**. Phase 02 NOT STARTED.

## Actual device evidence, supplied by owner

- IDE launched Gradle **9.6.1** daemon with **Java 21.0.10 (aarch64)**.
- The build evaluated root `TransVibe` and selected AGP **8.7.3**, Kotlin Android/Compose/serialization **2.0.21**, and KSP **2.0.21-1.0.28** from the previous project.
- Gradle failed during **root project classpath configuration**, before compiling any Kotlin/Java/C++ application code:
  `Could not find error_prone_annotations-2.18.0.jar (com.google.errorprone:error_prone_annotations:2.18.0)`.
- The only path listed for that artifact was an IDE-private `maven/localMvnRepository/` mirror. A missing JAR at this mirror is the demonstrated immediate blocker. Do **not** label it an ASR or Kotlin compilation failure.
- The Gradle daemon warning `Unable to set daemon's environment variables ... no native integration` was **not** the fatal exception shown in the provided log.
- `com.google.errorprone:error_prone_annotations:2.18.0` is a real Maven Central artifact, licensed Apache 2.0: https://central.sonatype.com/artifact/com.google.errorprone/error_prone_annotations/2.18.0

## Source-based repair attempted (not yet device-validated)

Code On The Go's official migration guide documents the September 2026 toolchain shift to Gradle **9.6.1**, AGP **9.3.1**, Kotlin **2.3.21**, and removal of Android's legacy Kotlin plugin in favor of AGP 9 built-in Kotlin. Source: https://github.com/appdevforall/CodeOnTheGo/wiki/IMPORTANT:-Fix-project-breaking-changes-after-the-Gradle-AGP-Kotlin-toolchain-upgrade

Updated:

1. `gradle/libs.versions.toml`: AGP 9.3.1, Kotlin 2.3.21, published KSP 2.3.14; introduced the Android library plugin alias for shared root plugin classpath. KSP 2.3.14 publication checked from Maven Central listings, **availability in device's local mirror NOT VERIFIED**.
2. `build.gradle.kts`: added Kotlin 2.3.21 root `buildscript` classpath for IDE-injected plugin, declared Android app/library and Compose/serialization/KSP plugin aliases. Removed legacy Kotlin Android root plugin.
3. `app/build.gradle.kts`: removed Kotlin Android plugin, replaced obsolete `android.kotlinOptions` with `kotlin { compilerOptions { jvmTarget.set(...JVM_17) } }`. Preserved all dependencies, app identity, Preview suffix, CMake diagnostics, 64-bit ABI, and release native-asset fail-closed gate.
4. `gradle/wrapper/gradle-wrapper.properties`: Gradle 9.6.1 distribution with official SHA-256 (`9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14`). **The existing authentic Gradle 9.7.0 wrapper JAR remains and matching-jar reproducibility is NOT YET VERIFIED**; the phone's GUI Build uses embedded Gradle 9.6.1 and does not run `gradlew`.
5. User-facing documentation now makes the **Code On The Go IDE GUI Build** the sole prescribed build method; no terminal instructions are required. Older guide marked historical. No APK/signing changes, no model/library installations, no data mutations.

This repair is a **compatibility hypothesis grounded in the IDE's published migration policy**; it is not proof the specific missing-JAR error or other transitive dependencies are resolved on the device. A new GUI Build log is needed.

## TEST FIRST evidence

- Before the source changes, `python3 -m unittest discover -s phase01/tests -p test_cogo_ui_build_compat.py -v` **FAILED** as expected (old toolchain pins, Android Kotlin plugin, mismatched Wrapper and missing UI guide). Full output: `phase01/PHASE01_COGO_UI_TEST_BEFORE.log`.
- After the edits, `python3 -m unittest discover -s phase01/tests -p 'test_*.py' -v` **PASSED: 19 tests** (source-level contracts and CMake configure gates). Log: `phase01/PHASE01_COGO_UI_TEST_AFTER.log`.
- Python bytecode compilation of the phase01 Python tests: **PASS**.
- TOML parsing of version catalog with Python 3.13 `tomllib`: **PASS**.
- Original source inventory protection: **PASS** for allowed Phase01-only edited files. No Android source/persistence/model JS/Kotlin business logic touched in this follow-up.
- `./gradlew --version` attempted here; **FAILED before any Gradle work** with `java.net.UnknownHostException: services.gradle.org`; see `phase01/PHASE01_COGO_UI_BOOTSTRAP_ATTEMPT.log`.
- `:app:assembleDebug`, `:app:assemblePreview`, `:app:testDebugUnitTest`, clean no-cache build: **NOT RUN** after source edit due missing local Android SDK/Gradle 9.6.1 and blocked network.
- Installation and launch of Preview APK on physical ARM64 device: **NOT RUN**. No APK generated.
- Real Whisper/Sherpa recognition, translation, performance, memory, WER/CER: **NOT RUN/NOT MEASURED**; real native inputs remain unavailable.

## Security and data-integrity boundaries

- Production ID **com.transvibe.app**, Preview **com.transvibe.app.preview** preserved.
- Room DB version/files, settings, in-place upgrade behavior, keystore and user models unchanged.
- Release remains **blocked intentionally** without verified Whisper source and Sherpa ARM64 AAR. Diagnostic JNI is not actual ASR.
- No GitHub/Notion uploads, commits, merges, remote write, publication, or signing key changes.

## Exact owner UI-only procedure

1. Unpack the delivered complete source archive into a **new folder**; open its `TransVibe` root (with `settings.gradle.kts`) in Code On The Go.
2. In **Preferences → Build & Run → Additional Gradle flags**, remove an enforced `--offline` option if present so dependencies may download. No terminal use.
3. Use IDE **Sync** if available. Select **Preview** build variant if supported and tap **Build**. If only Debug is offered, build and send logs but do not replace the production installation.
4. If Build succeeds, install **Preview APK only**, open app, and send a confirmation or crash log. If Build fails, send the new error, especially `FAILURE`, `What went wrong`, `Could not resolve` and `Caused by` sections.

## Gate verdict

**PHASE 01 STILL BLOCKED / DEVICE BUILD-VALIDATION PENDING.** The owner's device log established a new root classpath dependency-resolution defect. Source fix and 19 static/CMake tests are complete, but the actual GUI Gradle build and ARM64 installation gate are unverified. **DO NOT START PHASE 02** until owner supplies successful acceptance evidence or a further failing log for another Phase01 repair cycle.
