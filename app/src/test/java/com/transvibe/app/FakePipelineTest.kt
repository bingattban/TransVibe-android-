package com.transvibe.app

import com.transvibe.app.asr.ModelProfiles
import com.transvibe.app.domain.AudioSegment
import com.transvibe.app.models.ModelCatalog
import com.transvibe.app.testing.FakeAsrEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakePipelineTest {

    @Test
    fun fakeAsrEngineProducesDeterministicTimedSegments() = runTest {
        val engine = FakeAsrEngine("Hello TransVibe")
        val tiny = ModelCatalog.findById(ModelCatalog.WHISPER_TINY_ID)!!
        val profile = ModelProfiles.forModel(tiny.modelId)

        assertTrue(engine.open(tiny, "/tmp/fake", profile).isSuccess)
        val seg = AudioSegment(0, 1_000_000L, 3_000_000L, pcmSamples = FloatArray(32_000))
        seg.use {
            val res = engine.transcribe(seg, "en").getOrThrow()
            assertEquals(1, res.segments.size)
            assertEquals("Hello TransVibe", res.segments[0].text)
            assertEquals(1_000_000L, res.segments[0].startUs)
            assertEquals(3_000_000L, res.segments[0].endUs)
        }
        engine.close()
    }
}
