# PHASE 01 — BUILD-SYSTEM REPAIR, COMPATIBILITY MATRIX AND FIRST CLEAN BUILD

- **Date:** 2026-11-19
- **Scope:** Only Phase 01, based on Phase 00 full-source archive. No GitHub / Notion writes, owner signer/keystore changes, APK publishing or Phase 02+ work.
- **Input source SHA-256:** See `PHASE01_INPUT_SOURCE_SHA256.txt` (Phase00 source ZIP, user original ZIP); output ZIP SHA-256 is reported separately after packaging.
- **Local worktree:** local extracted copy at `TransVibe`; not a Git branch, no Git metadata in supplied source ZIP.
- **Result:** **CODE CHANGES PREPARED; BUILD/DEVICE GATE BLOCKED — NOT COMPLETE**. No Android/Gradle task has compiled; real ASR inference remains unavailable.

## 1. Files examined and consumers

- `app/src/main/java/com/transvibe/app/TransVibeApplication.kt` and `subtitles/SrtVttExporter.kt`, `presentation/TransVibeViewModel.kt` (exporter wiring)
- `subtitles/HallucinationDetector.kt`, `translation/MlKitTranslationEngine.kt` (newline/regex encoding)
- `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `app/src/main/cpp/CMakeLists.txt`, `app/src/main/cpp/whisper_jni.cpp`, `scripts/fetch_native_deps.sh`
- `app/src/main/AndroidManifest.xml`, `presentation/screens/SettingsScreen.kt`, `persistence/UserPreferencesStore.kt`, `nativebridge/SherpaNativeBridge.kt`, `models/ModelLifecycleManager.kt`, `app/src/main/res/xml/*` (runtime/package/data state)
- Existing `app/src/test/java/...` unit tests, Phase 00 audit/test sources, installed local toolchain discovery

## 2. Evidence: TEST FIRST

`python3 -m unittest discover -s phase01/tests -p 'test_*.py' -v`

- **BEFORE FIX:** 2 passed, 5 failed, exit=1. Failures: undefined SRT exporter constructor; missing wrapper JAR/launchers; no release native assets check; diagnostic CMake fallback available to release; no visible diagnostic marker in Preview/Debug. Evidence: `phase01/logs/BEFORE_FIX.txt`.
- **AFTER FIX:** 10 passed, 0 failed, exit=0. Seven structural/source tests + two actual CMake configure gate tests + one 98-file source-change protection test. Evidence: `phase01/logs/AFTER_FIX.txt`.
- The passing Python/source and Linux CMake configure tests are **not** a Kotlin/Android SDK build, not a Sherpa JNI smoke test and not a native speech-recognition benchmark.

## 3. Changes and defect IDs

- `BUILD-001`: `AppContainer` calls `SrtVttExporter(app)` matching the import and real class instead of the missing `SrtVibeExporter`. Preserved original wrapper property and view-model consumer.
- `BUILD-002`: `CMakeLists.txt` allows the no-whisper diagnostic JNI *only* with explicit `TRANSVIBE_ALLOW_DIAGNOSTIC_JNI=ON` from Debug/Preview. Default and Release configuration without source fail with `message(FATAL_ERROR ...)`.
- `BUILD-004`: Registered Gradle `verifyReleaseNativeDependencies` task. Before release compile/native/packaging, require actual `whisper.cpp` source, exactly one Sherpa AAR with `classes.jar`, `libonnxruntime.so` arm64-v8a, and a Sherpa arm64 native `.so` entry. This is **structural packaging** verification, NOT proof of official version/provenance or successful inference. Native assets remain absent; this intentionally blocks production Release.
- `BUILD-003`: Added functional Linux/macOS and Windows bootstrap launchers and authentic Gradle wrapper JAR (SHA-256 verified against published 9.9.0 wrapper JAR). Pinned distribution remains Gradle 8.11.1 with official ZIP SHA-256 verification. **Known mismatch**: verified wrapper JAR 9.9.0 versus pinned Gradle 8.11.1 distribution; cross-version wrapper bootstrapping NOT TESTED. See `gradle/wrapper/WRAPPER_PROVENANCE.md`.
- `BUILD-005`: `SettingsScreen` visibly labels Debug/Preview as a diagnostic-enabled build. Kept `com.transvibe.app` for Debug/Release and `.preview` for Preview. `FileProvider` manifest authority uses `${applicationId}`, and DataStore is scoped to application Context (separate sandbox per package). No release-signer assumption.
- `BUILD-006`: Reviewed SRT/VTT newlines and the escaped whitespace regular expressions in `HallucinationDetector` and `MlKitTranslationEngine`; structural tests pass. No source edit necessary for those three files.
- `BUILD-007`: Documented Code On The Go steps and exact compatibility pins, rather than force an unverified Gradle/AGP update.

## 4. Gradle/SDK compatibility and Android manifest

- AGP **8.7.3** official minimum Gradle **8.9**, maximum API **35**, Java **17**, and SDK build tools **34.0.0**: https://developer.android.com/build/releases/agp-8-7-0-release-notes . Pinned Gradle **8.11.1** meets stated minimum on paper. Actual dependency resolution is **NOT RUN**.
- Kotlin **2.0.21**, Kotlin Compose plugin **2.0.21**, KSP **2.0.21-1.0.28**, Room **2.6.1**, `compileSdk=35`, `minSdk=26`, `targetSdk=35`, JDK source/bytecode 17 unchanged. Host JDK 21.0.12.1; actual COTG JDK/SDK/NDK/CMake **unverified**.
- Native target only `arm64-v8a`. NDK flexible page-size argument is declared but shipped ELF 16 KiB alignment **NOT RUN**.
- Manifest declares INTERNET, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PROCESSING, POST_NOTIFICATIONS, WAKE_LOCK, FileProvider authority `${applicationId}.fileprovider`, MainActivity exported, processing service not exported. Physical-device permission/startForeground correctness on Android 26-35 **NOT RUN**. Known Android-version guard in `TransVibeProcessingService.kt` should be verified in Phase 15; no service semantics changed here.
- Preview app ID isolated at the Android OS sandbox level. Same package Debug may attempt replacing installed production; real signer lineage and previous database migration remain **BLOCKED** (Phase 03). Never uninstall to fix signer mismatch.
- `gradle/wrapper` JAR came from read-only legacy local ZIP; no fake JAR. Exact official checksum and provenance are recorded; no third-party native runtime added or claimed verified.

## 5. Exact commands, results and build blocker

```sh
python3 -m unittest discover -s phase01/tests -p 'test_*.py' -v
# PASS: 10 tests, exit 0; actual CMake configure tests included
sh -n gradlew
# PASS
python3 -m py_compile phase01/tests/test_build_contracts.py
# PASS
cmake -S app/src/main/cpp -B /tmp/transvibe_phase01_cmake_release -DTRANSVIBE_ALLOW_DIAGNOSTIC_JNI=OFF
# FAIL, as required: RELEASE BLOCKED: third_party/whisper.cpp missing
cmake -S app/src/main/cpp -B /tmp/transvibe_phase01_cmake_debug -DTRANSVIBE_ALLOW_DIAGNOSTIC_JNI=ON
# PASS for *configuration* with explicit diagnostic warning; native Android compile NOT RUN
./gradlew --version
# EXIT 1: java.net.UnknownHostException: services.gradle.org
```

Clean Gradle attempts, each with isolated temporary `GRADLE_USER_HOME` and flags `--no-daemon --no-build-cache --rerun-tasks`:

| Attempted Gradle task | Outcome |
| --- | --- |
| `:app:assembleDebug` | **BLOCKED** before Gradle distribution download — `UnknownHostException: services.gradle.org` |
| `:app:assemblePreview` | **BLOCKED** same |
| `:app:testDebugUnitTest` | **BLOCKED** same |
| `:app:assembleRelease` | **BLOCKED** same, Gradle release gate was NOT executed |

No Android SDK/ADB/physical ARM64 device is connected to this worker. Build tasks were genuinely attempted but were not executed, so there is NO APK, no Kotlin unit-test result and no install/launch evidence. No clean build can be claimed.

## 6. Scope/compatibility, test evidence, missing assets and owner decisions

- **Changed production files:** `app/src/main/java/com/transvibe/app/TransVibeApplication.kt`, `app/build.gradle.kts`, `app/src/main/cpp/CMakeLists.txt`, `app/src/main/java/com/transvibe/app/presentation/screens/SettingsScreen.kt`, `gradle/wrapper/gradle-wrapper.properties`, `CODE_ON_THE_GO_BUILD.md`.
- **Added support files:** `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/WRAPPER_PROVENANCE.md`, `phase01/tests/test_build_contracts.py`, `phase01/PHASE01_BUILD_COMPATIBILITY_MATRIX.csv`, `phase01/PHASE01_REPORT.md`, `phase01/PHASE01_CODE_ON_THE_GO.md` (if present), `phase01/PHASE01_CHANGED_FILES_INSTALLATION.md`, `phase01/PHASE01_INPUT_SOURCE_SHA256.txt` and `phase01/logs/*.txt`.
- **Deleted:** NONE. Room database, persistence schemas, signed versionCode, model filenames, model assets and existing Phase00 audit files remain untouched.
- **Missing real inputs:** `app/src/main/cpp/third_party/whisper.cpp/`, a verified single Sherpa-ONNX `app/libs/sherpa-onnx-<version>.aar`, auxiliary Silero VAD model, Seven ASR weights (not to be bundled; installed later by user), original owner's compatible release signing key, Android SDK/NDK/CMake on actual phone, Android ARM64 device.
- **Native provenance:** No Whisper or Sherpa native runtime shipped, no fake inference proof. Upstream pin/LICENSE/SHA-256/ELF ABI still require Phase 04; current checking is structural.
- **Performance/quality:** NOT MEASURED. No WER/CER, runtime processing time, battery/memory, on-device ASR or export claims.
- **Gate:** `CODE CHANGES PREPARED / BUILD-AND-DEVICE-VALIDATION BLOCKED`. **NEXT PHASE ELIGIBILITY: NO** until task builds, tests and ARM64 installation/launch are demonstrated, or owner explicitly revises gate.
- **Owner decision / action needed:** Validate COTG installed Gradle/SDK/JDK/NDK versions, provide actual `assemblePreview` / `testDebugUnitTest` logs, test Preview APK on ARM64 and record install/launch; before production upgrade verify signing certificate lineage and Phase03 migration. Do not move to Phase02 automatically.

## 7. Deliverables

1. Full source ZIP with original Phase00 files plus Phase01 fixes and audit artifacts.
2. Changed-files-only ZIP, preserving `TransVibe/` relative structure; installation instructions in `phase01/PHASE01_CHANGED_FILES_INSTALLATION.md`.
3. `phase01/PHASE01_INPUT_SOURCE_SHA256.txt`: input ZIP hashes; output ZIP hashes are generated separately after packaging (no circular self-hash).

**This report is a source/guard audit, not evidence that the complete Android application runs.**
