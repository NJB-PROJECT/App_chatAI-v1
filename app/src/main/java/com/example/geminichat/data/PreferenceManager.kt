package com.example.geminichat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {

    companion object {
        val KEY_IS_CUSTOM_KEY_ENABLED = booleanPreferencesKey("is_custom_key_enabled")
        val KEY_CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        val KEY_MODEL_TYPE = stringPreferencesKey("model_type") // "flash" or "pro"
        val KEY_SAFETY_LEVEL = intPreferencesKey("safety_level") // 0: 18+, 1: Block Some, 2: Block All
    }

    val isCustomKeyEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_CUSTOM_KEY_ENABLED] ?: false
    }

    val customApiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_API_KEY]
    }

    val modelType: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_MODEL_TYPE] ?: "flash"
    }

    // Default to "Block Some" (1)
    val safetyLevel: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_SAFETY_LEVEL] ?: 1
    }

    suspend fun setCustomKeyEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_CUSTOM_KEY_ENABLED] = enabled
        }
    }

    suspend fun setCustomApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CUSTOM_API_KEY] = key
        }
    }

    suspend fun setModelType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MODEL_TYPE] = type
        }
    }

    suspend fun setSafetyLevel(level: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SAFETY_LEVEL] = level
        }
    }
}
