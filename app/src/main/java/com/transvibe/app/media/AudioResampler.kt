package com.transvibe.app.media

import com.transvibe.app.domain.Resampler

/**
 * Stateful streaming downmixer and resampler to 16 kHz mono Float32 PCM.
 * Retains fractional phase across consecutive buffer boundaries to avoid clicks or timing drift.
 */
class AudioResampler : Resampler {
    private var lastMonoSample: Float = 0.0f
    private var hasPreviousSample: Boolean = false
    private var fractionalPhase: Double = 0.0

    fun reset() {
        lastMonoSample = 0.0f
        hasPreviousSample = false
        fractionalPhase = 0.0
    }

    override fun resampleToMono16kFloat(
        pcm16: ShortArray,
        inputSampleRateHz: Int,
        inputChannels: Int
    ): FloatArray {
        if (pcm16.isEmpty() || inputSampleRateHz <= 0 || inputChannels <= 0) {
            return FloatArray(0)
        }
        val frameCount = pcm16.size / inputChannels
        if (frameCount <= 0) return FloatArray(0)

        val mono = FloatArray(frameCount)
        val scale = 1.0f / 32768.0f
        if (inputChannels == 1) {
            for (i in 0 until frameCount) {
                mono[i] = (pcm16[i] * scale).coerceIn(-1.0f, 1.0f)
            }
        } else {
            val invChannels = 1.0f / inputChannels.toFloat()
            var leftEnergy = 0.0
            var rightEnergy = 0.0
            var monoEnergy = 0.0
            for (i in 0 until frameCount) {
                var sum = 0.0f
                val base = i * inputChannels
                for (c in 0 until inputChannels) {
                    sum += pcm16[base + c] * scale
                }
                val mixed = (sum * invChannels).coerceIn(-1.0f, 1.0f)
                mono[i] = mixed
                if (inputChannels == 2) {
                    val left = pcm16[base] * scale
                    val right = pcm16[base + 1] * scale
                    leftEnergy += left.toDouble() * left
                    rightEnergy += right.toDouble() * right
                    monoEnergy += mixed.toDouble() * mixed
                }
            }
            // If stereo channels are close to opposite-phase, averaging them
            // silently erases actual speech. Choose the louder channel only in
            // this narrow cancellation case; preserve normal stereo mixing.
            val dominantEnergy = maxOf(leftEnergy, rightEnergy)
            val chooseChannel = if (inputChannels == 2 &&
                dominantEnergy > 1e-9 && monoEnergy < dominantEnergy * 0.04
            ) {
                if (leftEnergy >= rightEnergy) 0 else 1
            } else -1
            if (chooseChannel >= 0) {
                for (i in 0 until frameCount) {
                    mono[i] = (pcm16[i * 2 + chooseChannel] * scale).coerceIn(-1f, 1f)
                }
            }
        }

        if (inputSampleRateHz == 16_000) {
            lastMonoSample = mono.last()
            hasPreviousSample = true
            return mono
        }

        val step = inputSampleRateHz.toDouble() / 16_000.0
        val outList = ArrayList<Float>((frameCount / step).toInt() + 2)
        var pos = fractionalPhase

        while (pos < frameCount.toDouble()) {
            val idx0 = pos.toInt()
            val frac = (pos - idx0).toFloat()
            val s0 = if (idx0 == 0 && hasPreviousSample && pos < 0.0) lastMonoSample else mono[idx0.coerceIn(0, frameCount - 1)]
            val s1 = mono[(idx0 + 1).coerceIn(0, frameCount - 1)]
            outList.add(s0 + (s1 - s0) * frac)
            pos += step
        }

        fractionalPhase = pos - frameCount.toDouble()
        lastMonoSample = mono.last()
        hasPreviousSample = true
        return outList.toFloatArray()
    }
}
