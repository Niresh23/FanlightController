package com.niresh23.fanlightcontroller.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.niresh23.fanlightcontroller.viewstate.DeviceConnectionState
import com.niresh23.fanlightcontroller.viewstate.StickActions
import kotlinx.coroutines.flow.MutableSharedFlow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FanlightViewModel(private val application: Application): AndroidViewModel(application) {

    private val _connectionStateFlow = MutableStateFlow<DeviceConnectionState>(DeviceConnectionState.Disconnected)
    val connectionStateFlow = _connectionStateFlow.asStateFlow()
    private val _actionFlow = MutableSharedFlow<StickActions>()
    val actionFlow = _actionFlow.asSharedFlow()

    fun connecting() {
        _connectionStateFlow.value = DeviceConnectionState.Connecting
    }

    fun connected() {
        _connectionStateFlow.value = DeviceConnectionState.Connected
    }

    fun disconnected() {
        _connectionStateFlow.value = DeviceConnectionState.Disconnected
    }

    fun startAudioVisualizer() {
        viewModelScope.launch {
            _actionFlow.emit(StickActions.StartAudioVisualizer)
        }
    }

    fun stopAudioVisualizer() {
        viewModelScope.launch {
            _actionFlow.emit(StickActions.StopAudioVisualizer)
        }
    }

    fun changeColor(color: Int) {
        viewModelScope.launch {
            _actionFlow.emit(StickActions.ChangeColor(color))
        }
    }

    fun changeBrightness(value: Float) {
        viewModelScope.launch {
            _actionFlow.emit(StickActions.ChangeBrightnessValue(value))
        }
    }

    fun changeFrequency(frequency: Float) {
        viewModelScope.launch {
            _actionFlow.emit(StickActions.ChangeVisualizationFrequency(frequency))
        }
    }
}