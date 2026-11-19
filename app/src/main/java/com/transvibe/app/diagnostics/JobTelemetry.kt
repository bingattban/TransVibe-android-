package com.transvibe.app.diagnostics

import android.os.Debug

/**
 * Privacy-safe per-job structured telemetry.
 * Stage timings may overlap; never sums stages and claims it is wall-clock time.
 */
class JobTelemetry(
    val jobId: String,
    val modelId: String
) {
    var wallTimeMs: Long = 0L
        private set
    val stageDurationsMs = mutableMapOf<String, Long>()

    fun recordStage(stage: String, durationMs: Long) {
        stageDurationsMs[stage] = durationMs
    }

    fun markCompleted(totalWallMs: Long) {
        wallTimeMs = totalWallMs
    }

    fun samplePssKb(): Long = Debug.getPss()
}
