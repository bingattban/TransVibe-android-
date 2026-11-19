package com.transvibe.app.subtitles

import com.transvibe.app.domain.CueValidationWarning
import com.transvibe.app.domain.SubtitleCue

object HallucinationDetector {
    private val WHITESPACE_REGEX = "\\s+".toRegex()

    fun flagSuspicious(cues: List<SubtitleCue>): List<SubtitleCue> {
        return cues.map { cue ->
            val warnings = cue.warnings.toMutableSet()
            val words = cue.originalText.trim().split(WHITESPACE_REGEX).filter { it.isNotBlank() }
            if (words.size >= 6) {
                val unique = words.map { it.lowercase() }.toSet()
                if (unique.size <= 2) {
                    warnings.add(CueValidationWarning.SUSPECT_REPETITION)
                }
            }
            cue.copy(warnings = warnings)
        }
    }
}
