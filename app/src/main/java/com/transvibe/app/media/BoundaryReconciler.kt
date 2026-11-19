package com.transvibe.app.media

import com.transvibe.app.domain.SubtitleCue
import com.transvibe.app.domain.TranscriptSegment

/**
 * Merges overlap-aware ASR segments into monotonic, non-overlapping cues
 * without erasing genuine repeated speech.
 */
object BoundaryReconciler {

    fun reconcile(segments: List<TranscriptSegment>, totalDurationUs: Long): List<SubtitleCue> {
        if (segments.isEmpty()) return emptyList()
        val sorted = segments
            .filter { it.text.isNotBlank() }
            .sortedWith(compareBy<TranscriptSegment> { it.startUs }.thenBy { it.endUs })

        val cues = mutableListOf<SubtitleCue>()
        for ((idx, seg) in sorted.withIndex()) {
            var startUs = Timebase.clampToDuration(seg.startUs, totalDurationUs)
            val endUs = Timebase.clampToDuration(seg.endUs, totalDurationUs)
            if (endUs <= startUs) continue

            val prev = cues.lastOrNull()
            if (prev != null && startUs < prev.endUs) {
                // Only deduplicate if text is identical AND within overlap boundary window (<= 1500ms)
                val overlapUs = prev.endUs - startUs
                if (overlapUs <= 1_500_000L && prev.originalText.trim() == seg.text.trim()) {
                    continue
                }
                startUs = prev.endUs
                if (endUs <= startUs) continue
            }

            cues.add(
                SubtitleCue(
                    cueId = "cue_${seg.segmentId}_$idx",
                    sourceSegmentIds = listOf(seg.segmentId),
                    startUs = startUs,
                    endUs = endUs,
                    originalText = seg.text.trim(),
                    translatedText = null,
                    modelId = seg.modelId,
                    engineId = seg.engineId
                )
            )
        }
        return cues
    }
}
