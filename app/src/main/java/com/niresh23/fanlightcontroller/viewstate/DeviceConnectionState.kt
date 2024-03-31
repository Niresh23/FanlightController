package com.niresh23.fanlightcontroller.viewstate

sealed class DeviceConnectionState {
    object Connected : DeviceConnectionState()
    object Connecting : DeviceConnectionState()
    object Disconnected : DeviceConnectionState()
}