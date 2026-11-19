package com.transvibe.app.media

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Signal-level diagnostics ONLY, never speech text or raw PCM.
 * The 16 kHz mono samples are already normalized Float32 in [-1, 1].
 */
object PcmSignalMetrics {
    data class Stats(
        val valid: Boolean,
        val samples: Int,
        val rms: Float,
        val peak: Float,
        val rmsDbfs: Double
    )

    fun analyze(samples: FloatArray): Stats {
        if (samples.isEmpty()) return Stats(true, 0, 0f, 0f, Double.NEGATIVE_INFINITY)
        var sumSquares = 0.0
        var peak = 0f
        for (sample in samples) {
            if (!sample.isFinite()) {
                return Stats(false, samples.size, 0f, 0f, Double.NaN)
            }
            val absSample = abs(sample)
            if (absSample > peak) peak = absSample
            sumSquares += sample.toDouble() * sample.toDouble()
        }
        val rms = sqrt(sumSquares / samples.size).toFloat()
        val dbfs = if (rms == 0f) Double.NEGATIVE_INFINITY else 20.0 * log10(rms.toDouble())
        return Stats(true, samples.size, rms, peak, dbfs)
    }
}
