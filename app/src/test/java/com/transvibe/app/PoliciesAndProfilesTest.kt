package com.transvibe.app

import com.transvibe.app.asr.ModelProfiles
import com.transvibe.app.diagnostics.TinyBenchmarkRunner
import com.transvibe.app.domain.BenchmarkRunRecord
import com.transvibe.app.domain.CacheInvalidationPolicy
import com.transvibe.app.domain.SessionSnapshot
import com.transvibe.app.models.ModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliciesAndProfilesTest {

    @Test
    fun allSevenModelsHaveIndependentProfiles() {
        val profiles = ModelCatalog.ALL.map { ModelProfiles.forModel(it.modelId) }
        assertEquals(7, profiles.size)
        assertNotEquals(ModelProfiles.WHISPER_TINY.modelId, ModelProfiles.WHISPER_BASE.modelId)
    }

    @Test
    fun cacheInvalidatesWhenModelOrTrackChanges() {
        val snap = SessionSnapshot(
            jobId = "j1",
            mediaUriString = "content://video/1",
            mediaTitle = "clip.mp4",
            mediaContentFingerprint = "fp1",
            selectedAudioTrackIndex = 0,
            selectedModelId = "whisper-tiny",
            actualEngineId = "whisper_cpp_jni",
            actualModelId = "whisper-tiny",
            modelSha256 = "abc",
            sourceLanguageSetting = "en",
            targetLanguageSetting = "ar",
            preprocessingVersion = 1,
            profileVersion = 1
        )
        assertTrue(
            CacheInvalidationPolicy.isTranscriptCacheValid(
                snap, "fp1", 0, "whisper-tiny", "abc", "en", 1, 1
            )
        )
        assertFalse(
            CacheInvalidationPolicy.isTranscriptCacheValid(
                snap, "fp1", 1, "whisper-tiny", "abc", "en", 1, 1
            )
        )
        assertFalse(
            CacheInvalidationPolicy.isTranscriptCacheValid(
                snap, "fp1", 0, "whisper-base", "abc", "en", 1, 1
            )
        )
    }

    @Test
    fun tinyBenchmarkScheduleInterleavesAndSummarizesMedian() {
        val sched = TinyBenchmarkRunner.buildInterleavedSchedule(listOf(2, 3, 4), runsPerCandidate = 3)
        assertEquals(listOf(2, 3, 4, 4, 3, 2, 2, 3, 4), sched)

        val runs = listOf(
            BenchmarkRunRecord("1", "whisper-tiny", 2, 0, "fp", "sha", 100, 120, 1000, 0, "h", "", 1),
            BenchmarkRunRecord("2", "whisper-tiny", 2, 1, "fp", "sha", 300, 320, 1200, 0, "h", "", 2),
            BenchmarkRunRecord("3", "whisper-tiny", 2, 2, "fp", "sha", 200, 220, 1100, 0, "h", "", 3)
        )
        val summary = TinyBenchmarkRunner.summarize(runs)
        assertEquals(1, summary.size)
        assertEquals(200L, summary[0].medianInferenceMs)
        assertEquals(1200L, summary[0].maxPeakPssKb)
    }
}
