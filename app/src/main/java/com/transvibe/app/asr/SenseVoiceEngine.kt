package com.transvibe.app.asr

import com.transvibe.app.domain.AsrEngine
import com.transvibe.app.domain.AsrResult
import com.transvibe.app.domain.AudioSegment
import com.transvibe.app.domain.ModelDefinition
import com.transvibe.app.domain.ModelProfile
import com.transvibe.app.domain.TranscriptSegment
import com.transvibe.app.nativebridge.SherpaNativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dedicated adapter for sensevoice-small-int8.zip via pinned Sherpa-ONNX runtime.
 * Verified languages: zh, yue, en, ja, ko. Never infers Arabic ASR support.
 */
class SenseVoiceEngine : AsrEngine {
    override val engineId: String = "sherpa_sensevoice"
    private var activeModel: ModelDefinition? = null
    private var activeProfile: ModelProfile? = null
    private var installedPath: String? = null

    override fun supportedCapabilities(model: ModelDefinition): Set<String> =
        setOf("zh", "yue", "en", "ja", "ko")

    override suspend fun open(
        model: ModelDefinition,
        installedPath: String,
        profile: ModelProfile
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SherpaNativeBridge.isSherpaAarPresent()) {
            return@withContext Result.failure(
                IllegalStateException("NATIVE_LIBRARY_NOT_FOUND: sherpa-onnx AAR not bundled in app/libs/")
            )
        }
        this@SenseVoiceEngine.activeModel = model
        this@SenseVoiceEngine.activeProfile = profile
        this@SenseVoiceEngine.installedPath = installedPath
        Result.success(Unit)
    }

    override suspend fun transcribe(
        segment: AudioSegment,
        languageHint: String?
    ): Result<AsrResult> = withContext(Dispatchers.Default) {
        val model = activeModel ?: return@withContext Result.failure(IllegalStateException("SenseVoiceEngine not opened"))
        val profile = activeProfile ?: return@withContext Result.failure(IllegalStateException("Profile missing"))
        val dir = installedPath ?: return@withContext Result.failure(IllegalStateException("Model path missing"))

        if (languageHint != null && languageHint != "auto" && languageHint !in supportedCapabilities(model)) {
            return@withContext Result.failure(
                IllegalArgumentException("Language $languageHint not supported by SenseVoice Small INT8")
            )
        }
        val startMs = System.currentTimeMillis()
        val text = SherpaNativeBridge.transcribeWithSherpaReflection(
            installedDir = dir,
            samples16k = segment.pcmSamples,
            sampleRateHz = segment.sampleRateHz,
            isMoonshine = false,
            threads = profile.defaultThreads,
            languageHint = languageHint ?: "auto"
        ).getOrElse { return@withContext Result.failure(it) }

        val elapsedMs = System.currentTimeMillis() - startMs
        val seg = TranscriptSegment(
            segmentId = "sv_${segment.index}_0",
            sourceIndex = segment.index,
            startUs = segment.startUs,
            endUs = segment.endUs,
            text = text,
            language = languageHint,
            engineId = engineId,
            modelId = model.modelId
        )
        Result.success(
            AsrResult(
                segments = listOf(seg),
                detectedLanguage = languageHint,
                inferenceDurationMs = elapsedMs,
                engineId = engineId,
                modelId = model.modelId
            )
        )
    }

    override fun cancelActive() {}

    override fun close() {
        activeModel = null
        activeProfile = null
        installedPath = null
    }
}
