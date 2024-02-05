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

class FanlightViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _connectionStateFlow = MutableStateFlow<DeviceConnectionState>(DeviceConnectionState.Disconnected)
    val connectionStateFlow = _connectionStateFlow.asStateFlow()

    private var gattClient: BluetoothGatt? = null
    private var gattClientCallback: GattClientCallback? = null

    private var gatt: BluetoothGatt? = null

    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private var currentDevice: BluetoothDevice? = null

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