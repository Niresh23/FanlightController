package com.niresh23.fanlightcontroller.ui.connection

data class DeviceViewState (
    val name: String,
    val address: String,
    val status: DeviceConnectionStatus = DeviceConnectionStatus.DISCONNECTED,
    val connecting: Boolean = false,
    val batteryLevel: Int = 0
)

enum class DeviceConnectionStatus {
    DISCONNECTED,
    CONNECTED,
    CONNECTING,
    DISCONNECTING
}
