package com.niresh23.fanlightcontroller.ui.audiovisualizer

import com.niresh23.fanlightcontroller.visualizer.AudioVisualizer

data class VisualizerViewState(
    val frequency: Float,
    val param: AudioVisualizer.Param
)
