package com.niresh23.fanlightcontroller.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.Log
import java.util.Queue
import java.util.UUID


class BleControllerFanlight: HandlerCode {
    private var SCAN_PERIOD: Long = 0
    private var instance: BleControllerFanlight? = null
    private var ColorChar: BluetoothGattCharacteristic? = null
    private var NOW_MAPPING = 0
    private var SCAN_TIMEOUT = 0L
    private var TXChar: BluetoothGattCharacteristic? = null
    private var activity: Activity? = null
    private var ckHandler: Handler? = null
    private val isTimeout = false
    private val mCharFirmwareRevision: BluetoothGattCharacteristic? = null
    private val mGatt: BluetoothGatt? = null
    private val mHandler: Handler? = null
    private val mLEScanner: BluetoothLeScanner? = null
    private val mainHandler: Handler? = null
    private val sendQueue: Queue<ByteArray>? = null
    private val context: Context? = null
    private val firmwareRevision: String? = null

    var DEVICENAME: String? = null
    var ColorUUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b19")
    var RXDESCRIPTERUUID: UUID? = null
    var RXDUUID: UUID? = null
    var ServiceUUID: UUID? = null
    var TXDESCRIPTERUUID: UUID? = null
    var TXDUUID: UUID? = null
    var bluetoothAdapter: BluetoothAdapter? = null
    var isConnected = false
    var isScaning = false
    var scanList: ArrayList<ScanResultModel>? = null
    var device: BluetoothDevice? = null

    var ckRunnable: Runnable = object : Runnable {
        override fun run() {
            if (this@BleControllerFanlight.device == null) {
                this@BleControllerFanlight.ckHandler?.postDelayed(
                    this,
                    SCAN_PERIOD
                )
            } else {
                this@BleControllerFanlight.scanLeDeviceStop()
            }
        }
    }

    private val mScanCallback: ScanCallback = object : ScanCallback() {
        override fun onBatchScanResults(results: List<ScanResult?>?) {}
        override fun onScanFailed(errorCode: Int) {}
        override fun onScanResult(type: Int, paramScanResult: ScanResult) {
            super.onScanResult(type, paramScanResult)

            if (paramScanResult.device != null) {
                this@BleControllerFanlight.addScanResult(
                    paramScanResult.device,
                    paramScanResult.rssi
                )
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            
        }
    }

    @SuppressLint("MissingPermission")
    private fun addScanResult(paramBluetoothDevice: BluetoothDevice, rssi: Int) {
        var paramInt = rssi
        var bool1: Boolean = true

        val stringBuilder = StringBuilder()
        stringBuilder.append("deviceName : ")
        stringBuilder.append(paramBluetoothDevice.name)
        stringBuilder.append(", Addr : ")
        stringBuilder.append(paramBluetoothDevice.address)
        stringBuilder.append(", UUIDS : ")
        stringBuilder.append(paramBluetoothDevice.uuids)

        Log.d("minwoo", stringBuilder.toString())

        val str = paramBluetoothDevice.name
        if (str != DEVICENAME) return

        val iterator = scanList!!.iterator()

        while (iterator.hasNext()) {
            val scanResultModel = iterator.next()
            if (str == DEVICENAME && paramBluetoothDevice.address == scanResultModel.device!!
                    .address
            ) {
                scanResultModel.rssi = paramInt
                bool1 = false
                break
            }
        }

        if (bool1) {
            val scanResultModel = ScanResultModel()
            scanResultModel.device = paramBluetoothDevice
            scanResultModel.rssi = paramInt
            val stringBuilder1 = StringBuilder()
            stringBuilder1.append("------ DEVICENAME : ")
            stringBuilder1.append(str)
            stringBuilder1.append(", address : ")
            stringBuilder1.append(paramBluetoothDevice.address)
            stringBuilder1.append(", Rssi : ")
            stringBuilder1.append(paramInt)
            stringBuilder1.append(" -------")
            Log.d("minwoo", stringBuilder1.toString())
            scanList?.add(scanResultModel)
        }

        paramInt = -200

        for (scanResultModel in scanList!!) {
            if (scanResultModel.rssi > paramInt) {
                device = scanResultModel.device
                paramInt = scanResultModel.rssi
            }
        }

        if (device == null || paramInt <= -68) device = null
    }


    @SuppressLint("MissingPermission")
    fun scanLeDeviceStop() {
        var b: Int = 0
        this.ckHandler?.removeCallbacks(this.ckRunnable)

        if (!this.isScaning)
            return

        this.mLEScanner?.stopScan(this.mScanCallback)
        this.isScaning = false

        b = if (this.isTimeout) {
            60
        } else if (this.device == null) {
            20
        } else {
            10
        }

        sendMessage(b, emptyList())
    }

    private fun sendMessage(what: Int, param: List<String>) {
        val message = Message()
        message.what = what
        message.obj = param
        mainHandler?.sendMessage(message)
    }


}