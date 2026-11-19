package com.transvibe.app.media

import android.content.Context
import com.transvibe.app.domain.ModelProfile
import com.transvibe.app.domain.SpeechRegion
import com.transvibe.app.domain.VadEngine
import com.transvibe.app.nativebridge.SherpaNativeBridge
import java.io.File
import kotlin.math.sqrt

/**
 * Silence-aware VAD chunk planner.
 * Uses real Sherpa-ONNX Silero VAD when silero_vad.onnx is present in filesDir/aux_models/vad/
 * and Sherpa AAR is bundled; otherwise falls back cleanly to profile-calibrated RMS speech detection.
 */
class SileroVadEngine(private val context: Context? = null) : VadEngine {

    fun isSileroOnnxAvailable(): Boolean {
        val ctx = context ?: return false
        val modelFile = File(ctx.filesDir, "aux_models/vad/silero_vad.onnx")
        return modelFile.exists() && SherpaNativeBridge.isSherpaAarPresent()
    }

    override suspend fun detectSpeechRegions(
        samples16k: FloatArray,
        baseOffsetUs: Long,
        profile: ModelProfile
    ): List<SpeechRegion> {
        if (samples16k.isEmpty()) return emptyList()
        if (isSileroOnnxAvailable()) {
            val onnxRegions = runSherpaSileroReflection(samples16k, baseOffsetUs, profile)
            if (onnxRegions != null) return onnxRegions
        }
        return detectEnergyRegions(samples16k, baseOffsetUs, profile)
    }

    private fun runSherpaSileroReflection(
        samples16k: FloatArray,
        baseOffsetUs: Long,
        profile: ModelProfile
    ): List<SpeechRegion>? {
        val ctx = context ?: return null
        val modelFile = File(ctx.filesDir, "aux_models/vad/silero_vad.onnx")
        if (!modelFile.exists()) return null
        return try {
            val sileroCfgCls = Class.forName("com.k2fsa.sherpa.onnx.SileroVadModelConfig")
            val vadCfgCls = Class.forName("com.k2fsa.sherpa.onnx.VadModelConfig")
            val vadCls = Class.forName("com.k2fsa.sherpa.onnx.Vad")

            val sileroCfg = sileroCfgCls.getDeclaredConstructor().newInstance()
            sileroCfgCls.getMethod("setModel", String::class.java).invoke(sileroCfg, modelFile.absolutePath)
            sileroCfgCls.getMethod("setThreshold", Float::class.javaPrimitiveType).invoke(sileroCfg, profile.vadThreshold)
            sileroCfgCls.getMethod("setMinSilenceDuration", Float::class.javaPrimitiveType).invoke(sileroCfg, profile.vadSilenceMs / 1000f)
            sileroCfgCls.getMethod("setMinSpeechDuration", Float::class.javaPrimitiveType).invoke(sileroCfg, profile.vadMinSpeechMs / 1000f)

            val vadCfg = vadCfgCls.getDeclaredConstructor().newInstance()
            vadCfgCls.getMethod("setSileroVadModelConfig", sileroCfgCls).invoke(vadCfg, sileroCfg)
            vadCfgCls.getMethod("setSampleRate", Int::class.javaPrimitiveType).invoke(vadCfg, 16000)

            val vad = vadCls.getDeclaredConstructor(vadCfgCls).newInstance(vadCfg)
            try {
                vadCls.getMethod("acceptWaveform", FloatArray::class.java).invoke(vad, samples16k)
                val regions = mutableListOf<SpeechRegion>()
                val emptyMethod = vadCls.getMethod("empty")
                val frontMethod = vadCls.getMethod("front")
                val popMethod = vadCls.getMethod("pop")
                while (!(emptyMethod.invoke(vad) as Boolean)) {
                    val seg = frontMethod.invoke(vad)
                    val startSample = (seg.javaClass.getMethod("getStart").invoke(seg) as Int).toLong()
                    val samplesArr = seg.javaClass.getMethod("getSamples").invoke(seg) as FloatArray
                    val startUs = baseOffsetUs + Timebase.samplesToMicros(startSample)
                    val endUs = startUs + Timebase.samplesToMicros(samplesArr.size.toLong())
                    regions.add(SpeechRegion(startUs, endUs, 0.90f))
                    popMethod.invoke(vad)
                }
                regions
            } finally {
                vadCls.getMethod("release").invoke(vad)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun detectEnergyRegions(
        samples16k: FloatArray,
        baseOffsetUs: Long,
        profile: ModelProfile
    ): List<SpeechRegion> {
        val frameSamples = 480
        val frameDurationUs = Timebase.samplesToMicros(frameSamples.toLong())
        val silenceFramesNeeded = (profile.vadSilenceMs * 1000L / frameDurationUs).toInt().coerceAtLeast(1)
        val minSpeechUs = profile.vadMinSpeechMs * 1000L
        val maxChunkUs = profile.maxChunkDurationMs * 1000L
        val paddingUs = profile.vadPaddingMs * 1000L

        val regions = mutableListOf<SpeechRegion>()
        var inSpeech = false
        var speechStartUs = baseOffsetUs
        var lastSpeechUs = baseOffsetUs
        var silentFrames = 0
        val energyThreshold = 0.012f * (profile.vadThreshold / 0.5f).coerceIn(0.4f, 1.8f)

        var offset = 0
        while (offset < samples16k.size) {
            val end = (offset + frameSamples).coerceAtMost(samples16k.size)
            var sumSq = 0.0
            for (i in offset until end) {
                val s = samples16k[i]
                sumSq += s * s
            }
            val rms = sqrt(sumSq / (end - offset).coerceAtLeast(1)).toFloat()
            val frameStartUs = baseOffsetUs + Timebase.samplesToMicros(offset.toLong())
            val frameEndUs = baseOffsetUs + Timebase.samplesToMicros(end.toLong())

            if (rms >= energyThreshold) {
                if (!inSpeech) {
                    inSpeech = true
                    speechStartUs = (frameStartUs - paddingUs).coerceAtLeast(baseOffsetUs)
                }
                lastSpeechUs = frameEndUs
                silentFrames = 0
                if (frameEndUs - speechStartUs >= maxChunkUs) {
                    regions.add(SpeechRegion(speechStartUs, frameEndUs, 0.85f))
                    speechStartUs = (frameEndUs - paddingUs).coerceAtLeast(baseOffsetUs)
                }
            } else if (inSpeech) {
                silentFrames++
                if (silentFrames >= silenceFramesNeeded) {
                    val regionEnd = lastSpeechUs + paddingUs
                    if (regionEnd - speechStartUs >= minSpeechUs) {
                        regions.add(SpeechRegion(speechStartUs, regionEnd, 0.80f))
                    }
                    inSpeech = false
                    silentFrames = 0
                }
            }
            offset = end
        }
        if (inSpeech) {
            val endTotalUs = baseOffsetUs + Timebase.samplesToMicros(samples16k.size.toLong())
            if (endTotalUs - speechStartUs >= minSpeechUs) {
                regions.add(SpeechRegion(speechStartUs, endTotalUs, 0.80f))
            }
        }
        return regions
    }

    override fun close() {}
}
