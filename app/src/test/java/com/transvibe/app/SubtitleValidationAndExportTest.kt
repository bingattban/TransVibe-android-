package com.transvibe.app

import com.transvibe.app.domain.CueValidationWarning
import com.transvibe.app.domain.SubtitleCue
import com.transvibe.app.subtitles.HallucinationDetector
import com.transvibe.app.subtitles.SubtitleCueValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleValidationAndExportTest {

    @Test
    fun detectsOverlapsMissingTranslationAndSuspectRepetition() {
        val cues = listOf(
            SubtitleCue(
                cueId = "c1",
                sourceSegmentIds = listOf("s1"),
                startUs = 0L,
                endUs = 2_000_000L,
                originalText = "test test test test test test",
                translatedText = null,
                modelId = "whisper-tiny",
                engineId = "whisper_cpp_jni"
            ),
            SubtitleCue(
                cueId = "c2",
                sourceSegmentIds = listOf("s2"),
                startUs = 1_500_000L,
                endUs = 3_000_000L,
                originalText = "Valid line",
                translatedText = "سطر صالح",
                modelId = "whisper-tiny",
                engineId = "whisper_cpp_jni"
            )
        )
        val flagged = HallucinationDetector.flagSuspicious(cues)
        val validated = SubtitleCueValidator.validate(flagged, requireTranslation = true)

        assertTrue(validated[0].warnings.contains(CueValidationWarning.SUSPECT_REPETITION))
        assertTrue(validated[0].warnings.contains(CueValidationWarning.MISSING_TRANSLATION))
        assertTrue(validated[1].warnings.contains(CueValidationWarning.OVERLAP))
    }
}
