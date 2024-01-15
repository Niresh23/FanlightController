package com.niresh23.fanlightcontroller.ble

import android.bluetooth.BluetoothDevice




class ScanResultModel {
    var Rssi = 0

    var device: BluetoothDevice? = null

    fun getDevice(): BluetoothDevice? {
        return device
    }

    fun getRssi(): Int {
        return Rssi
    }

    fun setDevice(paramBluetoothDevice: BluetoothDevice?) {
        device = paramBluetoothDevice
    }

    fun setRssi(paramInt: Int) {
        Rssi = paramInt
    }
}