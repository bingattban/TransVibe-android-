package com.transvibe.app.translation

import com.transvibe.app.domain.TranslationEngine
import com.transvibe.app.domain.TranslationResult

/**
 * Sequential/bounded cue translator preserving order and failing cleanly when offline pack is missing.
 */
class TranslationQueue(private val engine: TranslationEngine) {
    suspend fun translateAllOrdered(
        items: List<Pair<String, String>>,
        sourceLang: String,
        targetLang: String
    ): List<Result<TranslationResult>> {
        val results = mutableListOf<Result<TranslationResult>>()
        for ((segId, text) in items) {
            if (text.isBlank()) {
                results.add(Result.success(TranslationResult(segId, "", sourceLang, targetLang, 0L)))
            } else {
                results.add(engine.translate(segId, text, sourceLang, targetLang))
            }
        }
        return results
    }
}
