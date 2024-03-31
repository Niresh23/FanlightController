package com.niresh23.fanlightcontroller.ui.scan

import android.app.Application
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niresh23.fanlightcontroller.viewstate.DeviceScanViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DeviceScanViewModel(app: Application) : AndroidViewModel(app) {


    private val _viewState = MutableStateFlow<DeviceScanViewState>(DeviceScanViewState.Idle)
    val viewState = _viewState.asStateFlow()
    private val bleScannerAdapter = BleScannerAdapter(app, viewModelScope)

    init {
        viewModelScope.launch {
            bleScannerAdapter.scanActionFlow.collectLatest {
                when(it) {
                    is BleScannerAdapter.ScanAction.ScanResult -> {
                        _viewState.value = DeviceScanViewState.ScanResults(it.scanResults)
                    }

                    is BleScannerAdapter.ScanAction.Error -> {
                        _viewState.value = DeviceScanViewState.Error(it.message)
                    }
                }
            }
        }
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    override fun onCleared() {
        super.onCleared()
        bleScannerAdapter.onDismiss()
        stopScanning()
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    fun startScan() {
        _viewState.value = DeviceScanViewState.ActiveScan
        bleScannerAdapter.startScan()
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    fun stopScanning() {
        bleScannerAdapter.stopScan()
    }
}