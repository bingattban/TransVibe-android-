package com.transvibe.app

import com.transvibe.app.media.AudioResampler
import com.transvibe.app.media.PcmSignalMetrics
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSignalIntegrityTest {
    @Test fun silenceMeasuresNegativeInfinityWithoutInventingSpeech() {
        val signal = PcmSignalMetrics.analyze(FloatArray(16_000))
        assertTrue(signal.valid)
        assertEquals(0f, signal.peak, 0f)
        assertEquals(0f, signal.rms, 0f)
        assertTrue(signal.rmsDbfs == Double.NEGATIVE_INFINITY)
    }

    @Test fun normalizedAudioGivesFinitePrivacySafeLevels() {
        val signal = PcmSignalMetrics.analyze(floatArrayOf(0.1f, -0.1f, 0.1f, -0.1f))
        assertTrue(signal.valid)
        assertEquals(0.1f, signal.rms, 0.00001f)
        assertEquals(0.1f, signal.peak, 0.00001f)
        assertEquals(-20.0, signal.rmsDbfs, 0.01)
    }

    @Test fun invalidSamplesAreFlaggedRatherThanMasked() {
        val signal = PcmSignalMetrics.analyze(floatArrayOf(Float.NaN, 0.4f))
        assertFalse(signal.valid)
    }

    @Test fun oppositePhaseStereoDoesNotDestroySpeechSignal() {
        val samples = ShortArray(32_000) { i -> if (i % 2 == 0) 10_000 else -10_000 }
        val mono = AudioResampler().resampleToMono16kFloat(samples, 16_000, 2)
        assertEquals(16_000, mono.size)
        assertTrue("Opposite-phase stereo must not become digital silence",
            mono.any { abs(it) > 0.2f })
    }

    @Test fun ordinaryStereoMixRetainsCurrentBehavior() {
        val samples = ShortArray(16_000) { 8_000 }
        val mono = AudioResampler().resampleToMono16kFloat(samples, 16_000, 2)
        assertEquals(8_000, mono.size)
        assertEquals(8_000f / 32_768f, mono[0], 0.00001f)
    }
}
