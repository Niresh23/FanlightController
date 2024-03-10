package com.niresh23.fanlightcontroller.viewmodel

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niresh23.fanlightcontroller.utils.Constants.MESSAGE_UUID
import com.niresh23.fanlightcontroller.utils.Constants.SERVICE_UUID
import com.niresh23.fanlightcontroller.viewstate.DeviceConnectionState
import com.niresh23.fanlightcontroller.visualizer.AudioVisualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.concatWith
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min


class FanlightViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _connectionStateFlow = MutableStateFlow<DeviceConnectionState>(DeviceConnectionState.Disconnected)
    val connectionStateFlow = _connectionStateFlow.asStateFlow()

    private val _colorFlowState = MutableStateFlow(0xFF0000)
    val colorFlowState = _colorFlowState

    private var gattClient: BluetoothGatt? = null
    private var gattClientCallback: GattClientCallback? = null

    private var gatt: BluetoothGatt? = null

    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private var currentDevice: BluetoothDevice? = null
    private val sendQueue: Queue<ByteArray> = ConcurrentLinkedQueue()
    private val audioVisualizer = AudioVisualizer()

    fun connect(device: BluetoothDevice) {
        _connectionStateFlow.value = DeviceConnectionState.Connecting
        currentDevice = device
        gattClientCallback = GattClientCallback()
        if (ActivityCompat.checkSelfPermission(
                application,
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
        gattClient = device.connectGatt(application, false, gattClientCallback)

        viewModelScope.launch {
            _connectionStateFlow.collectLatest {

                if (it is DeviceConnectionState.Connected) {
                    launch {
                        _colorFlowState.collectLatest { color ->
                            colorChangeMapping(color)
                        }
                    }
                    launch {
                        audioVisualizer.colorSharedFlow.collectLatest { color ->
                            colorChangeMapping(color)
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun sendMessage(message: String): Int {
        messageCharacteristic?.let { characteristic ->
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

            val messageBytes = message.toByteArray(Charsets.UTF_8)
            characteristic.value = messageBytes
            gatt?.let {
                if (ActivityCompat.checkSelfPermission(
                        application,
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
                    return BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION
                } else {
                    return it.writeCharacteristic(characteristic, messageBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                }
            }
        }

        return BluetoothStatusCodes.ERROR_UNKNOWN
    }

    fun writeData(paramArrayOfbyte: ByteArray) {
        var i = 0
        while (i < paramArrayOfbyte.size) {
            val j = i + 20
            val arrayOfByte =
                paramArrayOfbyte.copyOfRange(i, j.coerceAtMost(paramArrayOfbyte.size))
            this.sendQueue.add(arrayOfByte)
            i = j
        }

        reQuest()
    }


    fun testFunction(color: Int) {
        val resultArray = ByteArray(200)

        for (i in 0 ..9) {
            val colorX = color * i
            val arrayOfByte = ByteArray(20)
            arrayOfByte[0] = i.toByte()
            arrayOfByte[1] = 15
            arrayOfByte[2] = 0
            arrayOfByte[3] = (0xFF0000 and colorX shr 16).toByte()
            arrayOfByte[4] = (0xFF00 and colorX shr 8).toByte()
            arrayOfByte[5] = (colorX and 0xFF).toByte()
            arrayOfByte.copyInto(resultArray, arrayOfByte.size * i)
        }

        this.writeDataStrobe(resultArray)
    }

    fun colorChangeMapping(color: Int) {
        val arrayOfByte = ByteArray(20)
        arrayOfByte[0] = 1
        arrayOfByte[1] = 15
        arrayOfByte[2] = 0
        arrayOfByte[3] = (0xFF0000 and color shr 16).toByte()
        arrayOfByte[4] = (0xFF00 and color shr 8).toByte()
        arrayOfByte[5] = (color and 0xFF).toByte()

        this.writeDataStrobe(arrayOfByte)
    }



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

    private fun reQuest() {
        if (this.sendQueue.isEmpty()) return
        this.messageCharacteristic?.value = this.sendQueue.poll()
        messageCharacteristic?.let {
            sendMessage(it)
        }
    }

    private fun reQuestStrobe() {
        if (sendQueue.isEmpty()) return
        this.messageCharacteristic?.value = sendQueue.poll( )
        messageCharacteristic?.let {
            sendMessage(it)
        }
    }

    private fun sendMessage(messageChar: BluetoothGattCharacteristic) {
        messageChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        gatt?.let {
            if (ActivityCompat.checkSelfPermission(
                    application,
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
                BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION
            } else {
                it.writeCharacteristic(messageChar)
            }
        }
    }

    @RequiresPermission(value = Manifest.permission.RECORD_AUDIO)
    fun startVisualizer() {
        audioVisualizer.init(0)
    }

    fun stopVisualizer() {
        audioVisualizer.release()
    }

    override fun onCleared() {
        super.onCleared()
        audioVisualizer.release()
    }

    private inner class GattClientCallback : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val isSuccess = status == BluetoothGatt.GATT_SUCCESS
            val isConnected = newState == BluetoothProfile.STATE_CONNECTED

            if (isSuccess && isConnected) {
                if (ActivityCompat.checkSelfPermission(
                        application,
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
                _connectionStateFlow.value = DeviceConnectionState.Disconnected
            }
        }

        override fun onServicesDiscovered(discoveredGatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(discoveredGatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt = discoveredGatt
                val service = discoveredGatt.getService(SERVICE_UUID)
                if(service != null) {
                    messageCharacteristic = service.getCharacteristic(MESSAGE_UUID)
                    currentDevice?.let {
                        _connectionStateFlow.value = DeviceConnectionState.Connected(it)
                    }
                }
            }
        }
    }
}