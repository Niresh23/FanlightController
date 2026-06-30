package com.niresh23.fanlightcontroller.ui.connection

data class DeviceViewState (
    val name: String,
    val address: String,
    val connectable: Boolean,
    val status: DeviceConnectionStatus = DeviceConnectionStatus.DISCONNECTED,
    val connecting: Boolean = false,
    val batteryLevel: Int = 0,
    val selectedService: String = "",
    val selectedCharacteristic: String = "",
    val characteristics: List<String> = emptyList(),
    val services: List<String> = emptyList(),
    val rainbowIsVisible: Boolean = false
)

enum class DeviceConnectionStatus {
    DISCONNECTED,
    CONNECTED,
    CONNECTING,
    DISCONNECTING
}
