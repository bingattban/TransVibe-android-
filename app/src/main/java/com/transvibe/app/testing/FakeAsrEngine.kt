package com.transvibe.app.testing

import com.transvibe.app.domain.AsrEngine
import com.transvibe.app.domain.AsrResult
import com.transvibe.app.domain.AudioSegment
import com.transvibe.app.domain.ModelDefinition
import com.transvibe.app.domain.ModelProfile
import com.transvibe.app.domain.TranscriptSegment

/**
 * Deterministic fake ASR engine strictly for unit/integration tests. Never wired into production.
 */
class FakeAsrEngine(
    private val fixedText: String = "Sample recognized speech"
) : AsrEngine {
    override val engineId: String = "fake_test_asr"
    private var modelId: String = "test"

    override fun supportedCapabilities(model: ModelDefinition): Set<String> = model.supportedLanguages

    override suspend fun open(
        model: ModelDefinition,
        installedPath: String,
        profile: ModelProfile
    ): Result<Unit> {
        modelId = model.modelId
        return Result.success(Unit)
    }

    override suspend fun transcribe(
        segment: AudioSegment,
        languageHint: String?
    ): Result<AsrResult> {
        return Result.success(
            AsrResult(
                segments = listOf(
                    TranscriptSegment(
                        segmentId = "fake_${segment.index}",
                        sourceIndex = segment.index,
                        startUs = segment.startUs,
                        endUs = segment.endUs,
                        text = fixedText,
                        language = languageHint ?: "en",
                        engineId = engineId,
                        modelId = modelId
                    )
                ),
                detectedLanguage = languageHint ?: "en",
                inferenceDurationMs = 12L,
                engineId = engineId,
                modelId = modelId
            )
        )
    }

    override fun cancelActive() {}
    override fun close() {}
}
