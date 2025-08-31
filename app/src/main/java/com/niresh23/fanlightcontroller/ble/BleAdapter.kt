package com.niresh23.fanlightcontroller.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.niresh23.fanlightcontroller.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class BleAdapter(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : IBleAdapter {
    override val bleDeviceEventFlow: Flow<DeviceEvent>
        get() = _bleDeviceEventFlow

    private val _bleDeviceEventFlow = MutableSharedFlow<DeviceEvent>()
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var gattClient: BluetoothGatt? = null
    private var gatt: BluetoothGatt? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private var batteryLevelCharacteristics: BluetoothGattCharacteristic? = null
    var isConnected = false
        private set


    private val gattClientCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            this@BleAdapter.gatt = gatt
            this@BleAdapter.messageCharacteristic = characteristic
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            scope.launch {
                val isSuccess = status == BluetoothGatt.GATT_SUCCESS
                if (isSuccess && newState == BluetoothProfile.STATE_CONNECTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@launch
                    }
                    isConnected = true
                    gatt.discoverServices()
                } else if(isSuccess && newState == BluetoothProfile.STATE_CONNECTING) {
                    _bleDeviceEventFlow.emit(DeviceEvent.Connecting(gatt.device.address))
                } else if(isSuccess && newState == BluetoothProfile.STATE_DISCONNECTING) {
                    _bleDeviceEventFlow.emit(DeviceEvent.Disconnecting(gatt.device.address))
                } else {
                    isConnected = false
                    _bleDeviceEventFlow.emit(DeviceEvent.Disconnected(gatt.device.address))
                }
            }
        }

        override fun onServicesDiscovered(discoveredGatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(discoveredGatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt = discoveredGatt
                val batteryService = discoveredGatt.getService(Constants.BATTERY_SERVICE_UUID)
                val service = discoveredGatt.getService(Constants.SERVICE_UUID)

                if(service != null) {
                    messageCharacteristic = service.getCharacteristic(Constants.MESSAGE_UUID)
                    scope.launch {
                        _bleDeviceEventFlow.emit(DeviceEvent.Connected(discoveredGatt.device.address))
                    }
                }

                if (batteryService != null) {
                    batteryLevelCharacteristics = batteryService.getCharacteristic(Constants.BATTERY_LEVEL_UUID)
                }

                requestBatteryLevel()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            val batteryLevel =
                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)

            scope.launch {
                _bleDeviceEventFlow.emit(DeviceEvent.BatteryLevel(gatt.device.address, batteryLevel))
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connect(address: String) {
        val device = adapter?.getRemoteDevice(address)
        gattClient = device?.connectGatt(context, true, gattClientCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun disconnect() {
        gattClient?.disconnect()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun sendMessage(message: ByteArray) {
        messageCharacteristic?.let { characteristic ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt?.writeCharacteristic(characteristic, message, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                characteristic.value = message
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                gatt?.writeCharacteristic(characteristic)
            }
        }
    }

    override fun requestBatteryLevel() {
        batteryLevelCharacteristics?.let {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                gatt?.readCharacteristic(batteryLevelCharacteristics)
            }
        }
    }

    override fun release() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            gatt?.close()
        }

        gattClient = null
        gatt = null
        messageCharacteristic = null
        batteryLevelCharacteristics = null
    }
}