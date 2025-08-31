package com.niresh23.fanlightcontroller.ble

import kotlinx.coroutines.flow.Flow

interface IBleAdapter {
    val bleDeviceEventFlow: Flow<DeviceEvent>

    fun connect(address: String)
    fun disconnect()
    fun sendMessage(message: ByteArray)
    fun requestBatteryLevel()
    fun release()
}