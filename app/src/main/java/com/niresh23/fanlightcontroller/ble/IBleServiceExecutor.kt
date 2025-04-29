package com.niresh23.fanlightcontroller.ble

import com.niresh23.fanlightcontroller.ui.connection.DeviceViewState
import kotlinx.coroutines.flow.Flow

interface IBleServiceExecutor {
    val devicesViewState: Flow<List<DeviceViewState>>

    fun onStart()

    fun onStop()

    fun connect(address: String)

    fun disconnect(address: String)

    fun changeColor(value: Int)

    fun startAudioVisualizer()

    fun stopAudioVisualizer()

    fun changeVisualizerFrequency(value: Float)

    fun changeBrightness(value: Float)

    fun addDevices(devices: Collection<String>)
}