package com.niresh23.fanlightcontroller.visualizer

import android.Manifest
import android.media.audiofx.Visualizer
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class AudioVisualizer {
    private var visualizer: Visualizer? = null
    private val _colorSharedFlow = MutableSharedFlow<Int>()
    val colorSharedFlow = _colorSharedFlow.asSharedFlow()
    private var captureRate: Int = Visualizer.getMaxCaptureRate()
    private var divider = 1f
    private var param = Param()

    private val onDataCaptureListener = object : Visualizer.OnDataCaptureListener {
        override fun onWaveFormDataCapture(
            visualizer: Visualizer?,
            waveform: ByteArray?,
            samplingRate: Int
        ) {}

        override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
            fft ?: return

            val color =
                if (param.mode == Mode.MODERN) convertFFTtoIntRGBColor(fft)
                else convertFFTtoColor(fft)
            CoroutineScope(Dispatchers.IO).launch {
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
        if (this.divider == divider) return
        this.divider = divider
        visualizer?.enabled = false
        captureRate = (Visualizer.getMaxCaptureRate() * divider).toInt()
        visualizer?.setDataCaptureListener(onDataCaptureListener, captureRate, false, true)
        visualizer?.enabled = true
    }

    fun setVisualizerParam(param: Param) {
        this.param = param
    }

    fun release() {
        visualizer?.release()
        visualizer = null
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
            curMagnitudes[k] = curMagnitude + (prevMagnitudes[k] - curMagnitude) * smoothingFactor
        }
        return curMagnitudes.map { it / maxMagnitude }.toFloatArray()
    }

    private fun convertFFTtoIntRGBColor(fft: ByteArray): Int {
        val n: Int = fft.size
        val magnitudes = FloatArray(n / 2 + 1)
        val phases = FloatArray(n / 2 + 1)
        magnitudes[0] = abs(fft[0].toInt()).toFloat() // DC
        magnitudes[n / 2] = abs(fft[1].toInt()).toFloat() // Nyquist
        phases[0] = 0f.also { phases[n / 2] = it }

        for (k in 1 until n / 2) {
            val i = k * 2
            magnitudes[k] = hypot(fft[i].toDouble(), fft[i + 1].toDouble()).toFloat()
            phases[k] = atan2(fft[i + 1].toDouble(), fft[i].toDouble()).toFloat()
        }

        return fftToRGB(magnitudes, phases, param.bassAmplifier, param.midAmplifier, param.trembleAmplifier)
    }

    private fun fftToRGB(magnitudes: FloatArray,
                         phases: FloatArray,
                         bassAmplifier: Float,
                         midAmplifier: Float,
                         trembleAmplifier: Float
                         ): Int {
        val size = min(magnitudes.size, phases.size)
        val bassUp = (size * 0.2).toInt()
        val midUp = bassUp + (size * 0.4).toInt()
        val bassRange = 0..< bassUp
        val midRange = bassUp ..< midUp
        val trebleRange = midUp ..< size

        val (bassMag, bassPhase) = averageInRange(magnitudes, phases, bassRange)
        val (midMag, midPhase) = averageInRange(magnitudes, phases, midRange)
        val (trebleMag, treblePhase) = averageInRange(magnitudes, phases, trebleRange)

        val twinkleFactor = 0.5f + 0.5f * sin(bassPhase)
        val midTwinkleEffect = 0.5f + 0.5f * sin(midPhase)
        val trebleTwinkleEffect = 0.5f + 0.5f * sin(treblePhase)

        val maxBass = bassMag * bassAmplifier / (twinkleFactor * param.bassPhaseAmplifier)
        val maxMid = midMag * midAmplifier / (midTwinkleEffect * param.midPhaseAmplifier)
        val maxTreble = trebleMag * trembleAmplifier / (trebleTwinkleEffect * param.tremblePhaseAmplifier)

        val maxMagnitude = max(maxTreble, max(maxBass, maxMid))

        val r = (maxBass / maxMagnitude * 255).coerceIn(0f, 255f).toInt()
        val g = (maxMid / maxMagnitude * 255).coerceIn(0f, 255f).toInt()
        val b = (maxTreble / maxMagnitude * 255).coerceIn(0f, 255f).toInt()

        return (r shl 16) or (g shl 8) or b
    }

    private fun averageInRange(magArray: FloatArray, phasesArray: FloatArray, range: IntRange): Pair<Float, Float> {
        if (range.isEmpty() || range.last >= magArray.size) return 0f to 0f

        var sumMag = 0f
        var sumPhase = 0f
        for (i in range) {
            sumMag += magArray[i]
            sumPhase += phasesArray[i]
        }

        return sumMag / range.count() to sumPhase / range.count()
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

    @Immutable
    @Serializable
    data class Param(
        val captureDivider: Float = 1f,
        val bassAmplifier: Float = 1f,
        val midAmplifier: Float = 1f,
        val trembleAmplifier: Float = 1f,
        val bassPhaseAmplifier: Float = 1f,
        val midPhaseAmplifier: Float = 1f,
        val tremblePhaseAmplifier: Float = 1f,
        val mode: Mode = Mode.CLASSIC
    )

    enum class Mode {
        CLASSIC,
        MODERN
    }

    private fun Int.map(inLow: Int, inMax: Int, outLow: Int, outHigh: Int): Int {
        if ((inMax - inLow) == 0) return 0
        return (this - inLow) * (outHigh - outLow) / (inMax - inLow) + outLow
    }
}