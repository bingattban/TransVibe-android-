package com.transvibe.app.media

import java.util.concurrent.ArrayBlockingQueue

/**
 * Bounded reusable FloatArray pool to reduce GC churn during streaming PCM processing.
 */
class AudioBufferPool(
    private val bufferSize: Int,
    maxPooled: Int = 4
) {
    private val pool = ArrayBlockingQueue<FloatArray>(maxPooled)

    fun acquire(): FloatArray = pool.poll() ?: FloatArray(bufferSize)

    fun release(buffer: FloatArray) {
        if (buffer.size == bufferSize) {
            pool.offer(buffer)
        }
    }
}
