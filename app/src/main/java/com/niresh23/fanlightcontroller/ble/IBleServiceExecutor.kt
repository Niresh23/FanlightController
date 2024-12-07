package com.niresh23.fanlightcontroller.ble

import kotlinx.coroutines.flow.Flow

interface IBleServiceExecutor {
    val serviceEventFlow: Flow<DeviceEvent>

    fun onStart()

    fun onStop()

    fun connect(address: String)

    fun disconnect(address: String)

    fun changeColor(value: Int)

    fun startAudioVisualizer()

    fun stopAudioVisualizer()

    fun changeVisualizerFrequency(value: Float)

    fun changeBrightness(value: Float)
}