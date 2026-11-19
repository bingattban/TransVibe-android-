package com.transvibe.app.asr

import com.transvibe.app.domain.AsrEngine
import com.transvibe.app.domain.AsrResult
import com.transvibe.app.domain.AudioSegment
import com.transvibe.app.domain.EngineType
import com.transvibe.app.domain.ModelDefinition
import com.transvibe.app.domain.ModelProfile
import com.transvibe.app.domain.TranscriptSegment
import com.transvibe.app.nativebridge.WhisperNativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class WhisperEngine : AsrEngine {
    override val engineId: String = "whisper_cpp_jni"

    private val mutex = Mutex()
    private var nativeHandle: Long = 0L
    private var activeModel: ModelDefinition? = null
    private var activeProfile: ModelProfile? = null

    override fun supportedCapabilities(model: ModelDefinition): Set<String> =
        if (model.engineType == EngineType.WHISPER_CPP) model.supportedLanguages else emptySet()

    override suspend fun open(
        model: ModelDefinition,
        installedPath: String,
        profile: ModelProfile
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!WhisperNativeBridge.isAvailable()) {
                return@withContext Result.failure(
                    IllegalStateException("NATIVE_LIBRARY_NOT_FOUND: whisper.cpp JNI library is not linked")
                )
            }
            val binFile = File(installedPath, model.physicalFileName)
            if (!binFile.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("MODEL_NOT_INSTALLED: Missing ${binFile.name}")
                )
            }
            if (nativeHandle != 0L) {
                WhisperNativeBridge.nativeFree(nativeHandle)
                nativeHandle = 0L
            }
            val handle = WhisperNativeBridge.nativeInit(binFile.absolutePath)
            if (handle == 0L) {
                return@withContext Result.failure(
                    IllegalStateException("INVALID_GGML_HEADER: whisper_init_from_file failed")
                )
            }
            nativeHandle = handle
            activeModel = model
            activeProfile = profile
            Result.success(Unit)
        }
    }

    override suspend fun transcribe(
        segment: AudioSegment,
        languageHint: String?
    ): Result<AsrResult> = withContext(Dispatchers.Default) {
        mutex.withLock {
            val handle = nativeHandle
            val model = activeModel
            val profile = activeProfile
            if (handle == 0L || model == null || profile == null) {
                return@withContext Result.failure(IllegalStateException("WhisperEngine not opened"))
            }
            val startMs = System.currentTimeMillis()
            val rawSegments = WhisperNativeBridge.nativeTranscribe(
                handle = handle,
                pcmData = segment.pcmSamples,
                numSamples = segment.pcmSamples.size,
                languageHint = languageHint,
                threads = profile.defaultThreads,
                greedyOnly = profile.greedyOnly,
                beamSize = profile.beamSize
            )
            val elapsedMs = System.currentTimeMillis() - startMs
            val mapped = rawSegments.mapIndexed { idx, seg ->
                val absStartUs = segment.startUs + (seg.startMs * 1000L)
                val absEndUs = (segment.startUs + (seg.endMs * 1000L)).coerceAtMost(segment.endUs)
                TranscriptSegment(
                    segmentId = "seg_${segment.index}_$idx",
                    sourceIndex = segment.index,
                    startUs = absStartUs,
                    endUs = absEndUs.coerceAtLeast(absStartUs + 1000L),
                    text = seg.text.trim(),
                    language = seg.detectedLanguage,
                    confidence = null,
                    engineId = engineId,
                    modelId = model.modelId
                )
            }
            Result.success(
                AsrResult(
                    segments = mapped,
                    detectedLanguage = mapped.firstOrNull()?.language ?: languageHint,
                    inferenceDurationMs = elapsedMs,
                    engineId = engineId,
                    modelId = model.modelId
                )
            )
        }
    }

    override fun cancelActive() {
        val handle = nativeHandle
        if (handle != 0L) {
            WhisperNativeBridge.nativeCancel(handle)
        }
    }

    override fun close() {
        val handle = nativeHandle
        nativeHandle = 0L
        if (handle != 0L) {
            WhisperNativeBridge.nativeFree(handle)
        }
        activeModel = null
        activeProfile = null
    }
}
