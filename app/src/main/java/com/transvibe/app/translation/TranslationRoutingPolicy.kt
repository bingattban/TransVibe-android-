package com.transvibe.app.translation

enum class TranslationRouteKind {
    DIRECT_EN_TO_AR,
    TWO_STAGE_VIA_ENGLISH,
    PRESERVE_ARABIC_TRANSCRIPT,
    UNSUPPORTED
}

enum class TranslationStageStatus {
    NOT_REQUIRED,
    PENDING,
    COMPLETED,
    FAILED
}

data class TranslationRoutePlan(
    val sourceLang: String,
    val targetLang: String,
    val kind: TranslationRouteKind,
    val requiredPacks: List<String>
)

/**
 * Centralized translation routing policy:
 * - en -> ar: 1-stage direct ML Kit translation
 * - ar -> ar: preserves original Arabic transcript without round-trip distortion
 * - non-en (e.g. fr, de, es, tr, ur, ja, ko, zh) -> en -> ar: explicit 2-stage pipeline
 * - unknown / auto / unsupported: fails explicitly rather than guessing English
 */
object TranslationRoutingPolicy {

    fun planRoute(sourceLang: String?, targetLang: String = "ar"): TranslationRoutePlan {
        val src = sourceLang?.trim()?.lowercase()
        if (src.isNullOrBlank() || src == "auto" || src == "unknown") {
            return TranslationRoutePlan("unknown", targetLang, TranslationRouteKind.UNSUPPORTED, emptyList())
        }
        if (MlKitTranslationEngine.mapCode(src) == null) {
            return TranslationRoutePlan(src, targetLang, TranslationRouteKind.UNSUPPORTED, emptyList())
        }
        if (src == "ar" && targetLang == "ar") {
            return TranslationRoutePlan("ar", "ar", TranslationRouteKind.PRESERVE_ARABIC_TRANSCRIPT, emptyList())
        }
        if (src == "en" && targetLang == "ar") {
            return TranslationRoutePlan("en", "ar", TranslationRouteKind.DIRECT_EN_TO_AR, listOf("en", "ar"))
        }
        if (targetLang == "ar") {
            return TranslationRoutePlan(src, "ar", TranslationRouteKind.TWO_STAGE_VIA_ENGLISH, listOf(src, "en", "ar"))
        }
        return TranslationRoutePlan(src, targetLang, TranslationRouteKind.UNSUPPORTED, emptyList())
    }
}
