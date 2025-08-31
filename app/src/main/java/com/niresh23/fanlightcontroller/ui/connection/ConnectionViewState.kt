package com.niresh23.fanlightcontroller.ui.connection

data class ConnectionViewState (
    val scanning: Boolean = false,
    val deviceList: List<DeviceViewState> = emptyList(),
    val error: String? = null
)