package com.transvibe.app.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.transvibe.app.domain.TranslationEngine
import com.transvibe.app.domain.TranslationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * On-device ML Kit Translation adapter.
 * Offline after pack download; never fabricates unsupported language routes.
 */
class MlKitTranslationEngine : TranslationEngine {
    private val mutex = Mutex()
    private var activeTranslator: Translator? = null
    private var activePair: Pair<String, String>? = null

    override suspend fun isPackInstalled(sourceLang: String, targetLang: String): Boolean =
        withContext(Dispatchers.IO) {
            val src = mapCode(sourceLang) ?: return@withContext false
            val dst = mapCode(targetLang) ?: return@withContext false
            if (src == dst) return@withContext true
            val manager = RemoteModelManager.getInstance()
            val downloaded = manager.getDownloadedModels(TranslateRemoteModel::class.java).await()
            val langs = downloaded.map { it.language }.toSet()
            langs.contains(src) && langs.contains(dst)
        }

    override suspend fun downloadPack(sourceLang: String, targetLang: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val src = mapCode(sourceLang)
                ?: return@withContext Result.failure(IllegalArgumentException("Unsupported source language: $sourceLang"))
            val dst = mapCode(targetLang)
                ?: return@withContext Result.failure(IllegalArgumentException("Unsupported target language: $targetLang"))
            try {
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(src)
                    .setTargetLanguage(dst)
                    .build()
                val translator = Translation.getClient(options)
                translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                translator.close()
                Result.success(Unit)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    override suspend fun translate(
        segmentId: String,
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<TranslationResult> = withContext(Dispatchers.Default) {
        if (sourceLang == targetLang) {
            return@withContext Result.success(
                TranslationResult(segmentId, text, sourceLang, targetLang, 0L)
            )
        }
        val src = mapCode(sourceLang)
            ?: return@withContext Result.failure(IllegalArgumentException("UNSUPPORTED_LANGUAGE_ROUTE: $sourceLang"))
        val dst = mapCode(targetLang)
            ?: return@withContext Result.failure(IllegalArgumentException("UNSUPPORTED_LANGUAGE_ROUTE: $targetLang"))

        mutex.withLock {
            val startMs = System.currentTimeMillis()
            try {
                val pair = src to dst
                if (activePair != pair || activeTranslator == null) {
                    activeTranslator?.close()
                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(src)
                        .setTargetLanguage(dst)
                        .build()
                    activeTranslator = Translation.getClient(options)
                    activePair = pair
                }
                val translated = activeTranslator!!.translate(text).await()
                val normalized = if (targetLang.equals("ar", ignoreCase = true)) {
                    ArabicTextNormalizer.normalize(translated)
                } else {
                    translated.trim()
                }
                Result.success(
                    TranslationResult(
                        segmentId = segmentId,
                        translatedText = normalized,
                        sourceLanguage = sourceLang,
                        targetLanguage = targetLang,
                        durationMs = System.currentTimeMillis() - startMs
                    )
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
    }

    override fun close() {
        activeTranslator?.close()
        activeTranslator = null
        activePair = null
    }

    companion object {
        fun mapCode(code: String): String? = when (code.lowercase()) {
            "ar" -> TranslateLanguage.ARABIC
            "en" -> TranslateLanguage.ENGLISH
            "fr" -> TranslateLanguage.FRENCH
            "de" -> TranslateLanguage.GERMAN
            "es" -> TranslateLanguage.SPANISH
            "tr" -> TranslateLanguage.TURKISH
            "ur" -> TranslateLanguage.URDU
            "ja" -> TranslateLanguage.JAPANESE
            "ko" -> TranslateLanguage.KOREAN
            "zh", "yue" -> TranslateLanguage.CHINESE
            else -> null
        }
    }
}

object ArabicTextNormalizer {
    private val WHITESPACE_REGEX = "\\s+".toRegex()

    fun normalize(input: String): String {
        return input
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }
}
