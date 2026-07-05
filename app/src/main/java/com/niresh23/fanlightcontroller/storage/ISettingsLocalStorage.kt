package com.niresh23.fanlightcontroller.storage

import com.niresh23.fanlightcontroller.visualizer.AudioVisualizer
import kotlinx.coroutines.flow.Flow

interface ISettingsLocalStorage {
    val brightnessFlow: Flow<Float>
    val frequencyFlow: Flow<Float>
    val visualizerParamsFlow: Flow<AudioVisualizer.Param>

    fun setAudioVisualizerParams(param: AudioVisualizer.Param)

    fun setBrightness(value: Float)

    fun setFrequency(value: Float)
}