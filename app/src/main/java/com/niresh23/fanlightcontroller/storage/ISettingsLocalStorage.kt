package com.niresh23.fanlightcontroller.storage

import com.niresh23.fanlightcontroller.visualizer.AudioVisualizer
import kotlinx.coroutines.flow.Flow

interface ISettingsLocalStorage {
    val brightnessFlow: Flow<Float>
    val frequencyFlow: Flow<Float>
    val visualizerParamsFlow: Flow<AudioVisualizer.Param>

    suspend fun setAudioVisualizerParams(param: AudioVisualizer.Param)

    suspend fun setBrightness(value: Float)

    suspend fun setFrequency(value: Float)
}