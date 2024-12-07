package com.niresh23.fanlightcontroller.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.niresh23.fanlightcontroller.settingsDataStore
import com.niresh23.fanlightcontroller.utils.SettingKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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