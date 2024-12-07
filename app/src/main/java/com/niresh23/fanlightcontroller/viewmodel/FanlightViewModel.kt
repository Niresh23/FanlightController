package com.niresh23.fanlightcontroller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niresh23.fanlightcontroller.ble.DeviceEvent
import com.niresh23.fanlightcontroller.ble.IBleServiceExecutor
import com.niresh23.fanlightcontroller.storage.ISettingsLocalStorage

import kotlinx.coroutines.flow.MutableSharedFlow

import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FanlightViewModel(
    private val settingsLocalStorage: ISettingsLocalStorage,
    private val serviceExecutor: IBleServiceExecutor
): ViewModel() {
    private val _eventFlow  = MutableSharedFlow<DeviceEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            launch {
                settingsLocalStorage.brightnessFlow.collectLatest {
                    serviceExecutor.changeBrightness(it)
                }
            }
            launch {
                settingsLocalStorage.frequencyFlow.collectLatest {
                    serviceExecutor.changeVisualizerFrequency(it)
                }
            }
            launch {
                serviceExecutor.serviceEventFlow.collectLatest {
                    _eventFlow.emit(it)
                }
            }
        }
    }

    fun onStart() {
        serviceExecutor.onStart()
    }

    fun onStop() {
        serviceExecutor.onStop()
    }

    fun onAction(action: ControllerAction) {
        when(action) {
            is ControllerAction.Connect -> {
                serviceExecutor.connect(action.address)
            }
            is ControllerAction.Disconnect -> {
                serviceExecutor.disconnect(action.address)
            }
            is ControllerAction.ChangeBrightness -> {
                viewModelScope.launch {
                    settingsLocalStorage.setBrightness(action.value)
                }
            }
            is ControllerAction.ChangeFrequency -> {
                viewModelScope.launch {
                    settingsLocalStorage.setFrequency(action.value)
                }
            }
            is ControllerAction.ChangeColor -> {
                serviceExecutor.changeColor(action.color)
            }
            ControllerAction.StartVisualizer -> {
                serviceExecutor.startAudioVisualizer()
            }
            ControllerAction.StopVisualizer -> {
                serviceExecutor.stopAudioVisualizer()
            }
        }
    }
}

sealed interface ControllerAction {
    data class Connect(val address: String): ControllerAction
    data class Disconnect(val address: String): ControllerAction
    data object StartVisualizer: ControllerAction
    data object StopVisualizer: ControllerAction
    data class ChangeColor(val color: Int): ControllerAction
    data class ChangeBrightness(val value: Float): ControllerAction
    data class ChangeFrequency(val value: Float): ControllerAction
}