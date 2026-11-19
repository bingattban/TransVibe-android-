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
 * Dedicated adapter for moonshine-base-ar.zip via pinned Sherpa-ONNX runtime.
 * Arabic ONLY. Never silently falls back to Whisper.
 */
class MoonshineArabicEngine : AsrEngine {
    override val engineId: String = "sherpa_moonshine_ar"
    private var activeModel: ModelDefinition? = null
    private var activeProfile: ModelProfile? = null
    private var installedPath: String? = null

    override fun supportedCapabilities(model: ModelDefinition): Set<String> = setOf("ar")

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
        this@MoonshineArabicEngine.activeModel = model
        this@MoonshineArabicEngine.activeProfile = profile
        this@MoonshineArabicEngine.installedPath = installedPath
        Result.success(Unit)
    }

    override suspend fun transcribe(
        segment: AudioSegment,
        languageHint: String?
    ): Result<AsrResult> = withContext(Dispatchers.Default) {
        val model = activeModel ?: return@withContext Result.failure(IllegalStateException("MoonshineArabicEngine not opened"))
        val profile = activeProfile ?: return@withContext Result.failure(IllegalStateException("Profile missing"))
        val dir = installedPath ?: return@withContext Result.failure(IllegalStateException("Model path missing"))

        if (languageHint != null && languageHint != "auto" && languageHint != "ar") {
            return@withContext Result.failure(
                IllegalArgumentException("Moonshine Base Arabic supports Arabic speech only")
            )
        }
        val startMs = System.currentTimeMillis()
        val text = SherpaNativeBridge.transcribeWithSherpaReflection(
            installedDir = dir,
            samples16k = segment.pcmSamples,
            sampleRateHz = segment.sampleRateHz,
            isMoonshine = true,
            threads = profile.defaultThreads,
            languageHint = "ar"
        ).getOrElse { return@withContext Result.failure(it) }

        val elapsedMs = System.currentTimeMillis() - startMs
        val seg = TranscriptSegment(
            segmentId = "moon_${segment.index}_0",
            sourceIndex = segment.index,
            startUs = segment.startUs,
            endUs = segment.endUs,
            text = text,
            language = "ar",
            engineId = engineId,
            modelId = model.modelId
        )
        Result.success(
            AsrResult(
                segments = listOf(seg),
                detectedLanguage = "ar",
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
