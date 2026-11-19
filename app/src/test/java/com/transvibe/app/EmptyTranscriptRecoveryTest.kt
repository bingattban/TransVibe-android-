package com.transvibe.app

import com.transvibe.app.processing.EmptyTranscriptRecovery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmptyTranscriptRecoveryTest {
    @Test fun `real silence is never sent to Whisper by fallback`() {
        assertFalse(EmptyTranscriptRecovery.isAudibleSpeechCandidate(FloatArray(16_000)))
    }

    @Test fun `very short audio is not treated as speech`() {
        assertFalse(EmptyTranscriptRecovery.isAudibleSpeechCandidate(FloatArray(1_000) { 0.025f }))
    }

    @Test fun `quiet speech-like audio lost by strict VAD is retried once per window`() {
        val pcm = FloatArray(16_000) { i -> if (i % 4 < 2) 0.007f else -0.007f }
        assertTrue(EmptyTranscriptRecovery.isAudibleSpeechCandidate(pcm))
    }

    @Test fun `NaNs and invalid values cannot cause false audible detection`() {
        assertFalse(EmptyTranscriptRecovery.isAudibleSpeechCandidate(FloatArray(16_000) { Float.NaN }))
    }

    @Test fun `different absence causes are not misreported as translation errors`() {
        assertEquals("AUDIO_NO_PCM", EmptyTranscriptRecovery.classify(0, 0, 0, 0))
        assertEquals("VAD_NO_SPEECH", EmptyTranscriptRecovery.classify(20, 0, 0, 0))
        assertEquals("ASR_NO_TRANSCRIPT", EmptyTranscriptRecovery.classify(20, 2, 2, 0))
        assertEquals("TRANSCRIPT_AVAILABLE", EmptyTranscriptRecovery.classify(20, 2, 2, 1))
    }
}
