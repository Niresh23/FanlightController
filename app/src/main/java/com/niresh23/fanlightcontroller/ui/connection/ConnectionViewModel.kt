package com.niresh23.fanlightcontroller.ui.connection

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConnectionViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _viewStateFlow = MutableStateFlow(ConnectionViewState())
    val viewState = _viewStateFlow.asStateFlow()

    val deviceMapState = mutableStateMapOf<String, DeviceViewState>()

    private val bleScannerAdapter = BleScannerAdapter(app, viewModelScope)

    private val _connectToDeviceFlow = MutableSharedFlow<String>()
    val connectToDeviceFlow = _connectToDeviceFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            bleScannerAdapter.scanActionFlow.collectLatest { scanAction ->
                when(scanAction) {
                    is BleScannerAdapter.ScanAction.ScanResult -> {
                        _viewStateFlow.value = _viewStateFlow.value.copy(scanning = false)
                        scanAction.scanResults.forEach { (address, device) ->
                            deviceMapState[address] = DeviceViewState.Disconnected(
                                "Exo Light stick",
                                address
                            )
                        }
                    }
                    is BleScannerAdapter.ScanAction.Error -> {
                        _viewStateFlow.value = _viewStateFlow.value.copy(scanning = false)
                    }
                    is BleScannerAdapter.ScanAction.Scanning -> {
                        _viewStateFlow.value = _viewStateFlow.value.copy(scanning = true)
                    }
                }
            }
        }
    }

    fun onAction(action: ConnectionAction) {
        viewModelScope.launch {
            when(action) {
                is ConnectionAction.Connect -> {
                    _connectToDeviceFlow.emit(action.deviceAddress)
                }

                is ConnectionAction.Disconnect -> {
                    _connectToDeviceFlow.emit(action.deviceAddress)
                }

                ConnectionAction.Scan -> {
                    if (ActivityCompat.checkSelfPermission(
                            app,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        bleScannerAdapter.startScan()
                    }
                }

                ConnectionAction.StopScan -> {
                    bleScannerAdapter.stopScan()
                }
                is ConnectionAction.DeviceConnected -> {
                    deviceMapState[action.deviceAddress] =
                        DeviceViewState.Connected("Exo Light Stick", address = action.deviceAddress)
                }
            }
        }
    }

    override fun onCleared() {
        if(ActivityCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bleScannerAdapter.stopScan()
        }
        super.onCleared()
    }
}