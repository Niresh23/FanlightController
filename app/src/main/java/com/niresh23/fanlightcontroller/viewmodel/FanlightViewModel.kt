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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import com.niresh23.fanlightcontroller.utils.Constants.MESSAGE_UUID
import com.niresh23.fanlightcontroller.utils.Constants.SERVICE_UUID
import com.niresh23.fanlightcontroller.viewstate.DeviceConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min


class FanlightViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _connectionStateFlow = MutableStateFlow<DeviceConnectionState>(DeviceConnectionState.Disconnected)
    val connectionStateFlow = _connectionStateFlow.asStateFlow()

    private val _colorFlowState = MutableStateFlow(16711680)
    val colorFlowState = _colorFlowState

    private var gattClient: BluetoothGatt? = null
    private var gattClientCallback: GattClientCallback? = null

    private var gatt: BluetoothGatt? = null
    private var colorIdx = 0

    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private var currentDevice: BluetoothDevice? = null
    val sendQueue: Queue<ByteArray> = ConcurrentLinkedQueue()

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

    fun colorChangeMapping(color: Int) {
        val arrayOfByte = ByteArray(20)
        arrayOfByte[0] = 1
        arrayOfByte[1] = 15
        arrayOfByte[2] = 0
        arrayOfByte[3] = (0xFF0000 and color shr 16).toByte()
        arrayOfByte[4] = (0xFF00 and color shr 8).toByte()
        arrayOfByte[5] = (color and 0xFF).toByte()
        arrayOfByte[6] = 0
        arrayOfByte[7] = 0
        this.writeDataStrobe(arrayOfByte)
    }

//    fun colorChangeMapping() {
//        try {
//            var i = this.colorIdx
//            if (i == 0) {
//                i = 16721408
//            } else if(i == 1) {
//                val c = '豈'
//                i = c.code
//            } else if(i == 2) {
//                i = 275455
//            }
//            val arrayOfByte = ByteArray(20)
//            arrayOfByte[0] = 1
//            arrayOfByte[1] = 15
//            arrayOfByte[2] = 0
//            arrayOfByte[3] = (0xFF0000 and i shr 16).toByte()
//            arrayOfByte[4] = (0xFF00 and i shr 8).toByte()
//            arrayOfByte[5] = (i and 0xFF).toByte()
//            arrayOfByte[6] = 0
//            arrayOfByte[7] = 0
//            this.writeDataStrobe(arrayOfByte)
//            i = this.colorIdx + 1
//            this.colorIdx = i
//            if (i == 4) this.colorIdx = 0
//            Handler().postDelayed({ colorChangeMapping() }, 500L)
//        } catch (e: Exception) {
//
//        }
//    }


    fun writeDataStrobe(paramArrayOfByte: ByteArray) {
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

    private val seatData = IntArray(54)
}