package com.transvibe.app.media

/**
 * Single integer-microsecond timebase to eliminate floating-point drift.
 */
object Timebase {
    const val TARGET_SAMPLE_RATE_HZ = 16_000L
    const val MICROS_PER_SECOND = 1_000_000L

    fun samplesToMicros(sampleCount: Long, sampleRateHz: Long = TARGET_SAMPLE_RATE_HZ): Long {
        if (sampleRateHz <= 0L) return 0L
        return (sampleCount * MICROS_PER_SECOND) / sampleRateHz
    }

    fun microsToSamples(micros: Long, sampleRateHz: Long = TARGET_SAMPLE_RATE_HZ): Long {
        if (micros <= 0L) return 0L
        return (micros * sampleRateHz) / MICROS_PER_SECOND
    }

    fun clampToDuration(timeUs: Long, totalDurationUs: Long): Long {
        return timeUs.coerceIn(0L, totalDurationUs.coerceAtLeast(0L))
    }
}
