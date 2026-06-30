package com.niresh23.fanlightcontroller.ble

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BleDeviceData(
    val address: String,
    val name: String,
    val connectable: Boolean
) : Parcelable
