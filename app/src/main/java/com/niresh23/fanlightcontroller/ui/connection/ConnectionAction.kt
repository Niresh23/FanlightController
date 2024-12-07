package com.niresh23.fanlightcontroller.ui.connection

sealed interface ConnectionAction {
    data object Scan: ConnectionAction
    data object StopScan: ConnectionAction
}