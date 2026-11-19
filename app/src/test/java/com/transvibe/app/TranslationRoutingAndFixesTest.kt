package com.transvibe.app

import com.transvibe.app.domain.TranslationEngine
import com.transvibe.app.domain.TranslationResult
import com.transvibe.app.subtitles.HallucinationDetector
import com.transvibe.app.subtitles.SrtVttExporter
import com.transvibe.app.translation.ArabicTextNormalizer
import com.transvibe.app.translation.TranslationPipeline
import com.transvibe.app.translation.TranslationRouteKind
import com.transvibe.app.translation.TranslationRoutingPolicy
import com.transvibe.app.translation.TranslationStageStatus
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationRoutingAndFixesTest {

    @Test
    fun arabicNormalizerCollapsesWhitespace() {
        assertEquals("مرحبا بكم في التطبيق", ArabicTextNormalizer.normalize("  مرحبا   بكم\nفي   التطبيق  "))
    }

    @Test
    fun routingPolicyPlansDirectTwoStageAndRejectsUnknown() {
        assertEquals(TranslationRouteKind.DIRECT_EN_TO_AR, TranslationRoutingPolicy.planRoute("en", "ar").kind)
        assertEquals(TranslationRouteKind.TWO_STAGE_VIA_ENGLISH, TranslationRoutingPolicy.planRoute("fr", "ar").kind)
        assertEquals(TranslationRouteKind.PRESERVE_ARABIC_TRANSCRIPT, TranslationRoutingPolicy.planRoute("ar", "ar").kind)
        assertEquals(TranslationRouteKind.UNSUPPORTED, TranslationRoutingPolicy.planRoute("unknown", "ar").kind)
        assertEquals(TranslationRouteKind.UNSUPPORTED, TranslationRoutingPolicy.planRoute("auto", "ar").kind)
    }

    @Test
    fun twoStagePipelinePreservesEnglishIntermediateOnStage2Failure() = runTest {
        val fakeEngine = object : TranslationEngine {
            override suspend fun isPackInstalled(sourceLang: String, targetLang: String) = true
            override suspend fun downloadPack(sourceLang: String, targetLang: String) = Result.success(Unit)
            override suspend fun translate(segmentId: String, text: String, sourceLang: String, targetLang: String): Result<TranslationResult> {
                return if (sourceLang == "fr" && targetLang == "en") {
                    Result.success(TranslationResult(segmentId, "Hello world", "fr", "en", 5L))
                } else {
                    Result.failure(IllegalStateException("en->ar pack missing"))
                }
            }
            override fun close() {}
        }

        val pipeline = TranslationPipeline(fakeEngine)
        val outcome = pipeline.translateSegment("seg1", "Bonjour le monde", "fr", "ar")
        assertFalse(outcome.isFullyComplete)
        assertEquals(TranslationStageStatus.COMPLETED, outcome.stage1Status)
        assertEquals(TranslationStageStatus.FAILED, outcome.stage2Status)
        assertEquals("Hello world", outcome.englishIntermediate)
    }

    @Test
    fun missingOfflinePackFailsWithoutCallingTranslation() = runTest {
        var calls = 0
        val fake = object : TranslationEngine {
            override suspend fun isPackInstalled(sourceLang: String, targetLang: String) = false
            override suspend fun downloadPack(sourceLang: String, targetLang: String) =
                Result.failure<Unit>(IllegalStateException("offline"))
            override suspend fun translate(
                segmentId: String, text: String, sourceLang: String, targetLang: String
            ): Result<TranslationResult> {
                calls++
                return Result.failure(IllegalStateException("must not be called"))
            }
            override fun close() {}
        }
        val outcome = TranslationPipeline(fake).translateSegment("one", "Hello", "en", "ar")
        assertFalse(outcome.isFullyComplete)
        assertTrue(outcome.errorMessage!!.contains("TRANSLATION_PACK_MISSING"))
        assertEquals(0, calls)
    }

    @Test
    fun blockedTranslationEndsWithExplicitTimeoutInsteadOfInfiniteSpinner() = runTest {
        val fake = object : TranslationEngine {
            override suspend fun isPackInstalled(sourceLang: String, targetLang: String) = true
            override suspend fun downloadPack(sourceLang: String, targetLang: String) = Result.success(Unit)
            override suspend fun translate(
                segmentId: String, text: String, sourceLang: String, targetLang: String
            ): Result<TranslationResult> {
                delay(300_000L)
                return Result.success(TranslationResult(segmentId, "مرحبا", sourceLang, targetLang, 1L))
            }
            override fun close() {}
        }
        val outcome = TranslationPipeline(fake).translateSegment("one", "Hello", "en", "ar")
        assertFalse(outcome.isFullyComplete)
        assertNotNull(outcome.errorMessage)
        assertTrue(outcome.errorMessage!!.contains("TRANSLATION_TIMEOUT"))
    }

    @Test
    fun hallucinationDetectorAndSrtFormattingWorkWithoutSyntaxErrors() {
        val us = 1_000_000L
        assertEquals("00:00:01,000", SrtVttExporter.formatTimestamp(us, ','))
        assertTrue(HallucinationDetector.flagSuspicious(emptyList()).isEmpty())
    }
}
