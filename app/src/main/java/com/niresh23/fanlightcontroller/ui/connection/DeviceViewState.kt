package com.niresh23.fanlightcontroller.ui.connection

sealed interface DeviceViewState {
    val name: String
    val address: String

    data class Disconnected(
        override val name: String,
        override val address: String
    ) : DeviceViewState

    data class Connected(
        override val name: String,
        override val address: String
    ): DeviceViewState

    data class Error(
        override val name: String,
        override val address: String,
        val errorMessage: String,
        val code: Int
    ): DeviceViewState
}
