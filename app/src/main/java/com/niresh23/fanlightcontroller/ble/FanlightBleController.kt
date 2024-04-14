package com.niresh23.fanlightcontroller.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.niresh23.fanlightcontroller.utils.Constants
import com.niresh23.fanlightcontroller.visualizer.AudioVisualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min

class FanlightBleController(private val context: Context, private val deviceCallback: DeviceCallback) {
    private var gattClient: BluetoothGatt? = null
    private var gattClientCallback: GattClientCallback? = null
    private var gatt: BluetoothGatt? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private var currentDevice: BluetoothDevice? = null
    private val sendQueue: Queue<ByteArray> = ConcurrentLinkedQueue()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var brightness = 1f
    private var currentColor: Int = 0

    private val audioVisualizer = AudioVisualizer()

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun connect(device: BluetoothDevice) {
        deviceCallback.onAction(DeviceActions.Connecting)
        scope.launch {
            audioVisualizer.colorSharedFlow.collectLatest {
                colorChange(it)
            }
        }
        currentDevice = device
        gattClientCallback = GattClientCallback()
        gattClient = device.connectGatt(context, false, gattClientCallback)
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun disconnect() {
        audioVisualizer.release()
        gattClient?.disconnect()
    }

    @RequiresPermission(value = Manifest.permission.RECORD_AUDIO)
    fun startVisualizer() {
        audioVisualizer.init(0)
    }

    fun setFrequencyValue(value: Float) {
        audioVisualizer.setCaptureRateDivider(value)
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
        messageCharacteristic?.let { characteristic ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt?.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                characteristic.value = value
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                gatt?.writeCharacteristic(characteristic)
            }
        }
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

    private inner class GattClientCallback : BluetoothGattCallback() {
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            this@FanlightBleController.gatt = gatt
            this@FanlightBleController.messageCharacteristic = characteristic
        }
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val isSuccess = status == BluetoothGatt.GATT_SUCCESS
            val isConnected = newState == BluetoothProfile.STATE_CONNECTED

            if (isSuccess && isConnected) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                gatt.discoverServices()
            } else {
                deviceCallback.onAction(DeviceActions.Disconnected)
            }
        }

        override fun onServicesDiscovered(discoveredGatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(discoveredGatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt = discoveredGatt
                val service = discoveredGatt.getService(Constants.SERVICE_UUID)
                if(service != null) {
                    messageCharacteristic = service.getCharacteristic(Constants.MESSAGE_UUID)
                    currentDevice?.let {
                        deviceCallback.onAction(DeviceActions.Connected)
                    }
                }
            }
        }
    }

    interface DeviceCallback {
        fun onAction(action: DeviceActions)
    }

    enum class DeviceActions {
        Connecting,
        Connected,
        Disconnected
    }
}