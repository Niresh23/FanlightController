package com.niresh23.fanlightcontroller.storage

import kotlinx.coroutines.flow.Flow

interface ISettingsLocalStorage {
    val brightnessFlow: Flow<Float>
    val frequencyFlow: Flow<Float>

    suspend fun setBrightness(value: Float)

    suspend fun setFrequency(value: Float)
}