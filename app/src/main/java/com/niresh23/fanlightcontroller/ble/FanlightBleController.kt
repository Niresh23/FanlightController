package com.niresh23.fanlightcontroller.ble

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.niresh23.fanlightcontroller.visualizer.AudioVisualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min

@OptIn(ObsoleteCoroutinesApi::class)
class FanlightBleController(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val bleAdapter = BleAdapter(context, scope)
    val bleDeviceEventFlow: Flow<DeviceEvent> = bleAdapter.bleDeviceEventFlow
    private val sendQueue: Queue<ByteArray> = ConcurrentLinkedQueue()
    private var brightness = 1f
    private var currentColor: Int = 0
    val isConnected: Boolean
        get() = bleAdapter.isConnected

    val audioVisualizer = AudioVisualizer()

    init {
        scope.launch {
            ticker(1000, context = coroutineContext).receiveAsFlow().collectLatest {
                bleAdapter.requestBatteryLevel()
            }
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun connect(deviceAddress: String) {
        bleAdapter.connect(deviceAddress)
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun disconnect() {
        audioVisualizer.release()
        bleAdapter.disconnect()
    }

    @RequiresPermission(value = Manifest.permission.RECORD_AUDIO)
    fun startVisualizer() {
        audioVisualizer.init(0)
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun setBrightness(value: Float) {
        if (value in 0f.. 1f) {
            brightness = value
            colorChange(currentColor)
        }
    }

    fun stopVisualizer() {
        audioVisualizer.release()
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun colorChange(color: Int) {
        currentColor = color
        val arrayOfByte = ByteArray(20)
        arrayOfByte[0] = 1
        arrayOfByte[1] = 15
        arrayOfByte[2] = 0
        arrayOfByte[3] = ((0xFF0000 and currentColor shr 16) * brightness).toInt().toByte()
        arrayOfByte[4] = ((0xFF00 and currentColor shr 8) * brightness).toInt().toByte()
        arrayOfByte[5] = ((currentColor and 0xFF) * brightness).toInt().toByte()

        this.writeDataStrobe(arrayOfByte)
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun stateChanged(
        color: Int,
        brightness: Float,
        frequency: Float,
        param: AudioVisualizer.Param
    ) {
        this.brightness = brightness
        audioVisualizer.setCaptureRateDivider(frequency)
        audioVisualizer.setVisualizerParam(param)
        colorChange(color)
    }

    fun release() {
        audioVisualizer.release()
        bleAdapter.release()
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun writeDataStrobe(paramArrayOfByte: ByteArray) {
        var i = 0
        while (i < paramArrayOfByte.size) {
            val j = i + 20
            val arrayOfByte =
                paramArrayOfByte.copyOfRange(i, min(j, paramArrayOfByte.size))
            sendQueue.add(arrayOfByte)
            i = j
        }

        reQuestStrobe()
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun reQuestStrobe() {
        if (this.sendQueue.isEmpty()) {return}
        this.sendQueue.poll()?.let {
            sendMessage(it)
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun sendMessage(value: ByteArray) {
        bleAdapter.sendMessage(value)
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun writeData(paramArrayOfByte: ByteArray) {
        var i = 0
        while (i < paramArrayOfByte.size) {
            val j = i + 20
            val arrayOfByte =
                paramArrayOfByte.copyOfRange(i, j.coerceAtMost(paramArrayOfByte.size))
            this.sendQueue.add(arrayOfByte)
            i = j
        }

        reQuest()
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun reQuest() {
        if (this.sendQueue.isEmpty()) return
        this.sendQueue.poll()?.let {
            sendMessage(it)
        }
    }
}

sealed interface DeviceEvent {
    data class Connecting(val address: String): DeviceEvent
    data class Connected(val address: String): DeviceEvent
    data class Disconnecting(val address: String): DeviceEvent
    data class Disconnected(val address: String): DeviceEvent
    data class BatteryLevel(val address: String, val level: Int): DeviceEvent
}