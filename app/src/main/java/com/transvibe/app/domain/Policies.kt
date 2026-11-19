package com.transvibe.app.domain

object MemoryBudgetPolicy {
    const val DEFAULT_CHANNEL_CAPACITY = 2
    const val MAX_QUEUED_PCM_BYTES = 8 * 1024 * 1024 // 8 MiB hard bound
    const val HIGH_MEMORY_MODEL_THRESHOLD_MB = 4096
}

object CacheInvalidationPolicy {
    fun isTranscriptCacheValid(
        cached: SessionSnapshot,
        currentMediaFingerprint: String,
        currentTrackIndex: Int,
        currentModelId: String,
        currentModelSha256: String?,
        currentSourceLang: String,
        currentPreprocessingVersion: Int,
        currentProfileVersion: Int
    ): Boolean {
        return cached.mediaContentFingerprint == currentMediaFingerprint &&
            cached.selectedAudioTrackIndex == currentTrackIndex &&
            cached.actualModelId == currentModelId &&
            cached.modelSha256 == currentModelSha256 &&
            cached.sourceLanguageSetting == currentSourceLang &&
            cached.preprocessingVersion == currentPreprocessingVersion &&
            cached.profileVersion == currentProfileVersion
    }
}

object RoutingPolicy {
    fun resolveLanguageCompatibility(
        model: ModelDefinition,
        requestedSourceLang: String,
        targetLang: String
    ): Result<Unit> {
        if (model.arabicOnly && requestedSourceLang != "auto" && requestedSourceLang != "ar") {
            return Result.failure(
                IllegalArgumentException("${model.displayNameEn} supports Arabic speech only.")
            )
        }
        if (targetLang == "ar" && requestedSourceLang == "ar") {
            // Arabic speech -> Arabic transcript-only route is always valid
            return Result.success(Unit)
        }
        if (requestedSourceLang != "auto" &&
            model.supportedLanguages.isNotEmpty() &&
            !model.supportedLanguages.contains(requestedSourceLang)
        ) {
            return Result.failure(
                IllegalArgumentException("Language $requestedSourceLang is not in verified allowlist for ${model.modelId}")
            )
        }
        return Result.success(Unit)
    }
}
