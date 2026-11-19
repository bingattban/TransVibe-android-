package com.transvibe.app.nativebridge

import java.io.File

/**
 * Reflection-safe bridge to the pinned Sherpa-ONNX Android AAR when placed in app/libs/.
 * Invokes real Sherpa-ONNX OfflineRecognizer when available; fails cleanly if AAR or assets are absent.
 */
object SherpaNativeBridge {
    fun isSherpaAarPresent(): Boolean {
        return try {
            Class.forName("com.k2fsa.sherpa.onnx.OfflineRecognizer")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    fun transcribeWithSherpaReflection(
        installedDir: String,
        samples16k: FloatArray,
        sampleRateHz: Int,
        isMoonshine: Boolean,
        threads: Int,
        languageHint: String?
    ): Result<String> {
        if (!isSherpaAarPresent()) {
            return Result.failure(IllegalStateException("NATIVE_LIBRARY_NOT_FOUND: sherpa-onnx AAR missing"))
        }
        return try {
            val dir = File(installedDir)
            val tokens = dir.walkTopDown().firstOrNull { it.isFile && it.name.contains("tokens") }
                ?: return Result.failure(IllegalStateException("MISSING_MODEL_ASSET: tokens.txt not found"))

            val recognizerCls = Class.forName("com.k2fsa.sherpa.onnx.OfflineRecognizer")
            val configCls = Class.forName("com.k2fsa.sherpa.onnx.OfflineRecognizerConfig")
            val modelCfgCls = Class.forName("com.k2fsa.sherpa.onnx.OfflineModelConfig")

            val modelConfig = modelCfgCls.getDeclaredConstructor().newInstance()
            modelCfgCls.getMethod("setTokens", String::class.java).invoke(modelConfig, tokens.absolutePath)
            modelCfgCls.getMethod("setNumThreads", Int::class.javaPrimitiveType).invoke(modelConfig, threads)

            if (isMoonshine) {
                val pre = dir.walkTopDown().firstOrNull { it.isFile && it.name.contains("preprocess") }
                val enc = dir.walkTopDown().firstOrNull { it.isFile && it.name.contains("encode") }
                val uncached = dir.walkTopDown().firstOrNull { it.isFile && it.name.contains("uncached") }
                val cached = dir.walkTopDown().firstOrNull { it.isFile && it.name.contains("cached") && !it.name.contains("uncached") }
                if (pre == null || enc == null || uncached == null || cached == null) {
                    return Result.failure(IllegalStateException("MISSING_MODEL_ASSET: Incomplete Moonshine ONNX files"))
                }
                val moonCls = Class.forName("com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig")
                val moonCfg = moonCls.getDeclaredConstructor().newInstance()
                moonCls.getMethod("setPreprocessor", String::class.java).invoke(moonCfg, pre.absolutePath)
                moonCls.getMethod("setEncoder", String::class.java).invoke(moonCfg, enc.absolutePath)
                moonCls.getMethod("setUncachedDecoder", String::class.java).invoke(moonCfg, uncached.absolutePath)
                moonCls.getMethod("setCachedDecoder", String::class.java).invoke(moonCfg, cached.absolutePath)
                modelCfgCls.getMethod("setMoonshine", moonCls).invoke(modelConfig, moonCfg)
            } else {
                val modelFile = dir.walkTopDown().firstOrNull { it.isFile && (it.extension == "onnx" || it.extension == "ort") }
                    ?: return Result.failure(IllegalStateException("MISSING_MODEL_ASSET: SenseVoice ONNX file not found"))
                val svCls = Class.forName("com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig")
                val svCfg = svCls.getDeclaredConstructor().newInstance()
                svCls.getMethod("setModel", String::class.java).invoke(svCfg, modelFile.absolutePath)
                svCls.getMethod("setLanguage", String::class.java).invoke(svCfg, languageHint ?: "auto")
                modelCfgCls.getMethod("setSenseVoice", svCls).invoke(modelConfig, svCfg)
            }

            val recConfig = configCls.getDeclaredConstructor().newInstance()
            configCls.getMethod("setModelConfig", modelCfgCls).invoke(recConfig, modelConfig)

            val recognizer = recognizerCls.getDeclaredConstructor(configCls).newInstance(recConfig)
            try {
                val stream = recognizerCls.getMethod("createStream").invoke(recognizer)
                val streamCls = stream.javaClass
                streamCls.getMethod("acceptWaveform", FloatArray::class.java, Int::class.javaPrimitiveType)
                    .invoke(stream, samples16k, sampleRateHz)
                recognizerCls.getMethod("decode", streamCls).invoke(recognizer, stream)
                val resultObj = recognizerCls.getMethod("getResult", streamCls).invoke(recognizer, stream)
                val text = resultObj.javaClass.getMethod("getText").invoke(resultObj) as? String ?: ""
                streamCls.getMethod("release").invoke(stream)
                Result.success(text.trim())
            } finally {
                recognizerCls.getMethod("release").invoke(recognizer)
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
