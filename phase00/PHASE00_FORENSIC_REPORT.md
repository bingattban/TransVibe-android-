# PHASE 00 — Forensic Baseline, Comparative Audit and Change Protection

**Date:** 2026-11-19  • **Phase ID:** `00`  • **Source of truth:** EXACT user-supplied `TransVibe_Android_Project.zip`, not a GitHub HEAD or a prior phase ZIP.  • **Result:** `AUDIT ARTIFACTS COMPLETE / NEXT-PHASE GATE BLOCKED`.

## 1. Input and change-protection contract

- **Source archive SHA-256:** `928362b02683aeb86f6eadc53cac8c2212f85ff2b3d6a275723363800943d880` (110,401 bytes).
- 98 unique original project files, **ZIP CRC and path safety PASS**, no duplicate paths, traversal paths or symlinks.
- Current source extracted to an independent working copy (`/mnt/data/transvibe_phase00_work/TransVibe/`); this is NOT a Git worktree. The original ZIP includes no `.git`, branch name, commit SHA or remote metadata; no source revision or branch identity is verifiable from it.
- Original files are byte-for-byte unchanged; verification reads each original ZIP member and compares its SHA-256 with extracted copy.
- No project source changes, no Gradle/native downloads, no Notion/GitHub/remote action, no installed app modification; only `phase00/` reports/matrices and `scripts/phase00_forensic_contracts.py` were created.
- Prior FOUNDATION documents are NOT in the exact original archive and were NOT substituted as baseline.

## 2. Exhaustive inventory of the exact ZIP

| Category | Original files |
|---|---:|
| Android XML resources | 6 |
| Android manifest | 1 |
| C/C++ native bridge sources | 1 |
| Documentation/metadata/licenses | 11 |
| Gradle/configuration | 6 |
| Helper scripts | 1 |
| Kotlin JVM unit tests | 7 |
| Kotlin production sources | 64 |
| Other | 1 |
| **TOTAL** | **98** |

Complete SHA-256, file sizes, ZIP CRC and path classification for **every original member** are in `phase00/PHASE00_SOURCE_INVENTORY_SHA256.csv`. ZIP contains Kotlin source, C++ JNI source, Gradle configuration, XML resources, test source and Markdown docs; no compiled APK, `.so`, `.aar`, ggml weights, ONNX VAD asset or translation language-pack binaries. Original unit tests: 7 Kotlin `app/src/test/...` files, not executed here. 14 screen/dialog files.

**Core files verified:**
- `app/src/main/java/com/transvibe/app/TransVibeApplication.kt:19-60` — dependencies/wiring; `SrtVibeExporter(app)` undefined at line 30; first-launch pack initialization at 58-59.
- `app/src/main/java/com/transvibe/app/persistence/TransVibeDatabase.kt:10-20,28-68` — actual **Room version 3**, DB `transvibe.db`, only 1->2 and 2->3 migrations.
- `app/src/main/java/com/transvibe/app/models/ModelCatalog.kt:11-143` — EXACT seven physical model files, independent IDs/declared language sets and unpinned generic upstream references.
- `app/src/main/java/com/transvibe/app/asr/ModelProfiles.kt:11-145` — EXACT seven model profiles. Tiny `UNDER_DEVELOPMENT`; others `NOT_STARTED`. VAD/chunks/threads are hypotheses, not optimized/frozen settings.
- `app/build.gradle.kts:9-60` — `com.transvibe.app`, minSdk26, targetSdk35, compileSdk35, versionCode6/2.0.0, arm64-v8a, distinct Preview suffix. `gradle/libs.versions.toml:1-16` — AGP8.7.3, Kotlin2.0.21, KSP2.0.21-1.0.28, Room2.6.1, ML Kit Translate17.0.3; **dependency resolution/build NOT RUN**.
- `app/src/main/cpp/CMakeLists.txt:10-31` — diagnostic JNI library can build if real whisper.cpp source absent. No release-mode guard present.
- `app/src/main/AndroidManifest.xml:5-51` — internet/download, foreground service, notification, wake lock, launcher, FileProvider; actual permission/device behavior untested.

**Known documentation drift:** `MIGRATION.md:5` says Room v2; source is v3. `PHASE_REPORT.md:6-18` asserts old fixes but says tests/native dependencies pending. Read actual source, not those statements, as implementation evidence.

## 3. Comparison E01–E18

Old claim map: `PHASE_REPORT.md:6-13 (old claim grouping)`. Each ID was classified individually against the current source, with file/line evidence and regression links in `phase00/PHASE00_E01_E18_STATUS.csv`.

Current classifications (scope precisely limited to source evidence): **confirmed fixed: 3, partially fixed: 13, still present: 2**. A label `confirmed fixed (source syntax only)` means the exact prior syntax escape defect is absent **in source text**; it does NOT mean Gradle compilation or Android runtime passed.

**Important outstanding observations:**
- `BUILD-001`: `AppContainer` references undefined `SrtVibeExporter` at `TransVibeApplication.kt:30`; actual class `SrtVttExporter` at `subtitles/SrtVttExporter.kt:14`. Static reproducer FAIL (correctly catches defect). First repair belongs **Phase 01 only**.
- `TRANS-001/E04`: Arabic original is bypassed (`translation/TranslationRoutingPolicy.kt:41-43`), which contradicts owner's literal non-English -> English -> Arabic rule. No change in Phase 00.
- `E09/RESOURCE-001`: `SileroVadEngine.kt:30-35,45-82` switches to RMS after reflection exception; no genuine Silero asset/runtime bundled. RMS is NOT proof of Silero VAD.
- `E13/NATIVE-001`: Sherpa branch at `ModelLifecycleManager.kt:339-344` succeeds on AAR presence alone; no actual model init or transcription smoke test.
- `E11`: `MediaInspector.kt:97-115` fingerprints metadata and first 64 KiB, not full media; if stream cannot reopen it uses metadata-only fallback. Need collision/relink tests before safe resume.
- `E10`: `TransVibeProcessingService.kt:58` uses Android 14/API34 check for `mediaProcessing` startForeground type; introduced API35, so verify runtime/manifest correctness in Phase15.
- `E16`: `AudioBufferPool.kt` present and byte budget in `ProcessingOrchestrator.kt:295-319`; real memory profile unmeasured.

## 4. Real legacy-source comparison: high-priority data-loss risk

Compared a **separate read-only** legacy v1.0.4 source archive supplied previously: `TransVibe_v1.0.4_Whisper_Tiny_Phase1_CodeOnTheGo_FULL_LOCAL.zip`, SHA-256 `9974afa300747c5664cb2b20452c7d0cf9dd9c6d9a0b7824822cb5273598b493`. Concrete legacy files/line numbers and current differences are documented in `phase00/PHASE00_LEGACY_EVIDENCE.md`. No legacy source was copied into the delivered app.

**CRITICAL MIGRATION BLOCKER:** Legacy `processing_jobs.db` is Room schema **v5** with `processing_jobs`/`subtitle_segments`; current `transvibe.db` is **v3** with different table names. Current 1->2->3 migrations do NOT import the legacy file or migrate schema5. Legacy DataStore `transvibe_preferences` differs from current `transvibe_prefs`, legacy models in `filesDir/models/whisper/`, `models/moonshine/...`, `models/sensevoice/...` differ from new `filesDir/models/<modelId>/`. A saved SAF tree URI is stored under `subtitle_output_tree_uri` in old preferences and not migrated. Old source has owner signing configuration via environment variables; actual installed signing certificate remains UNVERIFIED. A matching `applicationId` and higher versionCode do not prove safe update.

**Do not uninstall, force an upgrade, rename database files or overwrite old storage.** No migration was performed in Phase 00. Read-only actual-device/schema/signature evidence and approved migration strategy are required for Phase 03.

## 5. Assets and provenance

- ASR weights: **0/7 shipped**, by design for user-download/import AFTER APK install. All 7 model IDs/filenames exist in catalog, but no real load/format/accuracy validation was possible.
- Native whisper.cpp source: **NOT INCLUDED** at `app/src/main/cpp/third_party/whisper.cpp/`. `scripts/fetch_native_deps.sh:12-14` proposes upstream tag `v1.7.4` but does NOT establish a resolved commit checksum in the supplied ZIP.
- Sherpa-ONNX Android AAR: **NOT INCLUDED** in `app/libs/`. A generic release page URL is documented but no selected AAR version/asset SHA/ABI check is recorded.
- Silero model: **NOT INCLUDED** at `app/src/main/assets/vad/silero_vad.onnx` or `filesDir/aux_models/vad/silero_vad.onnx` (the latter requires runtime inspection); script references a proposed URL without a verified checksum. No network fetch performed.
- ML Kit language packs: NOT APK files; Android/ML Kit-managed downloads, not inspectable in source archive. `TranslationPackManager.kt:80-108` queries installed language models dynamically. Runtime readiness **UNVERIFIED**.
- `ModelCatalog.kt` exposes generic Whisper source `https://huggingface.co/ggerganov/whisper.cpp` and Sherpa source `https://github.com/k2-fsa/sherpa-onnx`; it has **no pinned file-specific SHA-256 or revision** for any seven artifacts, and model ZIP conventions are not verified against an AAR. No third-party binary was imported in Phase00. Actual authentic artifact license/provenance still requires Phase04/05 validation.
- Upstream reference pages inspected 2026-11-19 (metadata only; no downloading/repackaging): `https://huggingface.co/ggerganov/whisper.cpp`, `https://github.com/k2-fsa/sherpa-onnx/releases`. Existence of a repository is NOT a verified app binary or pinned artifact.
- Documentation declares `MIT` or `Apache-2.0` but accurate licenses/notice obligations of each exact chosen Moonshine/SenseVoice/VAD/model weight distribution are UNVERIFIED.

## 6. Acceptance matrix / device variants

Full matrix: `phase00/PHASE00_ACCEPTANCE_MATRIX.csv`: 7 model entries, 14 available Compose screen/dialog entries plus 1 unverified dedicated Export/Share screen, 5 export capabilities, 4 translation routes, Android APIs 26/29/33/34/35/36 and 5 device/build/signer checks. **ALL runtime rows = UNVERIFIED**; existence of source code does not imply feature acceptance. Debug and Release use production `applicationId` unless documented signing constraints are observed; Preview uses `.preview` sandbox. 32-bit native ABI not declared, only `arm64-v8a`. Android 16 KiB page sizing is a linker argument only, no ELF or device validation.

## 7. Test results and reproducible commands

All source contracts are READ ONLY. Python tests are *deterministic static reproducibility aids*, NOT a substitute for Kotlin/Gradle/ARM64 runtime tests.

```sh
# In extracted TransVibe project directory; pass source ZIP path
python3 scripts/phase00_forensic_contracts.py --suite baseline --archive /path/to/TransVibe_Android_Project.zip
# PASSED: 6 / 6 tests; ZIP CRC/path/byte SHA inventory, identity, models, schema, original E01..18 references
python3 scripts/phase00_forensic_contracts.py --suite defects
# FAILED: 8 / 8 intentionally, existing defects/missing prerequisites reproduced; exit=1
python3 -m py_compile scripts/phase00_forensic_contracts.py
# PASSED: Python source syntax
# Android build commands: NOT RUN because Gradle+SDK are absent
# ./gradlew :app:assembleDebug :app:assemblePreview :app:testDebugUnitTest
# Device model/Android/ABI: NO DEVICE; APK install/launch/native ASR/Migration NOT RUN
```

Details/tracebacks preserved in `phase00/PHASE00_TEST_RESULTS.txt`. **Never interpret the eight failing regression tests as passes.** Running an archive-based baseline test against the full augmented ZIP will require the **exact original 98-member source ZIP**; read the included `phase00/PHASE00_README_AR.md`.

## 8. Phase gate / decisions

- **PASS:** Original archive integrity and exhaustive inventory; identity/catalog/profile source checks; legacy names and schema differ; E01..E18 evidence catalogued; static known-defect reproducers recorded; no original source modified.
- **FAILED, deliberately reproducible:** BUILD-001, BUILD-002, BUILD-003, MIG-001, MIG-002, TRANS-001, NATIVE-001, RESOURCE-001. These tests define follow-up work, not Phase00 code fixes.
- **NOT RUN:** Gradle build, Kotlin/Room test suite, native library init/transcription, language packs, Android UI tests, 16 KiB ELF test, actual APK signer comparison, migration fixture or phone performance.
- **Performance/quality:** NOT MEASURED.
- **RESULT:** `PHASE00 DOCUMENTATION/STATIC AUDIT COMPLETE; STRICT PHASE ACCEPTANCE BLOCKED` while known defects and missing device/run-time evidence remain. **NEXT PHASE ELIGIBILITY: NO automatic transition.** Owner must explicitly send Phase01; this phase does NOT pre-authorize fixes or a release.
- **Owner actions before future in-place production installation:** provide compatible signer proof and a privacy-safe backup/DB schema from the ACTUAL installed legacy app (not a raw keystore/password), approve migration mapping when Phase03 is reached, and approve resolution of Arabic route rule if deviating from literal plan.

## 9. Added files

The only additions inside project are:
- `scripts/phase00_forensic_contracts.py`
- `phase00/PHASE00_SOURCE_INVENTORY_SHA256.csv`
- `phase00/PHASE00_E01_E18_STATUS.csv`
- `phase00/PHASE00_ACCEPTANCE_MATRIX.csv`
- `phase00/PHASE00_LEGACY_EVIDENCE.md`
- `phase00/PHASE00_TEST_RESULTS.txt`
- `phase00/PHASE00_FORENSIC_REPORT.md`
- `phase00/PHASE00_README_AR.md`

Full source archive and changed-only archive SHA-256 will be emitted **outside** this file after packaging, to avoid circular self-hashing.
