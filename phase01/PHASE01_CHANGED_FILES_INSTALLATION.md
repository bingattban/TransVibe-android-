# Phase 01 — changed-files ZIP installation

**Prerequisite**: a copy of the exact project obtained from **Phase 00 full ZIP**.

1. BACK UP your working project; do not overwrite your older installed Android app.
2. Extract `TransVibe_Phase01_Changed_Files_Only_2026-11-19.zip` *into the
   parent directory containing `TransVibe/`*; preserve relative paths and allow
   replacement of the listed code/config files **only in the copied project**.
3. Verify `TransVibe/gradle/wrapper/gradle-wrapper.jar` SHA-256 against
   `gradle/wrapper/WRAPPER_PROVENANCE.md` and review build-system safety notes.
4. Run `cd TransVibe && python3 -m unittest discover -s phase01/tests -p 'test_*.py' -v`
   on a computer with Python; use `./gradlew :app:testDebugUnitTest :app:assemblePreview`
   in a real Android build environment. Python test success is NOT APK success.
5. The release build must fail until genuine Whisper and Sherpa native runtimes
   are installed. Do not bypass it by changing CMake flags or renaming a stub.
6. Never install a new `com.transvibe.app` APK over the old app unless the
   existing signing certificate and versionCode upgrade have been verified.

No original Phase 00 audit files were removed or changed. Database and model
migration are explicitly out of scope for Phase 01.
