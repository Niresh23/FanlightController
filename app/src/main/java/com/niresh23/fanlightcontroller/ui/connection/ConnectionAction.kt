package com.niresh23.fanlightcontroller.ui.connection

sealed interface ConnectionAction {
    data object Scan: ConnectionAction

    data class Connect(
        val deviceAddress: String
    ): ConnectionAction

    data class Disconnect(
        val deviceAddress: String
    ): ConnectionAction

    data class DeviceConnected(val deviceAddress: String): ConnectionAction

    data object StopScan: ConnectionAction
}