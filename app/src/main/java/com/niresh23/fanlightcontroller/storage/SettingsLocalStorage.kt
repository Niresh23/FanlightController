package com.niresh23.fanlightcontroller.storage

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.niresh23.fanlightcontroller.settingsDataStore
import com.niresh23.fanlightcontroller.utils.SettingKey
import com.niresh23.fanlightcontroller.visualizer.AudioVisualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

class SettingsLocalStorageImpl(
    private val context: Context,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : ISettingsLocalStorage {
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    private fun editSetting(block: suspend (MutablePreferences) -> Unit) {
        scope.launch {
            context.settingsDataStore.edit(block)
        }
    }

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

    override fun setAudioVisualizerParams(param: AudioVisualizer.Param) {
        editSetting { dataStore ->
            val key = stringPreferencesKey(SettingKey.AUDIO_VISUALIZER_PARAM_KEY)
            dataStore[key] = Json.encodeToString(param)
        }
    }

    override fun setBrightness(value: Float) {
        editSetting { settings ->
            val key = floatPreferencesKey(SettingKey.BRIGHTNESS_KEY)
            settings[key] = value
        }
    }

    override fun setFrequency(value: Float) {
        editSetting { settings ->
            val key = floatPreferencesKey(SettingKey.VISUALIZATION_FREQUENCY_KEY)
            settings[key] = value
        }
    }
}