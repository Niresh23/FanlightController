package com.niresh23.fanlightcontroller.ble

import android.app.Activity
import android.content.Context
import android.os.Handler
import java.util.UUID

data class BluetoothModel (
    var DEVICENAME: String? = null,
    var RXDESCRIPTERUUID: UUID? = null,
    var RXDUUID: UUID? = null,
    var SCAN_PERIOD: Long = 0L,
    var SCAN_TIMEOUT: Long = 0L,
    var ServiceUUID: UUID? = null,
    var TXDESCRIPTERUUID: UUID? = null,
    var TXDUUID: UUID? = null,
    var activity: Activity? = null,
    var artist: Int = 6,
    var context: Context? = null,
    var endStr: String? = null,
    var handler: Handler? = null,
    var startStr: String? = null
)