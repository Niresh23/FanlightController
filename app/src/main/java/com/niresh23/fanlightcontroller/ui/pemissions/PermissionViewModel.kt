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

    private val _actionViewFlow = MutableStateFlow<ActionView>(ActionView.Non)
    val actonViewFlow = _actionViewFlow.asStateFlow()

    private val _showDialogFlow = MutableStateFlow<ShowDialogState>(ShowDialogState.Closed)
    val showDialogFlow = _showDialogFlow.asStateFlow()

    fun requestBluetoothPermission() {
        viewModelScope.launch {
            _actionFlow.emit(Action.RequestBluetoothPermission)
        }
    }

    fun bluetoothPermissionGranted(status: Boolean) {
        _viewStateFlow.value = _viewStateFlow.value.copy(bluetoothPermissionGranted = status)
    }

    fun bluetoothEnable(isEnabled: Boolean) {
        _viewStateFlow.value = _viewStateFlow.value.copy(bluetoothIsEnabled = isEnabled)
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

    fun showDialog(
        message: String,
        onConfirmation: () -> Unit
    ) {
        _showDialogFlow.value = ShowDialogState.ShownDialog(message, onConfirmation)
    }

    fun showExceptionDialog(exception: Exception) {
        viewModelScope.launch {
            _actionViewFlow.emit(ActionView.ShowErrorDialog(exception.message ?: "No Error Message"))
        }
    }

    fun showAlertDialog() {

    }

    fun closeDialog() {
        _showDialogFlow.value = ShowDialogState.Closed
    }

    sealed interface ShowDialogState {
        data object Closed: ShowDialogState
        data class ShownDialog(
            val message: String,
            val onConfirmation: () -> Unit
        ): ShowDialogState
    }

    sealed interface Action {
        data object EnableBluetooth: Action
        data object RequestBluetoothPermission: Action
    }

    sealed interface ActionView {
        data object Non: ActionView
        data object ShowBluetoothPermissionDialog: ActionView
        data object ShowEnableBluetoothDialog: ActionView
        data class ShowErrorDialog(val message: String): ActionView
    }
}