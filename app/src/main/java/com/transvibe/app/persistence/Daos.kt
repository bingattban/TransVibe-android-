package com.transvibe.app.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY updatedAtMs DESC")
    fun observeAllJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE jobId = :jobId LIMIT 1")
    suspend fun getJob(jobId: String): JobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJob(job: JobEntity)

    @Query("UPDATE jobs SET state = :state, lastCheckpointUs = :checkpointUs, updatedAtMs = :updatedAtMs WHERE jobId = :jobId")
    suspend fun updateProgress(jobId: String, state: String, checkpointUs: Long, updatedAtMs: Long)

    @Query("DELETE FROM jobs WHERE jobId = :jobId")
    suspend fun deleteJob(jobId: String)
}

@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcript_segments WHERE jobId = :jobId ORDER BY startUs ASC, segmentOrder ASC")
    suspend fun getSegmentsForJob(jobId: String): List<TranscriptSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSegments(segments: List<TranscriptSegmentEntity>)

    @Query("SELECT * FROM translations WHERE jobId = :jobId AND targetLanguage = :targetLang")
    suspend fun getTranslationsForJob(jobId: String, targetLang: String): List<TranslationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTranslations(translations: List<TranslationEntity>)

    @Query("SELECT * FROM cue_edits WHERE jobId = :jobId")
    suspend fun getCueEditsForJob(jobId: String): List<CueEditEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCueEdit(edit: CueEditEntity)
}

@Dao
interface BenchmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: BenchmarkRunEntity)

    @Query("SELECT * FROM benchmark_runs WHERE modelId = :modelId ORDER BY timestampMs DESC")
    suspend fun getRunsForModel(modelId: String): List<BenchmarkRunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFreezeRecord(record: FreezeRecordEntity)

    @Query("SELECT * FROM freeze_records")
    suspend fun getAllFreezeRecords(): List<FreezeRecordEntity>
}
