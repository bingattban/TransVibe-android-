package com.transvibe.app.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import com.transvibe.app.domain.AudioTrackInfo
import com.transvibe.app.domain.MediaInspectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class MediaInspector(private val context: Context) {

    suspend fun inspect(uri: Uri): Result<MediaInspectionResult> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val tracks = mutableListOf<AudioTrackInfo>()
            var maxDurationUs = 0L

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                val duration = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                    format.getLong(MediaFormat.KEY_DURATION)
                } else 0L
                if (duration > maxDurationUs) maxDurationUs = duration

                if (mime.startsWith("audio/")) {
                    val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    } else 48000
                    val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    } else 2
                    val lang = if (format.containsKey(MediaFormat.KEY_LANGUAGE)) {
                        format.getString(MediaFormat.KEY_LANGUAGE)
                    } else null
                    tracks.add(
                        AudioTrackInfo(
                            trackIndex = i,
                            mimeType = mime,
                            sampleRateHz = sampleRate,
                            channelCount = channels,
                            language = lang,
                            durationUs = duration
                        )
                    )
                }
            }

            if (tracks.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("NO_AUDIO_TRACK"))
            }

            val (name, size) = queryNameAndSize(uri)
            val fingerprint = computeContentFingerprint(uri, name, size, maxDurationUs)
            Result.success(
                MediaInspectionResult(
                    uriString = uri.toString(),
                    displayName = name,
                    durationUs = maxDurationUs,
                    audioTracks = tracks,
                    hasDrm = false,
                    contentFingerprint = fingerprint
                )
            )
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            extractor.release()
        }
    }

    private fun queryNameAndSize(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment ?: "video.mp4"
        var size = -1L
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        return name to size
    }

    private fun computeContentFingerprint(
        uri: Uri,
        name: String,
        size: Long,
        durationUs: Long
    ): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update("$name|$size|$durationUs".toByteArray())
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val head = ByteArray(64 * 1024)
                val read = input.read(head)
                if (read > 0) md.update(head, 0, read)
            }
        } catch (_: Throwable) {
            // Fallback to metadata if stream cannot be opened twice
        }
        return md.digest().joinToString("") { "%02x".format(it) }.take(24)
    }
}
