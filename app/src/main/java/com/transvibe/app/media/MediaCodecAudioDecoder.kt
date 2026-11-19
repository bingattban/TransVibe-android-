package com.transvibe.app.media

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import com.transvibe.app.domain.AudioDecoder
import com.transvibe.app.domain.DecodedAudioFrame
import com.transvibe.app.domain.MediaInspectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.nio.ByteOrder

/**
 * Audio-only streaming MediaExtractor + MediaCodec decoder.
 * Verifies actual MediaCodec PCM encoding (16-bit vs 32-bit float) and never buffers entire video in RAM.
 */
class MediaCodecAudioDecoder(
    private val context: Context,
    private val inspector: MediaInspector
) : AudioDecoder {

    override suspend fun inspect(uri: Uri): Result<MediaInspectionResult> = inspector.inspect(uri)

    override fun streamDecodedPcm(
        uri: Uri,
        trackIndex: Int,
        startFromUs: Long
    ): Flow<DecodedAudioFrame> = flow {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            extractor.selectTrack(trackIndex)
            if (startFromUs > 0L) {
                extractor.seekTo(startFromUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalStateException("Missing audio MIME type")

            var sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 48000
            var channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone && currentCoroutineContext().isActive) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val inputBuf = codec.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(inputBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            val ptsUs = extractor.sampleTime.coerceAtLeast(0L)
                            codec.queueInputBuffer(inIdx, 0, sampleSize, ptsUs, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFmt = codec.outputFormat
                        if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channelCount = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && outFmt.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            pcmEncoding = outFmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                    }
                    outIdx >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 0) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            val shorts: ShortArray = if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                                val floatBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                                val arr = ShortArray(floatBuf.remaining())
                                for (i in arr.indices) {
                                    arr[i] = (floatBuf.get().coerceIn(-1.0f, 1.0f) * 32767.0f).toInt().toShort()
                                }
                                arr
                            } else {
                                val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                                ShortArray(shortBuf.remaining()).also { shortBuf.get(it) }
                            }
                            emit(
                                DecodedAudioFrame(
                                    ptsUs = bufferInfo.presentationTimeUs.coerceAtLeast(0L),
                                    sampleRateHz = sampleRate,
                                    channelCount = channelCount,
                                    pcm16 = shorts
                                )
                            )
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }
        } finally {
            try { codec?.stop() } catch (_: Throwable) {}
            try { codec?.release() } catch (_: Throwable) {}
            extractor.release()
        }
    }.flowOn(Dispatchers.IO)
}
