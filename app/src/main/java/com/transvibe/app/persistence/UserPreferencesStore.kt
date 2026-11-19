package com.transvibe.app.persistence

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "transvibe_prefs")

data class UserPreferences(
    val uiLanguage: String = "ar",
    val onboardingCompleted: Boolean = false,
    val defaultModelId: String = "whisper-tiny",
    val bilingualExportDefault: Boolean = false
)

class UserPreferencesStore(private val context: Context) {
    private object Keys {
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DEFAULT_MODEL_ID = stringPreferencesKey("default_model_id")
        val BILINGUAL_EXPORT = booleanPreferencesKey("bilingual_export")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            uiLanguage = prefs[Keys.UI_LANGUAGE] ?: "ar",
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            defaultModelId = prefs[Keys.DEFAULT_MODEL_ID] ?: "whisper-tiny",
            bilingualExportDefault = prefs[Keys.BILINGUAL_EXPORT] ?: false
        )
    }

    suspend fun setUiLanguage(lang: String) {
        context.dataStore.edit { it[Keys.UI_LANGUAGE] = lang }
    }

    suspend fun setOnboardingCompleted(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = done }
    }

    suspend fun setDefaultModelId(modelId: String) {
        context.dataStore.edit { it[Keys.DEFAULT_MODEL_ID] = modelId }
    }
}
