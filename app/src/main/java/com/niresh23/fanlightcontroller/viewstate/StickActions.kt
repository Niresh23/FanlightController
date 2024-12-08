package com.niresh23.fanlightcontroller.viewstate

sealed interface StickActions {
    data class Connect(val address: String): StickActions
    data class Disconnect(val address: String) : StickActions
    data object StartAudioVisualizer : StickActions
    data object StopAudioVisualizer : StickActions
    data class ChangeColor(val color: Int) : StickActions
    data class ChangeVisualizationFrequency(val value: Float) : StickActions
    data class ChangeBrightnessValue(val brightness: Float) : StickActions // 0.. 1.0
}