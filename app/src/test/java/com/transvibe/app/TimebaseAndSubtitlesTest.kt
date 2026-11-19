package com.transvibe.app

import com.transvibe.app.domain.SubtitleCue
import com.transvibe.app.domain.TranscriptSegment
import com.transvibe.app.media.BoundaryReconciler
import com.transvibe.app.media.Timebase
import com.transvibe.app.subtitles.SrtVttExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimebaseAndSubtitlesTest {

    @Test
    fun formatsSrtAndVttTimestampsAccurately() {
        val us = 3_723_456_000L // 1h 2m 3s 456ms
        assertEquals("01:02:03,456", SrtVttExporter.formatTimestamp(us, ','))
        assertEquals("01:02:03.456", SrtVttExporter.formatTimestamp(us, '.'))
    }

    @Test
    fun timebaseConvertsSamplesToMicrosWithoutFloatDrift() {
        assertEquals(1_000_000L, Timebase.samplesToMicros(16_000L, 16_000L))
        assertEquals(250_000L, Timebase.samplesToMicros(4_000L, 16_000L))
    }

    @Test
    fun boundaryReconcilerDeduplicatesOverlapButPreservesGenuineRepetition() {
        val segs = listOf(
            TranscriptSegment("s1", 0, 0L, 2_000_000L, "Hello world", "en", null, "w", "whisper-tiny"),
            TranscriptSegment("s2", 1, 1_500_000L, 3_500_000L, "Hello world", "en", null, "w", "whisper-tiny"),
            TranscriptSegment("s3", 2, 5_000_000L, 7_000_000L, "Hello world", "en", null, "w", "whisper-tiny")
        )
        val cues: List<SubtitleCue> = BoundaryReconciler.reconcile(segs, 10_000_000L)
        assertEquals(2, cues.size)
        assertTrue(cues[1].startUs >= cues[0].endUs)
    }
}
