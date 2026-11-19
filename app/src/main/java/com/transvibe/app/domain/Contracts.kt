package com.transvibe.app.domain

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface AsrEngine : AutoCloseable {
    val engineId: String
    fun supportedCapabilities(model: ModelDefinition): Set<String>
    suspend fun open(model: ModelDefinition, installedPath: String, profile: ModelProfile): Result<Unit>
    suspend fun transcribe(segment: AudioSegment, languageHint: String?): Result<AsrResult>
    fun cancelActive()
}

interface AudioDecoder {
    suspend fun inspect(uri: Uri): Result<MediaInspectionResult>
    fun streamDecodedPcm(
        uri: Uri,
        trackIndex: Int,
        startFromUs: Long = 0L
    ): Flow<DecodedAudioFrame>
}

data class DecodedAudioFrame(
    val ptsUs: Long,
    val sampleRateHz: Int,
    val channelCount: Int,
    val pcm16: ShortArray
)

interface Resampler {
    fun resampleToMono16kFloat(
        pcm16: ShortArray,
        inputSampleRateHz: Int,
        inputChannels: Int
    ): FloatArray
}

interface VadEngine : AutoCloseable {
    suspend fun detectSpeechRegions(
        samples16k: FloatArray,
        baseOffsetUs: Long,
        profile: ModelProfile
    ): List<SpeechRegion>
}

data class SpeechRegion(
    val startUs: Long,
    val endUs: Long,
    val confidence: Float
)

interface TranslationEngine : AutoCloseable {
    suspend fun isPackInstalled(sourceLang: String, targetLang: String): Boolean
    suspend fun downloadPack(sourceLang: String, targetLang: String): Result<Unit>
    suspend fun translate(
        segmentId: String,
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<TranslationResult>
}

interface SubtitleExporter {
    fun formatSrt(cues: List<SubtitleCue>, bilingual: Boolean = false): String
    fun formatVtt(cues: List<SubtitleCue>, bilingual: Boolean = false): String
    suspend fun exportToSaf(uri: Uri, content: String): Result<Unit>
}

interface ModelVerifier {
    suspend fun verifyArtifact(model: ModelDefinition, filePath: String): Result<InstalledModelRecord>
    suspend fun computeSha256(filePath: String): String
}

interface ModelExporter {
    suspend fun exportToSaf(model: ModelDefinition, installedPath: String, targetUri: Uri): Result<Unit>
}

interface BenchmarkRecorder {
    suspend fun recordRun(run: BenchmarkRunRecord)
    suspend fun getRunsForModel(modelId: String): List<BenchmarkRunRecord>
}
