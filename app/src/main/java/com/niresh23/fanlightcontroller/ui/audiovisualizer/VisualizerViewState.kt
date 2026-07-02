package com.niresh23.fanlightcontroller.ui.audiovisualizer

import androidx.compose.runtime.Immutable
import com.niresh23.fanlightcontroller.visualizer.AudioVisualizer

@Immutable
data class VisualizerViewState(
    val frequency: Float,
    val param: AudioVisualizer.Param
)
