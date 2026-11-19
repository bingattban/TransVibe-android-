package com.transvibe.app

import android.app.Application
import com.transvibe.app.asr.AsrRouter
import com.transvibe.app.media.AudioResampler
import com.transvibe.app.media.MediaCodecAudioDecoder
import com.transvibe.app.media.MediaInspector
import com.transvibe.app.media.SileroVadEngine
import com.transvibe.app.models.ModelLifecycleManager
import com.transvibe.app.persistence.TransVibeDatabase
import com.transvibe.app.persistence.UserPreferencesStore
import com.transvibe.app.platform.ThermalMonitor
import com.transvibe.app.processing.ProcessingOrchestrator
import com.transvibe.app.subtitles.SrtVttExporter
import com.transvibe.app.subtitles.SubtitleEditorRepository
import com.transvibe.app.translation.MlKitTranslationEngine
import com.transvibe.app.translation.TranslationPackManager

class AppContainer(app: Application) {
    val database: TransVibeDatabase by lazy { TransVibeDatabase.getInstance(app) }
    val preferences: UserPreferencesStore by lazy { UserPreferencesStore(app) }
    val modelLifecycle: ModelLifecycleManager by lazy { ModelLifecycleManager(app) }
    val mediaInspector: MediaInspector by lazy { MediaInspector(app) }
    val audioDecoder: MediaCodecAudioDecoder by lazy { MediaCodecAudioDecoder(app, mediaInspector) }
    val resampler: AudioResampler by lazy { AudioResampler() }
    val vadEngine: SileroVadEngine by lazy { SileroVadEngine(app) }
    val asrRouter: AsrRouter by lazy { AsrRouter() }
    val translationEngine: MlKitTranslationEngine by lazy { MlKitTranslationEngine() }
    val translationPackManager: TranslationPackManager by lazy { TranslationPackManager(app) }
    val subtitleExporter: SrtVibeExporterWrapper by lazy { SrtVibeExporterWrapper(SrtVttExporter(app)) }
    val subtitleEditor: SubtitleEditorRepository by lazy { SubtitleEditorRepository() }
    val thermalMonitor: ThermalMonitor by lazy { ThermalMonitor(app) }

    val orchestrator: ProcessingOrchestrator by lazy {
        ProcessingOrchestrator(
            decoder = audioDecoder,
            resampler = resampler,
            vadEngine = vadEngine,
            asrRouter = asrRouter,
            translationEngine = translationEngine,
            translationPackManager = translationPackManager,
            modelLifecycle = modelLifecycle,
            jobDao = database.jobDao(),
            transcriptDao = database.transcriptDao()
        )
    }
}

class SrtVibeExporterWrapper(val exporter: SrtVttExporter)

class TransVibeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // E06: Check & auto-download core EN + AR ML Kit packs without blocking startup or ASR models
        container.translationPackManager.ensureCoreEnArOnFirstLaunch()
    }
}
