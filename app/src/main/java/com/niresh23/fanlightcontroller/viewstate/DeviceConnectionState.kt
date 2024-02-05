package com.niresh23.fanlightcontroller.viewstate

import android.bluetooth.BluetoothDevice

sealed class DeviceConnectionState {
    class Connected(val device: BluetoothDevice) : DeviceConnectionState()
    object Connecting : DeviceConnectionState()
    object Disconnected : DeviceConnectionState()
}