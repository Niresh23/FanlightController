package com.niresh23.fanlightcontroller.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niresh23.fanlightcontroller.ble.IBleServiceExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val serviceExecutor: IBleServiceExecutor,
    private val bleScanner: IBleScanner
) : ViewModel() {

    private val _viewStateFlow = MutableStateFlow(ConnectionViewState())
    val viewState = _viewStateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            bleScanner.scanEventFlow.collectLatest { scanAction ->
                when(scanAction) {
                    is BleScannerAdapter.ScanEvent.ScanResult -> {
                        serviceExecutor.addDevices(scanAction.scanResults.keys)
                        _viewStateFlow.value = _viewStateFlow.value.copy(scanning = false)
                    }

                    is BleScannerAdapter.ScanEvent.Error -> {
                        _viewStateFlow.value = _viewStateFlow.value.copy(scanning = false, error = scanAction.message)
                    }

                    is BleScannerAdapter.ScanEvent.Scanning -> {
                        _viewStateFlow.value = _viewStateFlow.value.copy(scanning = true)
                    }

                    BleScannerAdapter.ScanEvent.StopScanning -> {
                        _viewStateFlow.value = _viewStateFlow.value.copy(scanning = false, error = null)
                    }
                }
            }
        }
    }

    fun onCreate() {
        viewModelScope.launch {
            serviceExecutor.devicesViewState.collect {
                _viewStateFlow.value = _viewStateFlow.value.copy(deviceList = it)
            }
        }
    }

    fun onAction(action: ConnectionAction) {
        viewModelScope.launch {
            when(action) {
                ConnectionAction.Scan -> {
                    bleScanner.startScan()
                }

                ConnectionAction.StopScan -> {
                    bleScanner.stopScan()
                }
            }
        }
    }

    override fun onCleared() {
        bleScanner.release()
        super.onCleared()
    }
}