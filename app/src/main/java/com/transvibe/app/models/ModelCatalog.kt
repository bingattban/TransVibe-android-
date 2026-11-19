package com.transvibe.app.models

import com.transvibe.app.domain.ArtifactFormat
import com.transvibe.app.domain.EngineType
import com.transvibe.app.domain.ModelDefinition

/**
 * Immutable catalog of the EXACT SEVEN supported ASR artifacts.
 * No eighth model, no silent quantized substitutes, no arbitrary aliases.
 */
object ModelCatalog {
    const val WHISPER_TINY_ID = "whisper-tiny"
    const val WHISPER_BASE_ID = "whisper-base"
    const val WHISPER_SMALL_ID = "whisper-small"
    const val WHISPER_MEDIUM_ID = "whisper-medium"
    const val WHISPER_LARGE_V3_TURBO_ID = "whisper-large-v3-turbo"
    const val MOONSHINE_BASE_AR_ID = "moonshine-base-ar"
    const val SENSEVOICE_SMALL_INT8_ID = "sensevoice-small-int8"

    val ALL: List<ModelDefinition> = listOf(
        ModelDefinition(
            modelId = WHISPER_BASE_ID,
            physicalFileName = "ggml-base.bin",
            displayNameEn = "Whisper Base",
            displayNameAr = "ويسبر الأساسي (Base)",
            engineType = EngineType.WHISPER_CPP,
            artifactFormat = ArtifactFormat.GGML_BIN,
            variant = "Base",
            supportedLanguages = setOf("auto", "ar", "en", "fr", "de", "es", "tr", "ur", "ja", "ko", "zh"),
            arabicOnly = false,
            highMemoryPolicy = false,
            minRamMb = 2048,
            minStorageMb = 250,
            upstreamReference = "https://huggingface.co/ggerganov/whisper.cpp",
            licenseName = "MIT"
        ),
        ModelDefinition(
            modelId = WHISPER_LARGE_V3_TURBO_ID,
            physicalFileName = "ggml-large-v3-turbo.bin",
            displayNameEn = "Whisper Large V3 Turbo",
            displayNameAr = "ويسبر الكبير V3 تيربو",
            engineType = EngineType.WHISPER_CPP,
            artifactFormat = ArtifactFormat.GGML_BIN,
            variant = "Large V3 Turbo",
            supportedLanguages = setOf("auto", "ar", "en", "fr", "de", "es", "tr", "ur", "ja", "ko", "zh"),
            arabicOnly = false,
            highMemoryPolicy = true,
            minRamMb = 6144,
            minStorageMb = 2048,
            upstreamReference = "https://huggingface.co/ggerganov/whisper.cpp",
            licenseName = "MIT"
        ),
        ModelDefinition(
            modelId = WHISPER_MEDIUM_ID,
            physicalFileName = "ggml-medium.bin",
            displayNameEn = "Whisper Medium",
            displayNameAr = "ويسبر المتوسط (Medium)",
            engineType = EngineType.WHISPER_CPP,
            artifactFormat = ArtifactFormat.GGML_BIN,
            variant = "Medium",
            supportedLanguages = setOf("auto", "ar", "en", "fr", "de", "es", "tr", "ur", "ja", "ko", "zh"),
            arabicOnly = false,
            highMemoryPolicy = true,
            minRamMb = 4096,
            minStorageMb = 1800,
            upstreamReference = "https://huggingface.co/ggerganov/whisper.cpp",
            licenseName = "MIT"
        ),
        ModelDefinition(
            modelId = WHISPER_SMALL_ID,
            physicalFileName = "ggml-small.bin",
            displayNameEn = "Whisper Small",
            displayNameAr = "ويسبر الصغير (Small)",
            engineType = EngineType.WHISPER_CPP,
            artifactFormat = ArtifactFormat.GGML_BIN,
            variant = "Small",
            supportedLanguages = setOf("auto", "ar", "en", "fr", "de", "es", "tr", "ur", "ja", "ko", "zh"),
            arabicOnly = false,
            highMemoryPolicy = false,
            minRamMb = 3072,
            minStorageMb = 650,
            upstreamReference = "https://huggingface.co/ggerganov/whisper.cpp",
            licenseName = "MIT"
        ),
        ModelDefinition(
            modelId = WHISPER_TINY_ID,
            physicalFileName = "ggml-tiny.bin",
            displayNameEn = "Whisper Tiny",
            displayNameAr = "ويسبر السريع (Tiny)",
            engineType = EngineType.WHISPER_CPP,
            artifactFormat = ArtifactFormat.GGML_BIN,
            variant = "Tiny",
            supportedLanguages = setOf("auto", "ar", "en", "fr", "de", "es", "tr", "ur", "ja", "ko", "zh"),
            arabicOnly = false,
            highMemoryPolicy = false,
            minRamMb = 1536,
            minStorageMb = 120,
            upstreamReference = "https://huggingface.co/ggerganov/whisper.cpp",
            licenseName = "MIT"
        ),
        ModelDefinition(
            modelId = MOONSHINE_BASE_AR_ID,
            physicalFileName = "moonshine-base-ar.zip",
            displayNameEn = "Moonshine Base Arabic",
            displayNameAr = "مونشاين العربي (Moonshine Arabic)",
            engineType = EngineType.SHERPA_MOONSHINE_AR,
            artifactFormat = ArtifactFormat.DISTRIBUTION_ZIP,
            variant = "Moonshine Arabic",
            supportedLanguages = setOf("ar"),
            arabicOnly = true,
            highMemoryPolicy = false,
            minRamMb = 2048,
            minStorageMb = 400,
            upstreamReference = "https://github.com/k2-fsa/sherpa-onnx",
            licenseName = "Apache-2.0 / MIT"
        ),
        ModelDefinition(
            modelId = SENSEVOICE_SMALL_INT8_ID,
            physicalFileName = "sensevoice-small-int8.zip",
            displayNameEn = "SenseVoice Small INT8",
            displayNameAr = "سينس فويس (SenseVoice INT8)",
            engineType = EngineType.SHERPA_SENSEVOICE,
            artifactFormat = ArtifactFormat.DISTRIBUTION_ZIP,
            variant = "SenseVoice Small INT8",
            supportedLanguages = setOf("zh", "yue", "en", "ja", "ko"),
            arabicOnly = false,
            highMemoryPolicy = false,
            minRamMb = 2048,
            minStorageMb = 400,
            upstreamReference = "https://github.com/k2-fsa/sherpa-onnx",
            licenseName = "Apache-2.0"
        )
    )

    private val byId = ALL.associateBy { it.modelId }
    private val byFileName = ALL.associateBy { it.physicalFileName }

    fun findById(modelId: String): ModelDefinition? = byId[modelId]

    fun findByExactFileName(fileName: String): ModelDefinition? = byFileName[fileName]

    fun isAllowedFileName(fileName: String): Boolean = byFileName.containsKey(fileName)
}
