package com.transvibe.app.translation

import com.transvibe.app.domain.TranslationEngine

data class StagedTranslationOutcome(
    val segmentId: String,
    val sourceLang: String,
    val englishIntermediate: String?,
    val finalArabic: String?,
    val stage1Status: TranslationStageStatus,
    val stage2Status: TranslationStageStatus,
    val errorMessage: String? = null
) {
    val isFullyComplete: Boolean
        get() = (stage1Status == TranslationStageStatus.COMPLETED || stage1Status == TranslationStageStatus.NOT_REQUIRED) &&
            (stage2Status == TranslationStageStatus.COMPLETED || stage2Status == TranslationStageStatus.NOT_REQUIRED)
}

/**
 * Executes explicit 1-stage (en -> ar) or 2-stage (src -> en -> ar) translation.
 * Preserves Stage 1 English intermediate text if Stage 2 fails so retries do not repeat Stage 1 or ASR.
 */
class TranslationPipeline(private val engine: TranslationEngine) {

    suspend fun translateSegment(
        segmentId: String,
        originalText: String,
        sourceLang: String?,
        targetLang: String = "ar",
        existingEnglishIntermediate: String? = null
    ): StagedTranslationOutcome {
        val plan = TranslationRoutingPolicy.planRoute(sourceLang, targetLang)
        return when (plan.kind) {
            TranslationRouteKind.UNSUPPORTED -> StagedTranslationOutcome(
                segmentId = segmentId,
                sourceLang = plan.sourceLang,
                englishIntermediate = existingEnglishIntermediate,
                finalArabic = null,
                stage1Status = TranslationStageStatus.FAILED,
                stage2Status = TranslationStageStatus.PENDING,
                errorMessage = "UNSUPPORTED_LANGUAGE_ROUTE: Cannot translate from '${plan.sourceLang}'"
            )

            TranslationRouteKind.PRESERVE_ARABIC_TRANSCRIPT -> StagedTranslationOutcome(
                segmentId = segmentId,
                sourceLang = "ar",
                englishIntermediate = null,
                finalArabic = originalText,
                stage1Status = TranslationStageStatus.NOT_REQUIRED,
                stage2Status = TranslationStageStatus.NOT_REQUIRED
            )

            TranslationRouteKind.DIRECT_EN_TO_AR -> {
                val res = engine.translate(segmentId, originalText, "en", "ar")
                res.fold(
                    onSuccess = { tr ->
                        StagedTranslationOutcome(
                            segmentId = segmentId,
                            sourceLang = "en",
                            englishIntermediate = originalText,
                            finalArabic = tr.translatedText,
                            stage1Status = TranslationStageStatus.NOT_REQUIRED,
                            stage2Status = TranslationStageStatus.COMPLETED
                        )
                    },
                    onFailure = { err ->
                        StagedTranslationOutcome(
                            segmentId = segmentId,
                            sourceLang = "en",
                            englishIntermediate = originalText,
                            finalArabic = null,
                            stage1Status = TranslationStageStatus.NOT_REQUIRED,
                            stage2Status = TranslationStageStatus.FAILED,
                            errorMessage = err.message
                        )
                    }
                )
            }

            TranslationRouteKind.TWO_STAGE_VIA_ENGLISH -> {
                val englishText = if (!existingEnglishIntermediate.isNullOrBlank()) {
                    existingEnglishIntermediate
                } else {
                    val stage1 = engine.translate("${segmentId}_s1", originalText, plan.sourceLang, "en")
                    stage1.getOrElse { err ->
                        return StagedTranslationOutcome(
                            segmentId = segmentId,
                            sourceLang = plan.sourceLang,
                            englishIntermediate = null,
                            finalArabic = null,
                            stage1Status = TranslationStageStatus.FAILED,
                            stage2Status = TranslationStageStatus.PENDING,
                            errorMessage = "Stage 1 (${plan.sourceLang}->en) failed: ${err.message}"
                        )
                    }.translatedText
                }

                val stage2 = engine.translate("${segmentId}_s2", englishText, "en", "ar")
                stage2.fold(
                    onSuccess = { tr ->
                        StagedTranslationOutcome(
                            segmentId = segmentId,
                            sourceLang = plan.sourceLang,
                            englishIntermediate = englishText,
                            finalArabic = tr.translatedText,
                            stage1Status = TranslationStageStatus.COMPLETED,
                            stage2Status = TranslationStageStatus.COMPLETED
                        )
                    },
                    onFailure = { err ->
                        StagedTranslationOutcome(
                            segmentId = segmentId,
                            sourceLang = plan.sourceLang,
                            englishIntermediate = englishText,
                            finalArabic = null,
                            stage1Status = TranslationStageStatus.COMPLETED,
                            stage2Status = TranslationStageStatus.FAILED,
                            errorMessage = "Stage 2 (en->ar) failed: ${err.message}"
                        )
                    }
                )
            }
        }
    }
}
