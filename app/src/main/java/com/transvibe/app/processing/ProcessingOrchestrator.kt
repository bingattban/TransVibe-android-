package com.transvibe.app.processing

import android.net.Uri
import com.transvibe.app.asr.AsrRouter
import com.transvibe.app.asr.ModelProfiles
import com.transvibe.app.diagnostics.JobTelemetry
import com.transvibe.app.domain.AudioDecoder
import com.transvibe.app.domain.AudioSegment
import com.transvibe.app.domain.CacheInvalidationPolicy
import com.transvibe.app.domain.JobState
import com.transvibe.app.domain.MemoryBudgetPolicy
import com.transvibe.app.domain.ModelDefinition
import com.transvibe.app.domain.ProcessingProgress
import com.transvibe.app.domain.Resampler
import com.transvibe.app.domain.RoutingPolicy
import com.transvibe.app.domain.SessionSnapshot
import com.transvibe.app.domain.SubtitleCue
import com.transvibe.app.domain.TranscriptSegment
import com.transvibe.app.domain.TranslationEngine
import com.transvibe.app.domain.VadEngine
import com.transvibe.app.media.AudioResampler
import com.transvibe.app.media.BoundaryReconciler
import com.transvibe.app.media.Timebase
import com.transvibe.app.models.ModelLifecycleManager
import com.transvibe.app.persistence.JobDao
import com.transvibe.app.persistence.JobEntity
import com.transvibe.app.persistence.TranscriptDao
import com.transvibe.app.persistence.TranscriptSegmentEntity
import com.transvibe.app.persistence.TranslationEntity
import com.transvibe.app.subtitles.HallucinationDetector
import com.transvibe.app.subtitles.SubtitleCueValidator
import com.transvibe.app.translation.TranslationPackManager
import com.transvibe.app.translation.TranslationPipeline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Shared memory-bounded streaming pipeline.
 * - Enforces MAX_QUEUED_PCM_BYTES backpressure and fixed FloatArray windowing.
 * - Slices audio using VAD speech regions before sending to ASR.
 * - Validates resume eligibility via CacheInvalidationPolicy.
 * - Uses 2-stage TranslationPipeline and refuses COMPLETED if translation fails.
 */
class ProcessingOrchestrator(
    private val decoder: AudioDecoder,
    private val resampler: Resampler,
    private val vadEngine: VadEngine,
    private val asrRouter: AsrRouter,
    private val translationEngine: TranslationEngine,
    private val translationPackManager: TranslationPackManager,
    private val modelLifecycle: ModelLifecycleManager,
    private val jobDao: JobDao,
    private val transcriptDao: TranscriptDao
) {
    private val _progress = MutableStateFlow<ProcessingProgress?>(null)
    val progress: StateFlow<ProcessingProgress?> = _progress.asStateFlow()

    suspend fun runJob(
        mediaUri: Uri,
        mediaTitle: String,
        trackIndex: Int,
        model: ModelDefinition,
        sourceLang: String,
        targetLang: String,
        existingJobId: String? = null
    ): Result<String> {
        val jobId = existingJobId ?: UUID.randomUUID().toString()
        val telemetry = JobTelemetry(jobId, model.modelId)
        val startedMs = System.currentTimeMillis()

        RoutingPolicy.resolveLanguageCompatibility(model, sourceLang, targetLang).getOrElse {
            return Result.failure(it)
        }

        val installed = modelLifecycle.records.value[model.modelId]
        val installedPath = installed?.installedPath
            ?: return Result.failure(IllegalStateException("MODEL_NOT_INSTALLED: ${model.physicalFileName}"))

        val profile = ModelProfiles.forModel(model.modelId)
        val asrEngine = asrRouter.createEngineFor(model)
        modelLifecycle.acquireLease(model.modelId)

        try {
            val inspection = decoder.inspect(mediaUri).getOrElse { return Result.failure(it) }

            val existing = jobDao.getJob(jobId)
            val resumeFromUs = if (existing != null) {
                val snap = SessionSnapshot(
                    jobId = existing.jobId,
                    mediaUriString = existing.mediaUriString,
                    mediaTitle = existing.mediaTitle,
                    mediaContentFingerprint = existing.mediaFingerprint,
                    selectedAudioTrackIndex = existing.selectedAudioTrackIndex,
                    selectedModelId = existing.selectedModelId,
                    actualEngineId = existing.actualEngineId,
                    actualModelId = existing.actualModelId,
                    modelSha256 = existing.modelSha256,
                    sourceLanguageSetting = existing.sourceLanguageSetting,
                    targetLanguageSetting = existing.targetLanguageSetting,
                    preprocessingVersion = existing.preprocessingVersion,
                    profileVersion = existing.profileVersion
                )
                val validResume = CacheInvalidationPolicy.isTranscriptCacheValid(
                    cached = snap,
                    currentMediaFingerprint = inspection.contentFingerprint,
                    currentTrackIndex = trackIndex,
                    currentModelId = model.modelId,
                    currentModelSha256 = installed.localSha256,
                    currentSourceLang = sourceLang,
                    currentPreprocessingVersion = 1,
                    currentProfileVersion = profile.profileVersion
                )
                if (validResume) existing.lastCheckpointUs else 0L
            } else 0L

            val now = System.currentTimeMillis()
            jobDao.upsertJob(
                JobEntity(
                    jobId = jobId,
                    mediaUriString = mediaUri.toString(),
                    mediaTitle = mediaTitle,
                    mediaFingerprint = inspection.contentFingerprint,
                    durationUs = inspection.durationUs,
                    selectedAudioTrackIndex = trackIndex,
                    selectedModelId = model.modelId,
                    actualEngineId = asrEngine.engineId,
                    actualModelId = model.modelId,
                    modelSha256 = installed.localSha256,
                    sourceLanguageSetting = sourceLang,
                    targetLanguageSetting = targetLang,
                    preprocessingVersion = 1,
                    profileVersion = profile.profileVersion,
                    state = JobState.DECODING.name,
                    lastCheckpointUs = resumeFromUs,
                    errorCode = null,
                    errorMessage = null,
                    createdAtMs = existing?.createdAtMs ?: now,
                    updatedAtMs = now
                )
            )

            asrEngine.open(model, installedPath, profile).getOrElse { err ->
                jobDao.updateProgress(jobId, JobState.FAILED.name, resumeFromUs, System.currentTimeMillis())
                return Result.failure(err)
            }

            if (resampler is AudioResampler) resampler.reset()

            val totalAudioMs = inspection.durationUs / 1000L
            val channel = Channel<AudioSegment>(capacity = MemoryBudgetPolicy.DEFAULT_CHANNEL_CAPACITY)
            val queuedBytes = AtomicInteger(0)

            coroutineScope {
                val producer = launch(Dispatchers.IO) {
                    val maxWindowSamples = 16_000 * 20
                    val windowBuf = FloatArray(maxWindowSamples)
                    var windowCount = 0
                    var windowStartUs = resumeFromUs
                    var chunkIdx = (resumeFromUs / 1_000_000L).toInt()

                    decoder.streamDecodedPcm(mediaUri, trackIndex, resumeFromUs).collect { frame ->
                        val resampled = resampler.resampleToMono16kFloat(
                            frame.pcm16,
                            frame.sampleRateHz,
                            frame.channelCount
                        )
                        var offset = 0
                        while (offset < resampled.size) {
                            if (windowCount == 0) {
                                windowStartUs = frame.ptsUs + Timebase.samplesToMicros(offset.toLong())
                            }
                            val copyCount = (maxWindowSamples - windowCount).coerceAtMost(resampled.size - offset)
                            System.arraycopy(resampled, offset, windowBuf, windowCount, copyCount)
                            windowCount += copyCount
                            offset += copyCount

                            if (windowCount >= maxWindowSamples) {
                                dispatchVadSlicedSegments(
                                    pcm = windowBuf.copyOf(windowCount),
                                    baseStartUs = windowStartUs,
                                    profile = profile,
                                    channel = channel,
                                    queuedBytes = queuedBytes,
                                    nextIndex = { chunkIdx++ }
                                )
                                windowCount = 0
                            }
                        }
                    }

                    if (windowCount > 0) {
                        dispatchVadSlicedSegments(
                            pcm = windowBuf.copyOf(windowCount),
                            baseStartUs = windowStartUs,
                            profile = profile,
                            channel = channel,
                            queuedBytes = queuedBytes,
                            nextIndex = { chunkIdx++ }
                        )
                    }
                    channel.close()
                }

                val consumer = launch(Dispatchers.Default) {
                    for (seg in channel) {
                        try {
                            _progress.value = ProcessingProgress(
                                jobId = jobId,
                                state = JobState.RECOGNIZING,
                                stageLabel = "ASR",
                                processedAudioMs = seg.endUs / 1000L,
                                totalAudioMs = totalAudioMs,
                                elapsedMs = System.currentTimeMillis() - startedMs,
                                actualEngineId = asrEngine.engineId,
                                actualModelId = model.modelId
                            )
                            val res = asrEngine.transcribe(seg, sourceLang).getOrThrow()
                            val entities = res.segments.mapIndexed { order, ts ->
                                TranscriptSegmentEntity(
                                    segmentId = "${jobId}_${seg.index}_$order",
                                    jobId = jobId,
                                    sourceIndex = seg.index,
                                    segmentOrder = order,
                                    startUs = ts.startUs,
                                    endUs = ts.endUs,
                                    originalText = ts.text,
                                    detectedLanguage = ts.language,
                                    engineId = ts.engineId,
                                    modelId = ts.modelId
                                )
                            }
                            transcriptDao.upsertSegments(entities)
                            jobDao.updateProgress(jobId, JobState.RECOGNIZING.name, seg.endUs, System.currentTimeMillis())
                        } finally {
                            seg.close()
                        }
                    }
                }

                producer.join()
                consumer.join()
            }

            // E05: Enforce translation success before marking COMPLETED
            if (targetLang == "ar") {
                _progress.value = ProcessingProgress(
                    jobId, JobState.TRANSLATING, "Translation",
                    totalAudioMs, totalAudioMs, System.currentTimeMillis() - startedMs,
                    asrEngine.engineId, model.modelId
                )
                val trRes = retranslateCachedTranscript(jobId, sourceLang, targetLang) { completed, total ->
                    _progress.value = ProcessingProgress(
                        jobId, JobState.TRANSLATING, "Translation $completed/$total",
                        totalAudioMs, totalAudioMs, System.currentTimeMillis() - startedMs,
                        asrEngine.engineId, model.modelId
                    )
                }
                if (trRes.isFailure) {
                    val error = trRes.exceptionOrNull() ?: IllegalStateException("Translation stage failed")
                    jobDao.updateProgress(jobId, JobState.FAILED.name, inspection.durationUs, System.currentTimeMillis())
                    _progress.value = ProcessingProgress(
                        jobId, JobState.FAILED, "Translation failed",
                        totalAudioMs, totalAudioMs, System.currentTimeMillis() - startedMs,
                        asrEngine.engineId, model.modelId,
                        warningMessage = error.message ?: "Translation failed"
                    )
                    return Result.failure(error)
                }
            }

            jobDao.updateProgress(jobId, JobState.COMPLETED.name, inspection.durationUs, System.currentTimeMillis())
            _progress.value = ProcessingProgress(
                jobId, JobState.COMPLETED, "Completed",
                totalAudioMs, totalAudioMs, System.currentTimeMillis() - startedMs,
                asrEngine.engineId, model.modelId
            )
            telemetry.markCompleted(System.currentTimeMillis() - startedMs)
            return Result.success(jobId)
        } catch (ce: CancellationException) {
            asrEngine.cancelActive()
            val cur = jobDao.getJob(jobId)
            jobDao.updateProgress(jobId, JobState.INTERRUPTED_RECOVERABLE.name, cur?.lastCheckpointUs ?: 0L, System.currentTimeMillis())
            _progress.value = ProcessingProgress(
                jobId, JobState.INTERRUPTED_RECOVERABLE, "Interrupted",
                (cur?.lastCheckpointUs ?: 0L) / 1000L, (cur?.durationUs ?: 0L) / 1000L,
                System.currentTimeMillis() - startedMs, asrEngine.engineId, model.modelId,
                warningMessage = "Processing cancelled. Recognized text is kept for recovery."
            )
            throw ce
        } catch (t: Throwable) {
            val cur = jobDao.getJob(jobId)
            jobDao.updateProgress(jobId, JobState.FAILED.name, cur?.lastCheckpointUs ?: 0L, System.currentTimeMillis())
            _progress.value = ProcessingProgress(
                jobId, JobState.FAILED, "Processing failed",
                (cur?.lastCheckpointUs ?: 0L) / 1000L, (cur?.durationUs ?: 0L) / 1000L,
                System.currentTimeMillis() - startedMs, asrEngine.engineId, model.modelId,
                warningMessage = t.message ?: "Unexpected processing failure"
            )
            return Result.failure(t)
        } finally {
            asrEngine.close()
            modelLifecycle.releaseLease(model.modelId)
        }
    }

    private suspend fun dispatchVadSlicedSegments(
        pcm: FloatArray,
        baseStartUs: Long,
        profile: com.transvibe.app.domain.ModelProfile,
        channel: Channel<AudioSegment>,
        queuedBytes: AtomicInteger,
        nextIndex: () -> Int
    ) {
        val regions = vadEngine.detectSpeechRegions(pcm, baseStartUs, profile)
        if (regions.isEmpty()) return
        for (region in regions) {
            val startSample = Timebase.microsToSamples(region.startUs - baseStartUs).toInt().coerceIn(0, pcm.size)
            val endSample = Timebase.microsToSamples(region.endUs - baseStartUs).toInt().coerceIn(startSample, pcm.size)
            if (endSample - startSample < 1600) continue
            val slice = pcm.copyOfRange(startSample, endSample)
            val byteSize = slice.size * 4
            while (queuedBytes.get() + byteSize > MemoryBudgetPolicy.MAX_QUEUED_PCM_BYTES) {
                delay(25L)
            }
            val seg = AudioSegment(
                index = nextIndex(),
                startUs = region.startUs,
                endUs = region.endUs,
                pcmSamples = slice,
                vadConfidence = region.confidence,
                onRelease = { queuedBytes.addAndGet(-byteSize) }
            )
            queuedBytes.addAndGet(byteSize)
            channel.send(seg)
        }
    }

    /**
     * E04 & E18: Explicit 2-stage translation (src -> en -> ar) reusing Stage 1 English intermediate
     * and never guessing 'en' when source language is unknown.
     */
    suspend fun retranslateCachedTranscript(
        jobId: String,
        sourceLangHint: String,
        targetLang: String = "ar",
        onSegmentDone: ((Int, Int) -> Unit)? = null
    ): Result<Int> {
        val entities = transcriptDao.getSegmentsForJob(jobId)
        if (entities.isEmpty()) {
            return Result.failure(IllegalStateException("No cached transcript segments for job $jobId"))
        }
        val existingMap = transcriptDao.getTranslationsForJob(jobId, targetLang).associateBy { it.segmentId }
        val pipeline = TranslationPipeline(translationEngine)
        val translations = mutableListOf<TranslationEntity>()
        val leasedPacks = mutableSetOf<String>()

        try {
            val translatableCount = entities.count { it.originalText.isNotBlank() }
            var translatedCount = 0
            onSegmentDone?.invoke(0, translatableCount)
            for (ent in entities) {
                if (ent.originalText.isBlank()) continue
                val resolvedSrc = ent.detectedLanguage?.takeIf { it != "unknown" && it != "auto" }
                    ?: sourceLangHint.takeIf { it != "auto" && it != "unknown" }

                // Hold leases BEFORE translation: user cannot remove a pack
                // while its ML Kit task is running on another dispatcher.
                val requiredPacks = com.transvibe.app.translation.TranslationRoutingPolicy
                    .planRoute(resolvedSrc, targetLang).requiredPacks
                for (pack in requiredPacks) {
                    if (leasedPacks.add(pack)) translationPackManager.acquirePackLease(pack)
                }
                val existing = existingMap[ent.segmentId]
                val outcome = pipeline.translateSegment(
                    segmentId = ent.segmentId,
                    originalText = ent.originalText,
                    sourceLang = resolvedSrc,
                    targetLang = targetLang,
                    existingEnglishIntermediate = existing?.englishIntermediateText
                )


                translations.add(
                    TranslationEntity(
                        segmentId = ent.segmentId,
                        jobId = jobId,
                        sourceLanguage = outcome.sourceLang,
                        targetLanguage = targetLang,
                        englishIntermediateText = outcome.englishIntermediate,
                        translatedText = outcome.finalArabic,
                        stage1Status = outcome.stage1Status.name,
                        stage2Status = outcome.stage2Status.name,
                        updatedAtMs = System.currentTimeMillis()
                    )
                )

                if (!outcome.isFullyComplete) {
                    if (translations.isNotEmpty()) transcriptDao.upsertTranslations(translations)
                    return Result.failure(IllegalStateException(outcome.errorMessage ?: "Translation failed"))
                }
                translatedCount++
                onSegmentDone?.invoke(translatedCount, translatableCount)
            }
            if (translations.isNotEmpty()) transcriptDao.upsertTranslations(translations)
            return Result.success(translations.size)
        } finally {
            leasedPacks.forEach { translationPackManager.releasePackLease(it) }
        }
    }

    suspend fun loadCuesForJob(jobId: String, targetLang: String = "ar"): List<SubtitleCue> {
        val job = jobDao.getJob(jobId) ?: return emptyList()
        val segs = transcriptDao.getSegmentsForJob(jobId)
        val trMap = transcriptDao.getTranslationsForJob(jobId, targetLang).associateBy { it.segmentId }
        val editMap = transcriptDao.getCueEditsForJob(jobId).associateBy { it.cueId }

        val mapped = segs.map { ent ->
            TranscriptSegment(
                segmentId = ent.segmentId,
                sourceIndex = ent.sourceIndex,
                startUs = ent.startUs,
                endUs = ent.endUs,
                text = ent.originalText,
                language = ent.detectedLanguage,
                engineId = ent.engineId,
                modelId = ent.modelId
            )
        }
        val reconciled = BoundaryReconciler.reconcile(mapped, job.durationUs).map { cue ->
            val segId = cue.sourceSegmentIds.firstOrNull()
            val trText = segId?.let { trMap[it]?.translatedText }
            val edit = editMap[cue.cueId]
            cue.copy(
                translatedText = trText,
                userEditedOriginal = edit?.editedOriginal,
                userEditedTranslation = edit?.editedTranslation,
                revision = edit?.revision ?: 1
            )
        }
        val flagged = HallucinationDetector.flagSuspicious(reconciled)
        return SubtitleCueValidator.validate(flagged, requireTranslation = targetLang == "ar")
    }
}
