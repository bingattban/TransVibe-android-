package com.transvibe.app.asr

import com.transvibe.app.domain.AsrEngine
import com.transvibe.app.domain.EngineType
import com.transvibe.app.domain.ModelDefinition

/**
 * Strict ASR engine router. Manual user selection ALWAYS wins; never silently falls back to Whisper.
 */
class AsrRouter {
    fun createEngineFor(model: ModelDefinition): AsrEngine = when (model.engineType) {
        EngineType.WHISPER_CPP -> WhisperEngine()
        EngineType.SHERPA_MOONSHINE_AR -> MoonshineArabicEngine()
        EngineType.SHERPA_SENSEVOICE -> SenseVoiceEngine()
    }
}
