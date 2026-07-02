package com.niresh23.fanlightcontroller.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niresh23.fanlightcontroller.ble.IBleServiceExecutor
import com.niresh23.fanlightcontroller.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val serviceExecutor: IBleServiceExecutor,
    private val bleScanner: IBleScanner
) : ViewModel() {

    private val _viewStateFlow = MutableStateFlow(ConnectionViewState())
    val viewState = _viewStateFlow.asStateFlow()

    private var allDevices: List<DeviceViewState> = emptyList()

    init {
        viewModelScope.launch {
            bleScanner.scanEventFlow.collect { scanAction ->
                when(scanAction) {
                    is BleScannerAdapter.ScanEvent.ScanResult -> {
                        serviceExecutor.addDevices(scanAction.scanResults)
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
            serviceExecutor.devicesViewState.collect { devices ->
                allDevices = devices
                _viewStateFlow.value = _viewStateFlow.value.copy(
                    deviceList = filterDevices(devices, _viewStateFlow.value.onlyExo)
                )
            }
        }
    }

    fun onAction(action: ConnectionAction) {
        viewModelScope.launch {
            when(action) {
                is ConnectionAction.Scan -> {
                    bleScanner.startScan(Constants.SERVICE_UUID, Constants.SERVICE_UUID_2)
                }

                ConnectionAction.StopScan -> {
                    bleScanner.stopScan()
                }

                is ConnectionAction.ChangeScanFilter -> {
                    _viewStateFlow.value = _viewStateFlow.value.copy(
                        onlyExo = action.value,
                        deviceList = filterDevices(allDevices, action.value)
                    )
                }
            }
        }
    }

    private fun filterDevices(devices: List<DeviceViewState>, onlyExo: Boolean): List<DeviceViewState> {
        if (!onlyExo) return devices
        return devices.filter { it.name.contains("exo", ignoreCase = true) }
    }

    override fun onCleared() {
        bleScanner.release()
        super.onCleared()
    }
}