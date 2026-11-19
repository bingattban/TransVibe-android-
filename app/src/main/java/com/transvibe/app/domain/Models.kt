package com.transvibe.app.domain

import kotlinx.serialization.Serializable

enum class EngineType {
    WHISPER_CPP,
    SHERPA_MOONSHINE_AR,
    SHERPA_SENSEVOICE
}

enum class ArtifactFormat {
    GGML_BIN,
    DISTRIBUTION_ZIP
}

enum class ModelStatus {
    NOT_INSTALLED,
    TRANSFERRING,
    STAGING,
    VERIFYING,
    INSTALLING,
    READY,
    FAILED,
    CORRUPT,
    INCOMPATIBLE,
    REPAIRABLE
}

enum class ChecksumTrust {
    VERIFIED_PINNED,
    UNVERIFIED_LOCAL_ONLY
}

enum class FreezeStatus {
    NOT_STARTED,
    UNDER_DEVELOPMENT,
    MEASURED,
    QUALITY_REVIEW,
    OWNER_APPROVED,
    FROZEN,
    BLOCKED
}

enum class JobState {
    CREATED,
    QUEUED,
    INSPECTING,
    DECODING,
    RECOGNIZING,
    TRANSLATING,
    FINALIZING,
    COMPLETED,
    FAILED,
    CANCELLED,
    INTERRUPTED_RECOVERABLE
}

enum class ErrorCode {
    MODEL_NOT_INSTALLED,
    MODEL_UNTRUSTED,
    FILE_NAME_MISMATCH,
    INVALID_GGML_HEADER,
    INVALID_ZIP,
    MISSING_MODEL_ASSET,
    MODEL_ABI_INCOMPATIBLE,
    NATIVE_LIBRARY_NOT_FOUND,
    NOT_SUPPORTED_YET,
    LOW_MEMORY,
    LOW_STORAGE,
    NETWORK_UNAVAILABLE,
    TRANSFER_INTERRUPTED,
    UNSUPPORTED_CODEC,
    DRM_PROTECTED,
    NO_AUDIO_TRACK,
    SAF_PERMISSION_EXPIRED,
    TRANSLATION_PACK_MISSING,
    UNSUPPORTED_LANGUAGE_ROUTE,
    FOREGROUND_TIMEOUT,
    THERMAL_THROTTLED,
    CANCELLED,
    UNKNOWN
}

data class AppError(
    val code: ErrorCode,
    val message: String,
    val recoverable: Boolean = true,
    val cause: Throwable? = null
)

data class ModelDefinition(
    val modelId: String,
    val physicalFileName: String,
    val displayNameEn: String,
    val displayNameAr: String,
    val engineType: EngineType,
    val artifactFormat: ArtifactFormat,
    val variant: String,
    val supportedLanguages: Set<String>,
    val arabicOnly: Boolean,
    val highMemoryPolicy: Boolean,
    val minRamMb: Int,
    val minStorageMb: Int,
    val upstreamReference: String,
    val licenseName: String,
    val pinnedSha256: String? = null
)

data class InstalledModelRecord(
    val modelId: String,
    val status: ModelStatus,
    val installedPath: String?,
    val localSha256: String?,
    val checksumTrust: ChecksumTrust,
    val sizeBytes: Long,
    val profileVersion: Int,
    val validatedAtEpochMs: Long?,
    val lastError: ErrorCode? = null
)

@Serializable
data class ModelProfile(
    val modelId: String,
    val profileVersion: Int,
    val minThreads: Int,
    val maxThreads: Int,
    val defaultThreads: Int,
    val maxChunkDurationMs: Long,
    val vadSilenceMs: Long,
    val vadMinSpeechMs: Long,
    val vadPaddingMs: Long,
    val vadThreshold: Float,
    val greedyOnly: Boolean,
    val beamSize: Int,
    val resetContextPerChunk: Boolean,
    val freezeStatus: FreezeStatus,
    val baselineId: String? = null
)

class AudioSegment(
    val index: Int,
    val startUs: Long,
    val endUs: Long,
    val sampleRateHz: Int = 16000,
    val channels: Int = 1,
    val pcmSamples: FloatArray,
    val vadConfidence: Float? = null,
    private val onRelease: (() -> Unit)? = null
) : AutoCloseable {
    val durationUs: Long get() = (endUs - startUs).coerceAtLeast(0L)
    val byteSize: Int get() = pcmSamples.size * 4

    override fun close() {
        onRelease?.invoke()
    }
}

data class WordTimestamp(
    val word: String,
    val startUs: Long,
    val endUs: Long,
    val confidence: Float? = null
)

data class TranscriptSegment(
    val segmentId: String,
    val sourceIndex: Int,
    val startUs: Long,
    val endUs: Long,
    val text: String,
    val language: String?,
    val confidence: Float? = null,
    val engineId: String,
    val modelId: String,
    val words: List<WordTimestamp> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class AsrResult(
    val segments: List<TranscriptSegment>,
    val detectedLanguage: String?,
    val inferenceDurationMs: Long,
    val engineId: String,
    val modelId: String,
    val warnings: List<String> = emptyList()
)

data class TranslationResult(
    val segmentId: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val durationMs: Long
)

enum class CueValidationWarning {
    OVERLAP,
    NEGATIVE_OR_ZERO_DURATION,
    TOO_LONG_LINE,
    HIGH_READING_SPEED,
    MISSING_TRANSLATION,
    EMPTY_TEXT,
    SUSPECT_REPETITION,
    SILENCE_HALLUCINATION
}

data class SubtitleCue(
    val cueId: String,
    val sourceSegmentIds: List<String>,
    val startUs: Long,
    val endUs: Long,
    val originalText: String,
    val translatedText: String?,
    val userEditedOriginal: String? = null,
    val userEditedTranslation: String? = null,
    val revision: Int = 1,
    val warnings: Set<CueValidationWarning> = emptySet(),
    val modelId: String,
    val engineId: String
) {
    val effectiveOriginal: String get() = userEditedOriginal ?: originalText
    val effectiveTranslation: String? get() = userEditedTranslation ?: translatedText
}

@Serializable
data class SessionSnapshot(
    val jobId: String,
    val mediaUriString: String,
    val mediaTitle: String,
    val mediaContentFingerprint: String,
    val selectedAudioTrackIndex: Int,
    val selectedModelId: String,
    val actualEngineId: String,
    val actualModelId: String,
    val modelSha256: String?,
    val sourceLanguageSetting: String,
    val targetLanguageSetting: String,
    val preprocessingVersion: Int,
    val profileVersion: Int,
    val fallbackReason: String? = null
)

data class AudioTrackInfo(
    val trackIndex: Int,
    val mimeType: String,
    val sampleRateHz: Int,
    val channelCount: Int,
    val language: String?,
    val durationUs: Long
)

data class MediaInspectionResult(
    val uriString: String,
    val displayName: String,
    val durationUs: Long,
    val audioTracks: List<AudioTrackInfo>,
    val hasDrm: Boolean,
    val contentFingerprint: String
)

data class ProcessingProgress(
    val jobId: String,
    val state: JobState,
    val stageLabel: String,
    val processedAudioMs: Long,
    val totalAudioMs: Long,
    val elapsedMs: Long,
    val actualEngineId: String,
    val actualModelId: String,
    val warningMessage: String? = null
)

data class BenchmarkRunRecord(
    val runId: String,
    val modelId: String,
    val threads: Int,
    val runOrderIndex: Int,
    val mediaFingerprint: String,
    val modelSha256: String,
    val inferenceTimeMs: Long,
    val endToEndTimeMs: Long,
    val peakPssKb: Long,
    val thermalStatus: Int,
    val outputHash: String,
    val transcriptPreview: String,
    val timestampMs: Long
)
