# Phase 00 — Read-only comparison against an actual legacy source archive

- Current project **exact original ZIP** SHA-256: `928362b02683aeb86f6eadc53cac8c2212f85ff2b3d6a275723363800943d880` (98 original members).
- Legacy materialized reference: `TransVibe_v1.0.4_Whisper_Tiny_Phase1_CodeOnTheGo_FULL_LOCAL.zip`; SHA-256 `9974afa300747c5664cb2b20452c7d0cf9dd9c6d9a0b7824822cb5273598b493`; source root within ZIP: `TransVibe-Whisper-Tiny-Phase1-CodeOnTheGo/`.
- This archive was NOT added to the current project ZIP, and no legacy files were modified.
- This is **real legacy source**, but NOT proof that it exactly matches the user's currently installed APK, certificate, or latest phone database. No installed package/keystore/device inspection was possible.

## Verified evidence (legacy internal project-relative paths, 1-based line numbers)

| Item | Legacy source evidence | Current source evidence | Compatibility result |
|---|---|---|---|
| Android identity | `app/build.gradle.kts:40-49` => `com.transvibe.app`, `versionCode=5`, `versionName=1.0.4` | `app/build.gradle.kts:10-18` => same ID, versionCode=6 | Package/version path plausible; signing certificate **UNVERIFIED** |
| Room DB filename | `app/src/main/java/com/transvibe/app/data/persistence/TransVibeDatabase.kt:454-455,555-559` => `processing_jobs.db` | `app/src/main/java/com/transvibe/app/persistence/TransVibeDatabase.kt:58-66` => `transvibe.db` | **MIG-001: not an automatic in-place migration** |
| Room schema | `TransVibeDatabase.kt:441-449` version 5; tables `processing_jobs`,`subtitle_segments`,`legacy_session_migrations`,`migration_markers` (lines 20,51,70,85) | `Entities.kt:7,31,48,64` and `TransVibeDatabase.kt:10-20` => version 3; tables `jobs`,`transcript_segments`,`translations`,`cue_edits`,`benchmark_runs`,`freeze_records` | New 1->2->3 migrations do NOT cover legacy schema 5 |
| DataStore name | `data/persistence/AppPreferences.kt:19` => `transvibe_preferences` | `persistence/UserPreferencesStore.kt:11` => `transvibe_prefs` | **MIG-002: settings will not be read by new storage without explicit migration** |
| DataStore keys | `data/persistence/AppPreferences.kt:24-44` includes processing, language, output tree URI, UI scale, model choice etc | `persistence/UserPreferencesStore.kt:21-26` four keys | Missing legacy keys/default migration |
| Legacy Whisper weights | `data/model/ModelStore.kt:24-33` => `filesDir/models/whisper/`, metadata `filesDir/models/metadata/`, shared prefs `model_integrity` | `models/ModelLifecycleManager.kt:39-40,55-57` => `filesDir/models/<modelId>/` | **MIG-003** preserve/relink/verify existing installed files; do not delete |
| Legacy Moonshine | `data/asr/MoonshineModelStore.kt:44-50` => `filesDir/models/moonshine/tiny-en-v2/`; `AdditionalMoonshineModelStore.kt:31` => `filesDir/models/moonshine/<modelId>` | Current `filesDir/models/<modelId>/` | Legacy model variants may differ; never relabel unsupported models |
| Legacy SenseVoice | `data/asr/SenseVoiceModelStore.kt:37-41` => `filesDir/models/sensevoice/small-int8/` | Current `filesDir/models/<modelId>/` | **MIG-003** model layout incompatible without mapping |
| Legacy session storage | `data/storage/StorageRepository.kt:16-22` => `filesDir/sessions` and `processing_jobs.db` | Current Room `jobs/transcript_segments` | **MIG-004** preserve legacy sessions/JSON & cue edits |
| SAF saved tree URI | `data/persistence/AppPreferences.kt:44,174-178` => key `subtitle_output_tree_uri`; `TransVibeDatabase.kt:42` => `source_uri_persistable` | Current `platform/SafStorageHelper.kt:8-17` supports permission attempt, but no legacy key migration | **MIG-005** persist/revalidate URI grants and relink inaccessible documents |
| Signing | Legacy `app/build.gradle.kts:35-37,66-76,97-102` expects owner-provided release keystore env vars | Current build.gradle.kts lacks explicit owner signing integration | **SIGN-001** cannot verify same certificate lineage from source; no production replacement allowed |
| Manifest backups | Legacy `app/src/main/AndroidManifest.xml:22` `android:allowBackup=false` | Current `app/src/main/AndroidManifest.xml:17-19` backup true with rules | Backup/update data safety needs review; no device install test |

## Safe migration prerequisites — NOT IMPLEMENTED IN PHASE 00

1. Obtain owner/device-reported installed APK package versionCode and signing certificate fingerprint; compare with owner keystore certificate **without requesting/disclosing secrets**.
2. Read-only dump of actual installed Room schema (or a user-consented backup/export), database filenames and paths on the actual handset; no assumptions from the sample ZIP.
3. Implement idempotent import/convert for legacy schema 5 into the new schema and transfer legacy DataStore keys/model metadata/files, SAF permissions and session/cue edits; audit all old types/IDs.
4. Validate with staged tests on a separate Preview/test sandbox; preserve rollback; never delete or overwrite originals before acceptance.

**Phase 00 result: migration risks are verified. Migration is deliberately NOT executed; Phase 03 requires owner-approved in-place strategy.**
