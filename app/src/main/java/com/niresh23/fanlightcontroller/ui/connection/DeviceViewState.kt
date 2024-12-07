package com.niresh23.fanlightcontroller.ui.connection

data class DeviceViewState (
    val name: String,
    val address: String,
    val connected: Boolean = false,
    val connecting: Boolean = false
)
