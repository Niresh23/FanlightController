package com.niresh23.fanlightcontroller.ui.connection

import com.niresh23.fanlightcontroller.ui.connection.BleScannerAdapter.ScanEvent
import kotlinx.coroutines.flow.Flow

interface IBleScanner {
    val scanEventFlow: Flow<ScanEvent>
    fun startScan()
    fun stopScan()
    fun release()
}