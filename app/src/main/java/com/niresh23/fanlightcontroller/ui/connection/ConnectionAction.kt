package com.niresh23.fanlightcontroller.ui.connection

import com.niresh23.fanlightcontroller.utils.Constants

sealed interface ConnectionAction {
    data class ChangeScanFilter(val value: Boolean): ConnectionAction
    data class Scan(val uuids: String = Constants.SERVICE_UUID): ConnectionAction
    data object StopScan: ConnectionAction
}