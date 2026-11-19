package com.transvibe.app.subtitles

import com.transvibe.app.domain.SubtitleCue

/**
 * Tracks user cue edits separately from raw ASR and MT outputs with undo support.
 */
class SubtitleEditorRepository {
    private val undoStack = ArrayDeque<List<SubtitleCue>>()

    fun updateCueText(
        current: List<SubtitleCue>,
        cueId: String,
        newOriginal: String?,
        newTranslation: String?
    ): List<SubtitleCue> {
        undoStack.addLast(current)
        if (undoStack.size > 30) undoStack.removeFirst()
        return current.map { cue ->
            if (cue.cueId == cueId) {
                cue.copy(
                    userEditedOriginal = newOriginal ?: cue.userEditedOriginal,
                    userEditedTranslation = newTranslation ?: cue.userEditedTranslation,
                    revision = cue.revision + 1
                )
            } else cue
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun undo(): List<SubtitleCue>? = if (undoStack.isNotEmpty()) undoStack.removeLast() else null
}
