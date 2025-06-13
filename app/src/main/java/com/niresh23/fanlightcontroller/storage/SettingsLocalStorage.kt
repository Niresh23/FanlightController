package com.niresh23.fanlightcontroller.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.niresh23.fanlightcontroller.settingsDataStore
import com.niresh23.fanlightcontroller.utils.SettingKey
import com.niresh23.fanlightcontroller.visualizer.AudioVisualizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class SettingsLocalStorageImpl(
    private val context: Context
) : ISettingsLocalStorage {
    override val brightnessFlow: Flow<Float> by lazy {
        val key = floatPreferencesKey(SettingKey.BRIGHTNESS_KEY)
        context.settingsDataStore.data.map {
            it[key] ?: 1f
        }
    }

    override val frequencyFlow: Flow<Float> by lazy {
        val key = floatPreferencesKey(SettingKey.VISUALIZATION_FREQUENCY_KEY)
        context.settingsDataStore.data.map {
            it[key] ?: 1f
        }
    }

    override val visualizerParamsFlow: Flow<AudioVisualizer.Param> by lazy {
        val key = stringPreferencesKey(SettingKey.AUDIO_VISUALIZER_PARAM_KEY)

        context.settingsDataStore.data.map {
            val params = it[key]

            if (!params.isNullOrEmpty()) {
                Json.decodeFromString(params)
            } else {
                AudioVisualizer.Param()
            }
        }
    }

    override suspend fun setAudioVisualizerParams(param: AudioVisualizer.Param) {
        context.settingsDataStore.edit { dataStore ->
            val key = stringPreferencesKey(SettingKey.AUDIO_VISUALIZER_PARAM_KEY)
            dataStore[key] = Json.encodeToString(param)
        }
    }

    override suspend fun setBrightness(value: Float) {
        context.settingsDataStore.edit { settings ->
            val key = floatPreferencesKey(SettingKey.BRIGHTNESS_KEY)
            settings[key] = value
        }
    }

    override suspend fun setFrequency(value: Float) {
        context.settingsDataStore.edit { settings ->
            val key = floatPreferencesKey(SettingKey.VISUALIZATION_FREQUENCY_KEY)
            settings[key] = value
        }
    }
}