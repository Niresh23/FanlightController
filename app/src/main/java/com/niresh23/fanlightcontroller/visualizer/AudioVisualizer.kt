package com.niresh23.fanlightcontroller.visualizer

import android.Manifest
import android.media.audiofx.Visualizer
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

class AudioVisualizer {
    private var visualizer: Visualizer? = null
    private val _colorSharedFlow = MutableSharedFlow<Int>()
    val colorSharedFlow = _colorSharedFlow.asSharedFlow()
    private var captureRate: Int = Visualizer.getMaxCaptureRate()

    private val onDataCaptureListener = object : Visualizer.OnDataCaptureListener {
        override fun onWaveFormDataCapture(
            visualizer: Visualizer?,
            waveform: ByteArray?,
            samplingRate: Int
        ) {}

        override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
            fft ?: return

            val color = convertFFTtoColor(fft)
            CoroutineScope(Dispatchers.Main).launch {
                _colorSharedFlow.emit(color)
            }
        }
    }

    @RequiresPermission(value = Manifest.permission.RECORD_AUDIO)
    fun init(sessionId: Int) {
        visualizer = Visualizer(sessionId)
        visualizer?.enabled = false
        visualizer?.setDataCaptureListener(onDataCaptureListener, captureRate, false, true)
        visualizer?.enabled = true
    }

    fun setCaptureRateDivider(divider: Float) {
        visualizer?.enabled = false
        captureRate = (Visualizer.getMaxCaptureRate() / divider).toInt()
        visualizer?.setDataCaptureListener(onDataCaptureListener, captureRate, false, true)
        visualizer?.enabled = true

    }

    fun release() {
        visualizer?.enabled = false
        visualizer?.release()
    }

    private var magnitudes = floatArrayOf()
    private val maxMagnitude = calculateMagnitude(127f, 127f)
    private val smoothingFactor = 0.2f

    private fun convertMagnitudeToColor(magnitude: FloatArray): Int {
        val rArray = FloatArray(magnitude.size / 3)
        val gArray = FloatArray(magnitude.size / 3)
        val bArray = FloatArray(magnitude.size / 3)

        magnitude.copyInto(rArray, 0, 0, rArray.size)
        magnitude.copyInto(gArray, 0, rArray.size, rArray.size + gArray.size)
        magnitude.copyInto(bArray, 0, rArray.size + gArray.size, rArray.size + gArray.size + bArray.size)

        val red = (255 * (rArray.sum() / rArray.size)).toInt() shl 16
        val green = (255 * (gArray.sum() / gArray.size)).toInt() shl 8
        val blue = (255 * (bArray.sum() / bArray.size)).toInt()

        val delta = min(255 - red, min(255 - green, 255 - blue))

        val redHex = (red + delta) shl 16
        val greenHex = (green + delta) shl 8
        val blueHex = (blue + delta)

        return redHex or greenHex or blueHex
    }

//    private fun convertFFTtoMagnitudes(fft: ByteArray): FloatArray {
//        if (fft.isEmpty()) {
//            return floatArrayOf()
//        }
//
//        val n: Int = fft.size / FFT_NEEDED_PORTION
//        val curMagnitudes = FloatArray(n / 2)
//
//        var prevMagnitudes = magnitudes
//        if (prevMagnitudes.isEmpty()) {
//            prevMagnitudes = FloatArray(n)
//        }
//
//        for (k in 0 until n / 2 - 1) {
//            val index = k * FFT_STEP + FFT_OFFSET
//            val real: Byte = fft[index]
//            val imaginary: Byte = fft[index + 1]
//
//            val curMagnitude = calculateMagnitude(real.toFloat(), imaginary.toFloat())
//            curMagnitudes[k] = curMagnitude + (prevMagnitudes[k] - curMagnitude) * smoothingFactor
//        }
//        return curMagnitudes.map { it / maxMagnitude }.toFloatArray()
//    }

    private fun convertFFTtoMagnitudes(fft: ByteArray): FloatArray {
        if (fft.isEmpty()) {
            return floatArrayOf()
        }

        val n: Int = fft.size / FFT_NEEDED_PORTION
        val curMagnitudes = FloatArray(n / 2)

        var prevMagnitudes = magnitudes
        if (prevMagnitudes.isEmpty()) {
            prevMagnitudes = FloatArray(n)
        }

        for (k in 0 until n / 2 - 1) {
            val index = k * FFT_STEP + FFT_OFFSET
            val real: Byte = fft[index]
            val imaginary: Byte = fft[index + 1]

            val curMagnitude = calculateMagnitude(real.toFloat(), imaginary.toFloat())
            curMagnitudes[k] = curMagnitude
        }

        return curMagnitudes.map { it / maxMagnitude }.toFloatArray()
    }

    private fun convertFFTtoColor(fft: ByteArray): Int {
        val fftNormalized = normalizeArray(fft)

        var min = Int.MAX_VALUE
        var max = Int.MIN_VALUE


        val rArray = IntArray(fftNormalized.size / 3)
        val gArray = IntArray(fftNormalized.size / 3)
        val bArray = IntArray(fftNormalized.size / 3)

        fftNormalized.copyInto(rArray, 0, 0, rArray.size)
        fftNormalized.copyInto(gArray, 0, rArray.size, rArray.size + gArray.size)
        fftNormalized.copyInto(bArray, 0, rArray.size + gArray.size, rArray.size + gArray.size + bArray.size)

        val r = rArray.sum()
        val g = gArray.sum()
        val b = bArray.sum()

        val values = intArrayOf(r, g, b)
        val minValue = values.min()
        val maxValue = values.max()

        min = min(min, minValue)
        max = max(max, maxValue)

        val redHex = (r.map(min, max, MIN_COLOR_VALUE, MAX_COLOR_VALUE)) shl 16
        val greenHex = (g.map(min, max, MIN_COLOR_VALUE, MAX_COLOR_VALUE)) shl 8
        val blueHex = (b.map(min, max, MIN_COLOR_VALUE, MAX_COLOR_VALUE))

        return redHex or greenHex or blueHex
    }

    private fun normalizeArray(fft: ByteArray): IntArray {
        val min = fft.min()

        return if (min < 0) {
            fft.map { it.toInt() + min.toInt().absoluteValue }.toIntArray()
        } else {
            fft.map { it.toInt() }.toIntArray()
        }
    }

    private fun calculateMagnitude(r: Float, i: Float) =
        if (i == 0f && r == 0f) 0f else 10 * log10(r * r + i * i)
    companion object {
        private const val FFT_STEP = 1
        private const val FFT_OFFSET = 0
        private const val FFT_NEEDED_PORTION = 1 // 1/3
        private const val MIN_COLOR_VALUE = 0
        private const val MAX_COLOR_VALUE = 255
    }

    data class Color(
        val red: Float, // value 0..1f
        val green: Float, // value 0..1f
        val blue: Float // value 0..1f
    )

    private fun Int.map(inLow: Int, inMax: Int, outLow: Int, outHigh: Int): Int {
        if ((inMax - inLow) == 0) return 0
        return (this - inLow) * (outHigh - outLow) / (inMax - inLow) + outLow
    }
}