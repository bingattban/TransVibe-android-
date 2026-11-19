# TransVibe Architecture

Layered Clean/Hexagonal package structure under `com.transvibe.app`:
- `presentation/`: Compose Material 3 RTL screens (A–M), Navigation, ViewModel, Midnight Navy / Electric Cyan / Warm Gold theme.
- `domain/`: Immutable contracts (`AsrEngine`, `AudioDecoder`, `VadEngine`, `TranslationEngine`, `SubtitleExporter`), policies, and typed models.
- `models/`: Immutable 7-model `ModelCatalog`, `GgmlHeaderValidator`, `ZipSecurityValidator` (ZIP-slip/bomb protection), transactional `ModelLifecycleManager`.
- `media/`: `MediaInspector`, streaming audio-only `MediaCodecAudioDecoder`, `AudioResampler` (16 kHz mono Float32), `SileroVadEngine`, `BoundaryReconciler`, integer-microsecond `Timebase`.
- `asr/`: 3 independent adapters (`WhisperEngine`, `MoonshineArabicEngine`, `SenseVoiceEngine`), 7 isolated `ModelProfiles`, strict `AsrRouter` (zero silent Whisper fallback).
- `nativebridge/`: `WhisperNativeBridge` + `app/src/main/cpp/whisper_jni.cpp` (16 KB page aligned), `SherpaNativeBridge`.
- `translation/`: `MlKitTranslationEngine` (offline after pack download), `TranslationQueue`.
- `subtitles/`: `SubtitleCueValidator`, `HallucinationDetector`, `SrtVttExporter`, `SubtitleEditorRepository`.
- `persistence/`: Room DB (`TransVibeDatabase` v2 with `MIGRATION_1_2`) storing transcripts independently from translations; DataStore `UserPreferencesStore`.
- `processing/`: Bounded `Channel(capacity = 2)` `ProcessingOrchestrator` with idempotent segment commits and checkpoint resume.
- `platform/`: `TransVibeProcessingService` (`FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING`, Android 15+ `onTimeout`), `ThermalMonitor`, `SafStorageHelper`.
- `diagnostics/`: `JobTelemetry`, `TinyBenchmarkRunner` (Debug/Preview only), `DiagnosticExporter` (redacted).
