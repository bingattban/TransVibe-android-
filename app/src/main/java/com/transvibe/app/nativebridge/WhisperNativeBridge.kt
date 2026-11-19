package com.transvibe.app.nativebridge

data class NativeWhisperSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val detectedLanguage: String
)

object WhisperNativeBridge {
    private val libraryLoaded: Boolean = try {
        System.loadLibrary("transvibe_whisper")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    fun isAvailable(): Boolean {
        if (!libraryLoaded) return false
        return try {
            nativeIsLinked()
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    external fun nativeIsLinked(): Boolean
    external fun nativeInit(modelPath: String): Long
    external fun nativeCancel(handle: Long)
    external fun nativeTranscribe(
        handle: Long,
        pcmData: FloatArray,
        numSamples: Int,
        languageHint: String?,
        threads: Int,
        greedyOnly: Boolean,
        beamSize: Int
    ): Array<NativeWhisperSegment>
    external fun nativeFree(handle: Long)
}
