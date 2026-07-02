package com.niresh23.fanlightcontroller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niresh23.fanlightcontroller.ble.IBleServiceExecutor
import com.niresh23.fanlightcontroller.storage.ISettingsLocalStorage
import com.niresh23.fanlightcontroller.ui.audiovisualizer.VisualizerViewState
import com.niresh23.fanlightcontroller.ui.color.ColorViewState
import com.niresh23.fanlightcontroller.visualizer.AudioVisualizer
import kotlinx.coroutines.FlowPreview

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
class FanlightViewModel(
    private val settingsLocalStorage: ISettingsLocalStorage,
    private val serviceExecutor: IBleServiceExecutor
): ViewModel() {
    private val _visualizerViewStateFlow = MutableStateFlow(VisualizerViewState(
        frequency = 1f,
        param = AudioVisualizer.Param()
    ))
    val visualizerViewStateFlow = _visualizerViewStateFlow.asStateFlow()

    private val _colorViewStateFlow = MutableStateFlow(ColorViewState())
    val colorViewStateFlow = _colorViewStateFlow.asStateFlow()

    private val _brightnessValueFlow = MutableSharedFlow<Float>()
    private val _frequencyChangeFlow = MutableSharedFlow<Float>()
    private val _visualizerParamChangeFlow = MutableSharedFlow<AudioVisualizer.Param>()
    private val _colorChangedFlow = MutableSharedFlow<Int>()

    init {
        viewModelScope.launch {
            launch {
                settingsLocalStorage.brightnessFlow.collectLatest {
                    _brightnessValueFlow.emit(it)
                }
            }
            launch {
                settingsLocalStorage.frequencyFlow.collectLatest {
                    _frequencyChangeFlow.emit(it)
                }
            }
            launch {
                settingsLocalStorage.visualizerParamsFlow.collect {
                    _visualizerParamChangeFlow.emit(it)
                }
            }
            launch {
                _brightnessValueFlow.onEach { brightness ->
                    _colorViewStateFlow.value = _colorViewStateFlow.value.copy(brightness = brightness)
                }.debounce(50).collectLatest { brightness ->
                    settingsLocalStorage.setBrightness(brightness)
                    serviceExecutor.changeBrightness(brightness)
                }
            }
            launch {
                _frequencyChangeFlow.onEach { frequency ->
                    _visualizerViewStateFlow.value = _visualizerViewStateFlow.value.copy(frequency = frequency)
                }.debounce(50.milliseconds).collectLatest { frequency ->
                    settingsLocalStorage.setFrequency(frequency)
                    serviceExecutor.changeVisualizerFrequency(frequency)
                }
            }
            launch {
                _visualizerParamChangeFlow.onEach { param ->
                    _visualizerViewStateFlow.value = _visualizerViewStateFlow.value.copy(param = param)
                }.debounce(100).collect { param ->
                    settingsLocalStorage.setAudioVisualizerParams(param)
                    serviceExecutor.changeVisualizerParam(param)
                }
            }
            launch {
                _colorChangedFlow.sample(50).collect { color ->
                    serviceExecutor.changeColor(color)
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
        viewModelScope.launch {
            when(action) {
                is ControllerAction.Connect -> {
                    serviceExecutor.connect(action.address)
                }
                is ControllerAction.Disconnect -> {
                    serviceExecutor.disconnect(action.address)
                }
                is ControllerAction.ChangeBrightness -> {
                    _brightnessValueFlow.emit(action.value)
                }
                is ControllerAction.ChangeFrequency -> {
                    _frequencyChangeFlow.emit(action.value)
                }
                is ControllerAction.ChangeColor -> {
                    _colorChangedFlow.emit(action.color)
                }
                ControllerAction.StartVisualizer -> {
                    serviceExecutor.startAudioVisualizer()
                }
                ControllerAction.StopVisualizer -> {
                    serviceExecutor.stopAudioVisualizer()
                }
                is ControllerAction.RainbowClicked -> {
                    serviceExecutor.sendRainbowMessage()
                }
                is ControllerAction.ChangeVisualizerParam -> {
                    _visualizerParamChangeFlow.emit(action.param)
                }
                is ControllerAction.ServiceSelected -> {
                    serviceExecutor.serviceSelected(action.address, action.service)
                }
                is ControllerAction.CharacteristicSelected -> {
                    serviceExecutor.characteristicSelected(action.address, action.characteristic)
                }
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
    data class ChangeVisualizerParam(val param: AudioVisualizer.Param): ControllerAction
    data class CharacteristicSelected(val address: String, val characteristic: String): ControllerAction
    data class ServiceSelected(val address: String, val service: String): ControllerAction
    data class RainbowClicked(val address: String) : ControllerAction
}