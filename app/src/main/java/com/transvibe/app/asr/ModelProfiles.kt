package com.transvibe.app.asr

import com.transvibe.app.domain.FreezeStatus
import com.transvibe.app.domain.ModelProfile
import com.transvibe.app.models.ModelCatalog

/**
 * Seven completely isolated, versioned model profiles.
 * Modifying Tiny NEVER alters Base/Small/Medium/Turbo, Moonshine, or SenseVoice.
 */
object ModelProfiles {

    val WHISPER_TINY = ModelProfile(
        modelId = ModelCatalog.WHISPER_TINY_ID,
        profileVersion = 1,
        minThreads = 2,
        maxThreads = 4,
        defaultThreads = 3,
        maxChunkDurationMs = 25_000L,
        vadSilenceMs = 500L,
        vadMinSpeechMs = 250L,
        vadPaddingMs = 150L,
        vadThreshold = 0.50f,
        greedyOnly = true,
        beamSize = 1,
        resetContextPerChunk = true,
        freezeStatus = FreezeStatus.UNDER_DEVELOPMENT,
        baselineId = "tiny_baseline_pending"
    )

    val WHISPER_BASE = ModelProfile(
        modelId = ModelCatalog.WHISPER_BASE_ID,
        profileVersion = 1,
        minThreads = 2,
        maxThreads = 4,
        defaultThreads = 4,
        maxChunkDurationMs = 25_000L,
        vadSilenceMs = 500L,
        vadMinSpeechMs = 250L,
        vadPaddingMs = 150L,
        vadThreshold = 0.50f,
        greedyOnly = true,
        beamSize = 1,
        resetContextPerChunk = true,
        freezeStatus = FreezeStatus.NOT_STARTED
    )

    val WHISPER_SMALL = ModelProfile(
        modelId = ModelCatalog.WHISPER_SMALL_ID,
        profileVersion = 1,
        minThreads = 2,
        maxThreads = 4,
        defaultThreads = 4,
        maxChunkDurationMs = 22_000L,
        vadSilenceMs = 550L,
        vadMinSpeechMs = 250L,
        vadPaddingMs = 180L,
        vadThreshold = 0.50f,
        greedyOnly = false,
        beamSize = 2,
        resetContextPerChunk = true,
        freezeStatus = FreezeStatus.NOT_STARTED
    )

    val WHISPER_MEDIUM = ModelProfile(
        modelId = ModelCatalog.WHISPER_MEDIUM_ID,
        profileVersion = 1,
        minThreads = 2,
        maxThreads = 4,
        defaultThreads = 4,
        maxChunkDurationMs = 20_000L,
        vadSilenceMs = 600L,
        vadMinSpeechMs = 250L,
        vadPaddingMs = 200L,
        vadThreshold = 0.50f,
        greedyOnly = false,
        beamSize = 2,
        resetContextPerChunk = true,
        freezeStatus = FreezeStatus.NOT_STARTED
    )

    val WHISPER_LARGE_V3_TURBO = ModelProfile(
        modelId = ModelCatalog.WHISPER_LARGE_V3_TURBO_ID,
        profileVersion = 1,
        minThreads = 2,
        maxThreads = 4,
        defaultThreads = 4,
        maxChunkDurationMs = 18_000L,
        vadSilenceMs = 600L,
        vadMinSpeechMs = 250L,
        vadPaddingMs = 200L,
        vadThreshold = 0.50f,
        greedyOnly = true,
        beamSize = 1,
        resetContextPerChunk = true,
        freezeStatus = FreezeStatus.NOT_STARTED
    )

    val MOONSHINE_BASE_AR = ModelProfile(
        modelId = ModelCatalog.MOONSHINE_BASE_AR_ID,
        profileVersion = 1,
        minThreads = 2,
        maxThreads = 4,
        defaultThreads = 2,
        maxChunkDurationMs = 15_000L,
        vadSilenceMs = 450L,
        vadMinSpeechMs = 250L,
        vadPaddingMs = 150L,
        vadThreshold = 0.50f,
        greedyOnly = true,
        beamSize = 1,
        resetContextPerChunk = true,
        freezeStatus = FreezeStatus.NOT_STARTED
    )

    val SENSEVOICE_SMALL_INT8 = ModelProfile(
        modelId = ModelCatalog.SENSEVOICE_SMALL_INT8_ID,
        profileVersion = 1,
        minThreads = 2,
        maxThreads = 4,
        defaultThreads = 4,
        maxChunkDurationMs = 20_000L,
        vadSilenceMs = 500L,
        vadMinSpeechMs = 250L,
        vadPaddingMs = 150L,
        vadThreshold = 0.50f,
        greedyOnly = true,
        beamSize = 1,
        resetContextPerChunk = true,
        freezeStatus = FreezeStatus.NOT_STARTED
    )

    private val all = mapOf(
        ModelCatalog.WHISPER_TINY_ID to WHISPER_TINY,
        ModelCatalog.WHISPER_BASE_ID to WHISPER_BASE,
        ModelCatalog.WHISPER_SMALL_ID to WHISPER_SMALL,
        ModelCatalog.WHISPER_MEDIUM_ID to WHISPER_MEDIUM,
        ModelCatalog.WHISPER_LARGE_V3_TURBO_ID to WHISPER_LARGE_V3_TURBO,
        ModelCatalog.MOONSHINE_BASE_AR_ID to MOONSHINE_BASE_AR,
        ModelCatalog.SENSEVOICE_SMALL_INT8_ID to SENSEVOICE_SMALL_INT8
    )

    fun forModel(modelId: String): ModelProfile =
        all[modelId] ?: throw IllegalArgumentException("Unknown modelId: $modelId")
}
