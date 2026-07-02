package com.niresh23.fanlightcontroller.ble

import com.niresh23.fanlightcontroller.ui.connection.DeviceViewState
import com.niresh23.fanlightcontroller.visualizer.AudioVisualizer
import kotlinx.coroutines.flow.StateFlow

interface IBleServiceExecutor {
    val devicesViewState: StateFlow<List<DeviceViewState>>

    fun onStart()

    fun onStop()

    fun connect(address: String)

    fun disconnect(address: String)

    fun changeColor(value: Int)

    fun startAudioVisualizer()

    fun stopAudioVisualizer()

    fun changeVisualizerFrequency(value: Float)

    fun changeBrightness(value: Float)

    fun addDevices(devices: Collection<BleDeviceData>)

    fun changeVisualizerParam(param: AudioVisualizer.Param)

    fun characteristicSelected(address: String, characteristic: String)

    fun serviceSelected(address: String, service: String)

    fun sendRainbowMessage()
}