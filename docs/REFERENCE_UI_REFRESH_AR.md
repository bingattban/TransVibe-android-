# Reference-inspired TransVibe Android UI refresh

Source: the screen/UI archive supplied by the application owner on 2026-11-20.

## Changes
- Modern Material 3 night/day palette, cyan and gold on a navy foundation.
- Persistent four-section bottom navigation for Home, Models, History and Settings; hides automatically on onboarding, processing and results.
- More legible home cards and responsive/scrollable settings and extended forms.
- Added persistent appearance preference to the existing DataStore (no database-schema change).
- Welcome page uses branded TV/play artwork. The Android GitHub branch uses a lightweight vector recreation of the supplied visual identity; the separately delivered full source ZIP retains the supplied optimized PNGs.
- Processing UI distinguishes *audio processed* from *translation completed* and keeps retry/cancel mechanisms unchanged.

## Safety
- The translation hang fix from commit e3ef638 stays in place.
- No changes to JNI, imported native Whisper binary, model identifiers, Room schema or signing secrets.
- Only the first model, whisper-tiny, is in testing. Do not advance without actual device evidence.

## Merge gate
The existing GitHub CI must pass source regression, Kotlin JVM tests, verified ARM64 JNI compatibility and TWO signed Preview/Release APK builds. The automatic merge will then proceed under main protection rules.
