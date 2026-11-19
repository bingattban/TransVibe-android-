# Phase 01 — Code On The Go: UI-only handoff

The authoritative instructions are in `phase01/COTG_UI_ONLY_AR.md`. Follow
only the application's **Sync / Build** interface: no terminal is required.

The observed phone reports Gradle 9.6.1 and Java 21.0.10. The project now
requests AGP 9.3.1, Kotlin 2.3.21 and KSP 2.3.14 to align with the official
Code On The Go migration guide. Network access may be necessary once to
resolve third-party dependencies unavailable in the IDE's local Maven mirror.

Choose **Preview** when available. `com.transvibe.app.preview` cannot access
the production app's private data. Debug has the production ID and must not
be used to replace the installed production app without matching signing.

The diagnostic JNI bridge is only a debug/preview placeholder. Release remains
fail-closed without real native libraries. A successful Build does not prove
real ASR inference or correct on-device subtitles.
