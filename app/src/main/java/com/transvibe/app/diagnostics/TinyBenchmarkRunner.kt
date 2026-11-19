package com.transvibe.app.diagnostics

import com.transvibe.app.domain.BenchmarkRecorder
import com.transvibe.app.domain.BenchmarkRunRecord
import kotlin.math.ceil

data class ThreadBenchmarkSummary(
    val threads: Int,
    val runCount: Int,
    val medianInferenceMs: Long,
    val p90InferenceMs: Long,
    val medianEndToEndMs: Long,
    val maxPeakPssKb: Long
)

/**
 * Deterministic Debug/Preview-only Whisper Tiny benchmark helper.
 * Interleaves candidate thread counts (2, 3, 4) to reduce thermal/order bias.
 */
object TinyBenchmarkRunner {

    fun buildInterleavedSchedule(
        candidates: List<Int> = listOf(2, 3, 4),
        runsPerCandidate: Int = 3
    ): List<Int> {
        val schedule = mutableListOf<Int>()
        for (round in 0 until runsPerCandidate) {
            val ordered = if (round % 2 == 0) candidates else candidates.reversed()
            schedule.addAll(ordered)
        }
        return schedule
    }

    fun summarize(runs: List<BenchmarkRunRecord>): List<ThreadBenchmarkSummary> {
        return runs.groupBy { it.threads }
            .toSortedMap()
            .map { (threads, list) ->
                val infSorted = list.map { it.inferenceTimeMs }.sorted()
                val e2eSorted = list.map { it.endToEndTimeMs }.sorted()
                ThreadBenchmarkSummary(
                    threads = threads,
                    runCount = list.size,
                    medianInferenceMs = percentile(infSorted, 0.50),
                    p90InferenceMs = percentile(infSorted, 0.90),
                    medianEndToEndMs = percentile(e2eSorted, 0.50),
                    maxPeakPssKb = list.maxOfOrNull { it.peakPssKb } ?: 0L
                )
            }
    }

    private fun percentile(sorted: List<Long>, p: Double): Long {
        if (sorted.isEmpty()) return 0L
        val idx = (ceil(p * sorted.size).toInt() - 1).coerceIn(0, sorted.lastIndex)
        return sorted[idx]
    }
}

class InMemoryBenchmarkRecorder : BenchmarkRecorder {
    private val runs = mutableListOf<BenchmarkRunRecord>()
    override suspend fun recordRun(run: BenchmarkRunRecord) {
        runs.add(run)
    }
    override suspend fun getRunsForModel(modelId: String): List<BenchmarkRunRecord> =
        runs.filter { it.modelId == modelId }
}
