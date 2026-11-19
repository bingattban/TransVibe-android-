package com.transvibe.app.subtitles

import android.content.Context
import android.net.Uri
import com.transvibe.app.domain.SubtitleCue
import com.transvibe.app.domain.SubtitleExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale

class SrtVttExporter(private val context: Context) : SubtitleExporter {

    override fun formatSrt(cues: List<SubtitleCue>, bilingual: Boolean): String {
        val sb = StringBuilder()
        cues.forEachIndexed { index, cue ->
            sb.append(index + 1).append('\n')
            sb.append(formatTimestamp(cue.startUs, ',')).append(" --> ").append(formatTimestamp(cue.endUs, ',')).append('\n')
            val primary = cue.effectiveTranslation?.takeIf { it.isNotBlank() } ?: cue.effectiveOriginal
            if (bilingual && !cue.effectiveTranslation.isNullOrBlank()) {
                sb.append(cue.effectiveTranslation).append('\n')
                sb.append(cue.effectiveOriginal).append('\n')
            } else {
                sb.append(primary).append('\n')
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    override fun formatVtt(cues: List<SubtitleCue>, bilingual: Boolean): String {
        val sb = StringBuilder("WEBVTT\n\n")
        cues.forEachIndexed { index, cue ->
            sb.append(index + 1).append('\n')
            sb.append(formatTimestamp(cue.startUs, '.')).append(" --> ").append(formatTimestamp(cue.endUs, '.')).append('\n')
            val primary = cue.effectiveTranslation?.takeIf { it.isNotBlank() } ?: cue.effectiveOriginal
            if (bilingual && !cue.effectiveTranslation.isNullOrBlank()) {
                sb.append(cue.effectiveTranslation).append('\n')
                sb.append(cue.effectiveOriginal).append('\n')
            } else {
                sb.append(primary).append('\n')
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    override suspend fun exportToSaf(uri: Uri, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Prepare complete UTF-8 byte buffer before opening SAF output stream to prevent partial corruption
            val baos = ByteArrayOutputStream()
            OutputStreamWriter(baos, StandardCharsets.UTF_8).use { writer ->
                writer.write(content)
                writer.flush()
            }
            val bytes = baos.toByteArray()
            context.contentResolver.openOutputStream(uri, "wt")?.use { os ->
                os.write(bytes)
                os.flush()
            } ?: return@withContext Result.failure(IllegalStateException("Cannot open SAF output stream"))
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    companion object {
        fun formatTimestamp(timeUs: Long, msSeparator: Char): String {
            val totalMs = (timeUs / 1000L).coerceAtLeast(0L)
            val hours = totalMs / 3_600_000L
            val minutes = (totalMs % 3_600_000L) / 60_000L
            val seconds = (totalMs % 60_000L) / 1_000L
            val millis = totalMs % 1_000L
            return String.format(
                Locale.US,
                "%02d:%02d:%02d%c%03d",
                hours,
                minutes,
                seconds,
                msSeparator,
                millis
            )
        }
    }
}
