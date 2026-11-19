package com.transvibe.app.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val jobId: String,
    val mediaUriString: String,
    val mediaTitle: String,
    val mediaFingerprint: String,
    val durationUs: Long,
    val selectedAudioTrackIndex: Int,
    val selectedModelId: String,
    val actualEngineId: String,
    val actualModelId: String,
    val modelSha256: String?,
    val sourceLanguageSetting: String,
    val targetLanguageSetting: String,
    val preprocessingVersion: Int,
    val profileVersion: Int,
    val state: String,
    val lastCheckpointUs: Long,
    val errorCode: String?,
    val errorMessage: String?,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

@Entity(
    tableName = "transcript_segments",
    indices = [Index(value = ["jobId", "sourceIndex", "segmentOrder"], unique = true)]
)
data class TranscriptSegmentEntity(
    @PrimaryKey val segmentId: String,
    val jobId: String,
    val sourceIndex: Int,
    val segmentOrder: Int,
    val startUs: Long,
    val endUs: Long,
    val originalText: String,
    val detectedLanguage: String?,
    val engineId: String,
    val modelId: String
)

@Entity(
    tableName = "translations",
    primaryKeys = ["segmentId", "targetLanguage"]
)
data class TranslationEntity(
    val segmentId: String,
    val jobId: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val englishIntermediateText: String?,
    val translatedText: String?,
    val stage1Status: String,
    val stage2Status: String,
    val updatedAtMs: Long
)

@Entity(tableName = "cue_edits")
data class CueEditEntity(
    @PrimaryKey val cueId: String,
    val jobId: String,
    val editedOriginal: String?,
    val editedTranslation: String?,
    val revision: Int,
    val updatedAtMs: Long
)

@Entity(tableName = "benchmark_runs")
data class BenchmarkRunEntity(
    @PrimaryKey val runId: String,
    val modelId: String,
    val threads: Int,
    val runOrderIndex: Int,
    val mediaFingerprint: String,
    val modelSha256: String,
    val inferenceTimeMs: Long,
    val endToEndTimeMs: Long,
    val peakPssKb: Long,
    val thermalStatus: Int,
    val outputHash: String,
    val transcriptPreview: String,
    val timestampMs: Long
)

@Entity(tableName = "freeze_records")
data class FreezeRecordEntity(
    @PrimaryKey val modelId: String,
    val status: String,
    val profileVersion: Int,
    val approvedByOwner: Boolean,
    val ownerSignoffTimestampMs: Long?,
    val notes: String
)
