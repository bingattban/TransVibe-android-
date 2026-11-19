package com.transvibe.app.processing

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Guard for zero-transcript jobs. A quiet but sustained PCM signal may have been
 * rejected by the fixed VAD threshold; make ONE Whisper attempt on that bounded
 * window. Never use the fallback for silence, NaNs or short noise spikes.
 *
 * This is an admission heuristic, not proof that speech was recognized. ASR
 * results are still required before translation or COMPLETED state.
 */
object EmptyTranscriptRecovery {
    private const val SAMPLE_RATE = 16_000
    private const val FRAME_SAMPLES = 480 // 30 ms
    private const val MIN_FRAMES = 8 // 240 ms sustained audible audio
    private const val FRAME_RMS_THRESHOLD = 0.0035f
    private const val PEAK_THRESHOLD = 0.006f

    fun isAudibleSpeechCandidate(pcm: FloatArray): Boolean {
        if (pcm.size < SAMPLE_RATE / 2) return false
        var activeFrames = 0
        var peak = 0.0f
        var position = 0
        while (position < pcm.size) {
            val end = (position + FRAME_SAMPLES).coerceAtMost(pcm.size)
            var squares = 0.0
            for (i in position until end) {
                val value = pcm[i]
                if (!value.isFinite()) return false
                peak = maxOf(peak, abs(value))
                squares += (value * value).toDouble()
            }
            val rms = sqrt(squares / (end - position)).toFloat()
            if (rms >= FRAME_RMS_THRESHOLD) activeFrames++
            position = end
        }
        return peak >= PEAK_THRESHOLD && activeFrames >= MIN_FRAMES
    }

    fun classify(decodedFrames: Int, candidateSegments: Int, asrCalls: Int, storedSegments: Int): String {
        if (storedSegments > 0) return "TRANSCRIPT_AVAILABLE"
        if (decodedFrames == 0) return "AUDIO_NO_PCM"
        if (candidateSegments == 0 && asrCalls == 0) return "VAD_NO_SPEECH"
        return "ASR_NO_TRANSCRIPT"
    }
}
