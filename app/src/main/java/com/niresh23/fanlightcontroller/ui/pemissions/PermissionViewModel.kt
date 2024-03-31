package com.niresh23.fanlightcontroller.ui.pemissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.Exception

class PermissionViewModel: ViewModel() {
    private val _viewStateFlow = MutableStateFlow(PermissionViewState())
    val permissionViewStateStateFlow = _viewStateFlow.asStateFlow()

    private val _actionFlow = MutableSharedFlow<Action>()
    val actionFlow = _actionFlow.asSharedFlow()

    private val _actionViewFlow = MutableStateFlow<ActionView>(ActionView.Close)
    val actonViewFlow = _actionViewFlow.asStateFlow()

    fun requestBluetoothPermissionClicked() {
        viewModelScope.launch {
            _actionFlow.emit(Action.RequestBluetoothPermission)
        }
    }

    fun requestLocationPermissionClicked() {
        viewModelScope.launch {
            _actionFlow.emit(Action.RequestLocationPermission)
        }
    }

    fun bluetoothPermissionGranted(status: Boolean) {
        _viewStateFlow.value = _viewStateFlow.value.copy(bluetoothPermissionGranted = status)
    }

    fun locationPermissionGranted(status: Boolean) {
        _viewStateFlow.value = _viewStateFlow.value.copy(locationPermissionGranted = status)
    }

    fun bluetoothEnable(isEnabled: Boolean) {
        _viewStateFlow.value = _viewStateFlow.value.copy(bluetoothIsEnabled = isEnabled)
    }

    fun locationEnable(isEnabled: Boolean) {
        _viewStateFlow.value = _viewStateFlow.value.copy(locationIsEnabled = isEnabled)
    }

    fun enableBluetoothClicked() {
        viewModelScope.launch {
            if (!_viewStateFlow.value.bluetoothPermissionGranted) {
                _actionViewFlow.emit(ActionView.ShowBluetoothPermissionDialog)
            } else if(!_viewStateFlow.value.bluetoothIsEnabled) {
                _actionFlow.emit(Action.EnableBluetooth)
            }
        }
    }

    fun enableLocationClicked() {
        viewModelScope.launch {
            if (!_viewStateFlow.value.locationPermissionGranted) {
                _actionViewFlow.emit(ActionView.ShowLocationPermissionDialog)
            } else if(!_viewStateFlow.value.locationIsEnabled) {
                _actionFlow.emit(Action.EnableLocation)
            }
        }
    }

    fun showExceptionDialog(exception: Exception) {
        viewModelScope.launch {
            _actionViewFlow.emit(ActionView.ShowErrorDialog(exception.message ?: "No Error Message"))
        }
    }

    fun closeDialog() {
        _actionViewFlow.value = ActionView.Close
    }

    sealed interface Action {
        object EnableBluetooth: Action
        object EnableLocation: Action
        object RequestBluetoothPermission: Action
        object RequestLocationPermission: Action
    }

    sealed interface ActionView {
        object Close: ActionView
        object ShowBluetoothPermissionDialog: ActionView
        object ShowLocationPermissionDialog: ActionView
        data class ShowErrorDialog(val message: String): ActionView
    }
}