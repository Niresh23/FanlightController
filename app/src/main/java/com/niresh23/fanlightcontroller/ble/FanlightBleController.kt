package com.niresh23.fanlightcontroller.ble

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.niresh23.fanlightcontroller.utils.Constants
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

    fun selectService(service: String) {
        bleAdapter.selectService(service)
    }

    fun selectCharacteristic(characteristic: String) {
        bleAdapter.selectCharacteristic(characteristic)
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

        val arrayOfByte = if (bleAdapter.getServiceUUID() == Constants.SERVICE_UUID) {
            ByteArray(20).also {
                it[0] = 1
                it[1] = 15
                it[2] = 0
                it[3] = ((0xFF0000 and currentColor shr 16) * brightness).toInt().toByte()
                it[4] = ((0xFF00 and currentColor shr 8) * brightness).toInt().toByte()
                it[5] = ((currentColor and 0xFF) * brightness).toInt().toByte()
            }
        } else if (bleAdapter.getServiceUUID() == Constants.SERVICE_UUID_2) {
            var b = 11.toByte()
            ByteArray(11).also {
                var b1: Byte = 2
                it[0] = 1
                it[1] = 1
                it[2] = b
                it[3] = 0
                it[4] = 0
                it[5] = ((0xFF0000 and currentColor shr 16) * brightness).toInt().toByte()
                it[6] = ((0xFF00 and currentColor shr 8) * brightness).toInt().toByte()
                it[7] = ((currentColor and 0xFF) * brightness).toInt().toByte()
                it[8] = 0
                it[9] = 0

                b = 0
                while (b1 < 11) {
                    b = (b + it[b1.toInt()]).toByte()
                    b1++
                }

                it[10] = b
            }
        } else {
            ByteArray(0)
        }

        this.writeDataStrobe(arrayOfByte)
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun sendRainbowMessage() {
        val arrayOfByte = ByteArray(20)
        arrayOfByte[0] = 1
        arrayOfByte[1] = 15
        arrayOfByte[2] = 0
        arrayOfByte[3] = 7
        arrayOfByte[4] = 13
        arrayOfByte[5] = 0

        this.writeDataStrobe(arrayOfByte, true)
    }
    // For rainbow mode
    // [1, 15, 0, 7, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

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
    private fun writeDataStrobe(paramArrayOfByte: ByteArray, isRainbow: Boolean = false) {
        var i = 0
        while (i < paramArrayOfByte.size) {
            val j = i + 20
            val arrayOfByte =
                paramArrayOfByte.copyOfRange(i, min(j, paramArrayOfByte.size))
            sendQueue.add(arrayOfByte)
            i = j
        }

        reQuestStrobe(isRainbow)
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun reQuestStrobe(isRainbow: Boolean) {
        if (this.sendQueue.isEmpty()) {return}
        this.sendQueue.poll()?.let {
            sendMessage(it, isRainbow)
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun sendMessage(value: ByteArray, isRainbow: Boolean) {
        if (isRainbow) {
            bleAdapter.sendRainbowMessage(value)
        } else {
            bleAdapter.sendMessage(value)
        }
    }
}

sealed interface DeviceEvent {
    val address: String
    data class Connecting(override val address: String): DeviceEvent
    data class Connected(override val address: String, val supportsRainbow: Boolean): DeviceEvent
    data class Disconnecting(override val address: String): DeviceEvent
    data class Disconnected(override val address: String): DeviceEvent
    data class BatteryLevel(override val address: String, val level: Int): DeviceEvent
    data class Services(override val address: String, val services: List<String>): DeviceEvent
    data class Characteristics(
        override val address: String,
        val service: String,
        val characteristics: List<String>,
    ): DeviceEvent
    data class CharacteristicSelected(
        override val address: String,
        val service: String,
        val characteristic: String,
    ): DeviceEvent
}