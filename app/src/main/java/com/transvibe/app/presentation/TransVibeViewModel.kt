package com.transvibe.app.presentation

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.transvibe.app.AppContainer
import com.transvibe.app.asr.ModelProfiles
import com.transvibe.app.asr.WhisperEngine
import com.transvibe.app.diagnostics.DiagnosticExporter
import com.transvibe.app.diagnostics.ThreadBenchmarkSummary
import com.transvibe.app.diagnostics.TinyBenchmarkRunner
import com.transvibe.app.domain.AudioSegment
import com.transvibe.app.domain.BenchmarkRunRecord
import com.transvibe.app.domain.FreezeStatus
import com.transvibe.app.domain.InstalledModelRecord
import com.transvibe.app.domain.MediaInspectionResult
import com.transvibe.app.domain.ModelDefinition
import com.transvibe.app.domain.ModelStatus
import com.transvibe.app.domain.ProcessingProgress
import com.transvibe.app.domain.SubtitleCue
import com.transvibe.app.media.FfmpegFallbackBridge
import com.transvibe.app.models.ModelCatalog
import com.transvibe.app.persistence.CueEditEntity
import com.transvibe.app.persistence.FreezeRecordEntity
import com.transvibe.app.persistence.JobEntity
import com.transvibe.app.platform.DeviceMemoryPreflight
import com.transvibe.app.platform.SafStorageHelper
import com.transvibe.app.platform.TransVibeProcessingService
import com.transvibe.app.translation.TranslationPackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class TransVibeViewModel(private val container: AppContainer) : ViewModel() {

    val preferences = container.preferences.preferences
    val modelRecords: StateFlow<Map<String, InstalledModelRecord>> = container.modelLifecycle.records
    val translationPacks: StateFlow<List<TranslationPackInfo>> = container.translationPackManager.packs
    val jobs: Flow<List<JobEntity>> = container.database.jobDao().observeAllJobs()
    val processingProgress: StateFlow<ProcessingProgress?> = container.orchestrator.progress

    private val _selectedModel = MutableStateFlow(ModelCatalog.findById(ModelCatalog.WHISPER_TINY_ID)!!)
    val selectedModel: StateFlow<ModelDefinition> = _selectedModel.asStateFlow()

    private val _inspectedMedia = MutableStateFlow<MediaInspectionResult?>(null)
    val inspectedMedia: StateFlow<MediaInspectionResult?> = _inspectedMedia.asStateFlow()

    private val _selectedTrackIndex = MutableStateFlow(0)
    val selectedTrackIndex: StateFlow<Int> = _selectedTrackIndex.asStateFlow()

    private val _activeJobId = MutableStateFlow<String?>(null)
    val activeJobId: StateFlow<String?> = _activeJobId.asStateFlow()

    private val _activeJobEntity = MutableStateFlow<JobEntity?>(null)
    val activeJobEntity: StateFlow<JobEntity?> = _activeJobEntity.asStateFlow()

    private val _activeCues = MutableStateFlow<List<SubtitleCue>>(emptyList())
    val activeCues: StateFlow<List<SubtitleCue>> = _activeCues.asStateFlow()

    private val _recoveryError = MutableStateFlow<String?>(null)
    val recoveryError: StateFlow<String?> = _recoveryError.asStateFlow()

    // Freeze decisions are persisted in Room and observed by the benchmark UI.
    private val _freezeRecords = MutableStateFlow<Map<String, FreezeRecordEntity>>(emptyMap())
    val freezeRecords: StateFlow<Map<String, FreezeRecordEntity>> = _freezeRecords.asStateFlow()

    private val _benchmarkSummaries = MutableStateFlow<List<ThreadBenchmarkSummary>>(emptyList())
    val benchmarkSummaries: StateFlow<List<ThreadBenchmarkSummary>> = _benchmarkSummaries.asStateFlow()

    private val _modelTransferProgress = MutableStateFlow<String?>(null)
    val modelTransferProgress: StateFlow<String?> = _modelTransferProgress.asStateFlow()

    private val _cacheSizeMb = MutableStateFlow(0L)
    val cacheSizeMb: StateFlow<Long> = _cacheSizeMb.asStateFlow()

    private var modelTransferJob: Job? = null

    init {
        loadFreezeRecords()
        refreshTranslationPacks()
    }

    fun setUiLanguage(lang: String) {
        viewModelScope.launch { container.preferences.setUiLanguage(lang) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { container.preferences.setOnboardingCompleted(true) }
    }

    fun selectModel(model: ModelDefinition) {
        _selectedModel.value = model
    }

    fun inspectSelectedVideo(app: Application, uri: Uri) {
        SafStorageHelper.takePersistableReadPermission(app, uri)
        viewModelScope.launch {
            container.mediaInspector.inspect(uri).onSuccess { result ->
                _inspectedMedia.value = result
                _selectedTrackIndex.value = result.audioTracks.firstOrNull()?.trackIndex ?: 0
            }.onFailure { err ->
                _recoveryError.value = err.message ?: "Media inspection failed"
            }
        }
    }

    fun selectAudioTrack(trackIndex: Int) {
        _selectedTrackIndex.value = trackIndex
    }

    fun importModelFromSaf(uri: Uri, model: ModelDefinition) {
        modelTransferJob?.cancel()
        modelTransferJob = viewModelScope.launch {
            _modelTransferProgress.value = "Importing ${model.physicalFileName}..."
            container.modelLifecycle.importFromSaf(uri, model) { copied, _ ->
                val mb = copied / (1024 * 1024)
                _modelTransferProgress.value = "${model.physicalFileName}: $mb MB"
            }.onSuccess {
                _modelTransferProgress.value = null
            }.onFailure { err ->
                _modelTransferProgress.value = null
                _recoveryError.value = err.message ?: "Model import failed"
            }
        }
    }

    fun downloadModelFromHttps(url: String, model: ModelDefinition) {
        modelTransferJob?.cancel()
        modelTransferJob = viewModelScope.launch {
            _modelTransferProgress.value = "Downloading ${model.physicalFileName}..."
            container.modelLifecycle.downloadModelHttps(url, model) { downloaded, total ->
                val mb = downloaded / (1024 * 1024)
                val totalStr = if (total > 0) " / ${total / (1024 * 1024)} MB" else " MB"
                _modelTransferProgress.value = "${model.physicalFileName}: $mb$totalStr"
            }.onSuccess {
                _modelTransferProgress.value = null
            }.onFailure { err ->
                _modelTransferProgress.value = null
                _recoveryError.value = err.message ?: "Model download failed"
            }
        }
    }

    fun cancelModelTransfer() {
        modelTransferJob?.cancel()
        _modelTransferProgress.value = null
    }

    fun verifyModel(model: ModelDefinition) {
        val path = modelRecords.value[model.modelId]?.installedPath ?: return
        viewModelScope.launch {
            container.modelLifecycle.verifyArtifact(model, path).onFailure { err ->
                _recoveryError.value = err.message
            }
        }
    }

    fun exportModelToSaf(model: ModelDefinition, targetUri: Uri) {
        val path = modelRecords.value[model.modelId]?.installedPath ?: return
        viewModelScope.launch {
            container.modelLifecycle.exportToSaf(model, path, targetUri).onFailure { err ->
                _recoveryError.value = err.message
            }
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            container.modelLifecycle.deleteModel(modelId).onFailure { err ->
                _recoveryError.value = err.message
            }
        }
    }

    fun refreshTranslationPacks() {
        viewModelScope.launch { container.translationPackManager.refreshInventory() }
    }

    fun downloadTranslationPack(code: String) {
        viewModelScope.launch {
            container.translationPackManager.downloadPack(code).onFailure { err ->
                _recoveryError.value = err.message ?: "Pack download failed"
            }
        }
    }

    fun deleteTranslationPack(code: String) {
        viewModelScope.launch {
            container.translationPackManager.deletePack(code).onFailure { err ->
                _recoveryError.value = err.message ?: "Pack delete failed"
            }
        }
    }

    fun retryPendingTranslationPacks() {
        container.translationPackManager.retryPending()
    }

    // E10 & E17: Delegates execution to TransVibeProcessingService; navigates once
    fun startProcessing(
        app: Application,
        sourceLang: String,
        targetLang: String,
        resumeJobId: String? = null
    ): Boolean {
        val preflight = DeviceMemoryPreflight.check(app, _selectedModel.value)
        if (!preflight.compatible) {
            _recoveryError.value = preflight.warningMessage
            return false
        }
        val media = _inspectedMedia.value
        if (media == null && resumeJobId == null) {
            _recoveryError.value = "No media selected"
            return false
        }

        val jobId = resumeJobId ?: UUID.randomUUID().toString()
        _activeJobId.value = jobId

        viewModelScope.launch {
            val existing = if (resumeJobId != null) container.database.jobDao().getJob(resumeJobId) else null
            val serviceIntent = Intent(app, TransVibeProcessingService::class.java).apply {
                putExtra(TransVibeProcessingService.EXTRA_JOB_ID, jobId)
                putExtra(TransVibeProcessingService.EXTRA_MEDIA_URI, existing?.mediaUriString ?: media!!.uriString)
                putExtra(TransVibeProcessingService.EXTRA_MEDIA_TITLE, existing?.mediaTitle ?: media!!.displayName)
                putExtra(TransVibeProcessingService.EXTRA_TRACK_INDEX, existing?.selectedAudioTrackIndex ?: _selectedTrackIndex.value)
                putExtra(TransVibeProcessingService.EXTRA_MODEL_ID, existing?.selectedModelId ?: _selectedModel.value.modelId)
                putExtra(TransVibeProcessingService.EXTRA_SOURCE_LANG, existing?.sourceLanguageSetting ?: sourceLang)
                putExtra(TransVibeProcessingService.EXTRA_TARGET_LANG, existing?.targetLanguageSetting ?: targetLang)
            }
            app.startForegroundService(serviceIntent)
        }
        return true
    }

    fun cancelProcessing(app: Application) {
        val intent = Intent(app, TransVibeProcessingService::class.java).apply {
            action = TransVibeProcessingService.ACTION_CANCEL
        }
        app.startService(intent)
    }

    fun loadJobCues(jobId: String) {
        _activeJobId.value = jobId
        viewModelScope.launch {
            _activeJobEntity.value = container.database.jobDao().getJob(jobId)
            _activeCues.value = container.orchestrator.loadCuesForJob(jobId)
        }
    }

    fun retranslateActiveJob(targetLang: String = "ar") {
        val jobId = _activeJobId.value ?: return
        val src = _activeJobEntity.value?.sourceLanguageSetting ?: "unknown"
        viewModelScope.launch {
            container.orchestrator.retranslateCachedTranscript(jobId, src, targetLang)
                .onSuccess { loadJobCues(jobId) }
                .onFailure { err -> _recoveryError.value = err.message ?: "Retranslation failed" }
        }
    }

    fun editCue(cueId: String, newOriginal: String?, newTranslation: String?) {
        val jobId = _activeJobId.value ?: return
        val updated = container.subtitleEditor.updateCueText(_activeCues.value, cueId, newOriginal, newTranslation)
        _activeCues.value = updated
        val editedCue = updated.find { it.cueId == cueId } ?: return
        viewModelScope.launch {
            container.database.transcriptDao().upsertCueEdit(
                CueEditEntity(
                    cueId = cueId,
                    jobId = jobId,
                    editedOriginal = editedCue.userEditedOriginal,
                    editedTranslation = editedCue.userEditedTranslation,
                    revision = editedCue.revision,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun undoCueEdit() {
        val prev = container.subtitleEditor.undo() ?: return
        _activeCues.value = prev
    }

    fun canUndoCueEdit(): Boolean = container.subtitleEditor.canUndo()

    fun exportSubtitles(uri: Uri, asVtt: Boolean, bilingual: Boolean) {
        viewModelScope.launch {
            val content = if (asVtt) {
                container.subtitleExporter.exporter.formatVtt(_activeCues.value, bilingual)
            } else {
                container.subtitleExporter.exporter.formatSrt(_activeCues.value, bilingual)
            }
            container.subtitleExporter.exporter.exportToSaf(uri, content).onFailure { err ->
                _recoveryError.value = err.message
            }
        }
    }

    fun requestVideoBurnInCheck() {
        if (!FfmpegFallbackBridge.isAvailable()) {
            _recoveryError.value = "NOT_SUPPORTED_YET: Optional FFmpegKit AAR is not bundled in app/libs/ for MP4 burn-in."
        }
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch { container.database.jobDao().deleteJob(jobId) }
    }

    fun loadFreezeRecords() {
        viewModelScope.launch {
            val list = container.database.benchmarkDao().getAllFreezeRecords()
            _freezeRecords.value = list.associateBy { it.modelId }
        }
    }

    fun approveModelFreeze(modelId: String) {
        viewModelScope.launch {
            val profile = ModelProfiles.forModel(modelId)
            val rec = FreezeRecordEntity(
                modelId = modelId,
                status = FreezeStatus.FROZEN.name,
                profileVersion = profile.profileVersion,
                approvedByOwner = true,
                ownerSignoffTimestampMs = System.currentTimeMillis(),
                notes = "Owner approved freeze via Benchmark Lab"
            )
            container.database.benchmarkDao().upsertFreezeRecord(rec)
            loadFreezeRecords()
        }
    }

    // E08: Real Whisper Tiny inference benchmark on actual selected media (no silent dummy PCM)
    fun runTinyBenchmarkStudy(corpusMediaUri: Uri?) {
        viewModelScope.launch(Dispatchers.Default) {
            if (corpusMediaUri == null) {
                _recoveryError.value = "Select a reference audio/video clip first to run a real Whisper Tiny benchmark."
                return@launch
            }
            val tiny = ModelCatalog.findById(ModelCatalog.WHISPER_TINY_ID)!!
            val installed = modelRecords.value[tiny.modelId]
            if (installed?.status != ModelStatus.READY || installed.installedPath == null) {
                _recoveryError.value = "MODEL_NOT_INSTALLED: Install ggml-tiny.bin before running Tiny benchmark"
                return@launch
            }

            val inspection = container.mediaInspector.inspect(corpusMediaUri).getOrElse {
                _recoveryError.value = it.message ?: "Cannot inspect benchmark clip"
                return@launch
            }

            val pcmBuffer = ArrayList<Float>(16_000 * 30)
            container.audioDecoder.streamDecodedPcm(corpusMediaUri, inspection.audioTracks.first().trackIndex, 0L)
                .collect { frame ->
                    val resampled = container.resampler.resampleToMono16kFloat(frame.pcm16, frame.sampleRateHz, frame.channelCount)
                    for (s in resampled) {
                        if (pcmBuffer.size < 16_000 * 30) pcmBuffer.add(s)
                    }
                }
            if (pcmBuffer.isEmpty()) {
                _recoveryError.value = "Benchmark clip produced 0 PCM samples"
                return@launch
            }
            val samples = pcmBuffer.toFloatArray()
            val baseProfile = ModelProfiles.forModel(tiny.modelId)
            val schedule = TinyBenchmarkRunner.buildInterleavedSchedule(listOf(2, 3, 4), runsPerCandidate = 3)
            val records = mutableListOf<BenchmarkRunRecord>()
            val engine = WhisperEngine()

            try {
                schedule.forEachIndexed { idx, threads ->
                    val profile = baseProfile.copy(defaultThreads = threads)
                    val e2eStart = System.currentTimeMillis()
                    engine.open(tiny, installed.installedPath, profile).getOrThrow()
                    val seg = AudioSegment(idx, 0L, (samples.size * 1_000_000L) / 16_000L, pcmSamples = samples)
                    val res = engine.transcribe(seg, "en").getOrThrow()
                    val e2eEnd = System.currentTimeMillis() - e2eStart
                    engine.close()

                    val joinedText = res.segments.joinToString(" ") { it.text }
                    val textHash = MessageDigest.getInstance("SHA-256")
                        .digest(joinedText.toByteArray()).joinToString("") { "%02x".format(it) }.take(12)

                    records.add(
                        BenchmarkRunRecord(
                            runId = UUID.randomUUID().toString(),
                            modelId = tiny.modelId,
                            threads = threads,
                            runOrderIndex = idx,
                            mediaFingerprint = inspection.contentFingerprint,
                            modelSha256 = installed.localSha256 ?: "local",
                            inferenceTimeMs = res.inferenceDurationMs,
                            endToEndTimeMs = e2eEnd,
                            peakPssKb = android.os.Debug.getPss(),
                            thermalStatus = container.thermalMonitor.currentThermalStatus(),
                            outputHash = textHash,
                            transcriptPreview = joinedText.take(120),
                            timestampMs = System.currentTimeMillis()
                        )
                    )
                }
                _benchmarkSummaries.value = TinyBenchmarkRunner.summarize(records)
            } catch (t: Throwable) {
                _recoveryError.value = t.message ?: "Benchmark failed"
            } finally {
                engine.close()
            }
        }
    }

    fun refreshCacheSize(app: Application) {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheBytes = app.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            val stagingBytes = File(app.filesDir, "staging").walkTopDown().filter { it.isFile }.sumOf { it.length() }
            _cacheSizeMb.value = (cacheBytes + stagingBytes) / (1024L * 1024L)
        }
    }

    fun clearAppCache(app: Application) {
        viewModelScope.launch(Dispatchers.IO) {
            app.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            File(app.filesDir, "staging").listFiles()?.forEach { it.deleteRecursively() }
            refreshCacheSize(app)
        }
    }

    fun buildRedactedDiagnosticText(): String {
        val installed = modelRecords.value.filterValues { it.status == ModelStatus.READY }.keys.toList()
        return DiagnosticExporter.buildRedactedReport(installed, emptyList())
    }

    fun dismissError() {
        _recoveryError.value = null
    }
}

class TransVibeViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransVibeViewModel(container) as T
    }
}
