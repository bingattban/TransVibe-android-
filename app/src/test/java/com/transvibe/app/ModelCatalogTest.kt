package com.transvibe.app

import com.transvibe.app.domain.RoutingPolicy
import com.transvibe.app.models.ModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun catalogContainsExactlySevenApprovedArtifacts() {
        assertEquals(7, ModelCatalog.ALL.size)
        val expectedFiles = setOf(
            "ggml-base.bin",
            "ggml-large-v3-turbo.bin",
            "ggml-medium.bin",
            "ggml-small.bin",
            "ggml-tiny.bin",
            "moonshine-base-ar.zip",
            "sensevoice-small-int8.zip"
        )
        assertEquals(expectedFiles, ModelCatalog.ALL.map { it.physicalFileName }.toSet())
    }

    @Test
    fun rejectsEighthOrQuantizedSubstituteArtifacts() {
        assertFalse(ModelCatalog.isAllowedFileName("ggml-small-q5_1.bin"))
        assertFalse(ModelCatalog.isAllowedFileName("whisper-base.en.bin"))
        assertFalse(ModelCatalog.isAllowedFileName("custom-model.bin"))
        assertTrue(ModelCatalog.isAllowedFileName("ggml-tiny.bin"))
    }

    @Test
    fun moonshineArabicRejectsNonArabicSource() {
        val moonshine = ModelCatalog.findById(ModelCatalog.MOONSHINE_BASE_AR_ID)!!
        assertTrue(moonshine.arabicOnly)
        assertTrue(RoutingPolicy.resolveLanguageCompatibility(moonshine, "ar", "ar").isSuccess)
        assertTrue(RoutingPolicy.resolveLanguageCompatibility(moonshine, "en", "ar").isFailure)
    }

    @Test
    fun arabicTranscriptOnlyStillRequiresModelArabicSupport() {
        val senseVoice = ModelCatalog.findById(ModelCatalog.SENSEVOICE_SMALL_INT8_ID)!!
        val whisperTiny = ModelCatalog.findById(ModelCatalog.WHISPER_TINY_ID)!!
        // Changing translation target never grants an ASR language to an engine.
        assertTrue(RoutingPolicy.resolveLanguageCompatibility(senseVoice, "ar", "ar").isFailure)
        assertTrue(RoutingPolicy.resolveLanguageCompatibility(senseVoice, "ar", "en").isFailure)
        assertTrue(RoutingPolicy.resolveLanguageCompatibility(whisperTiny, "ar", "ar").isSuccess)
    }

    @Test
    fun senseVoiceDoesNotInferArabicAsrSupport() {
        val sv = ModelCatalog.findById(ModelCatalog.SENSEVOICE_SMALL_INT8_ID)!!
        assertFalse(sv.supportedLanguages.contains("ar"))
        assertTrue(RoutingPolicy.resolveLanguageCompatibility(sv, "en", "ar").isSuccess)
        assertTrue(RoutingPolicy.resolveLanguageCompatibility(sv, "ar", "ar").isFailure)
    }
}
