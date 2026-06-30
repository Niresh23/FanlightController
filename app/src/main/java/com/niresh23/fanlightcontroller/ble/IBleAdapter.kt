package com.niresh23.fanlightcontroller.ble

import kotlinx.coroutines.flow.Flow

interface IBleAdapter {
    val bleDeviceEventFlow: Flow<DeviceEvent>

    fun connect(address: String)
    fun disconnect()
    fun sendMessage(message: ByteArray)
    fun sendRainbowMessage(message: ByteArray)
    fun requestBatteryLevel()
    fun release()
    fun selectService(service: String)
    fun selectCharacteristic(characteristic: String)
}