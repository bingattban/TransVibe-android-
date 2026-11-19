package com.transvibe.app.subtitles

import com.transvibe.app.domain.CueValidationWarning
import com.transvibe.app.domain.SubtitleCue

object SubtitleCueValidator {
    private const val MAX_LINE_CHARS = 48
    private const val MAX_CHARS_PER_SECOND = 25.0

    fun validate(cues: List<SubtitleCue>, requireTranslation: Boolean): List<SubtitleCue> {
        return cues.mapIndexed { idx, cue ->
            val warnings = cue.warnings.toMutableSet()
            if (cue.endUs <= cue.startUs) {
                warnings.add(CueValidationWarning.NEGATIVE_OR_ZERO_DURATION)
            }
            if (idx > 0 && cue.startUs < cues[idx - 1].endUs) {
                warnings.add(CueValidationWarning.OVERLAP)
            }
            val text = cue.effectiveTranslation ?: cue.effectiveOriginal
            if (text.isBlank()) {
                warnings.add(CueValidationWarning.EMPTY_TEXT)
            }
            if (requireTranslation && cue.effectiveTranslation.isNullOrBlank()) {
                warnings.add(CueValidationWarning.MISSING_TRANSLATION)
            }
            if (text.length > MAX_LINE_CHARS * 2) {
                warnings.add(CueValidationWarning.TOO_LONG_LINE)
            }
            val durSec = (cue.endUs - cue.startUs).coerceAtLeast(100_000L) / 1_000_000.0
            if (text.length / durSec > MAX_CHARS_PER_SECOND) {
                warnings.add(CueValidationWarning.HIGH_READING_SPEED)
            }
            cue.copy(warnings = warnings)
        }
    }
}
