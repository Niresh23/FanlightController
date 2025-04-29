package com.niresh23.fanlightcontroller.ble

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.niresh23.fanlightcontroller.ui.BluetoothControlService
import com.niresh23.fanlightcontroller.ui.connection.DeviceViewState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BleServiceExecutor(
    private val context: Context,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main
): IBleServiceExecutor {
    override val devicesViewState: Flow<List<DeviceViewState>>
        get() = _devicesViewState

    private val _devicesViewState = MutableSharedFlow<List<DeviceViewState>>()
    private val coroutineScope = CoroutineScope(coroutineDispatcher + SupervisorJob())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as BluetoothControlService.ServiceBinder
            val bleService = binder.getService()

            coroutineScope.launch {
                bleService.deviceViewStateFlow.collectLatest {
                    println("NRES -- devices from service in executor = $it")

                    _devicesViewState.emit(it)
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            coroutineScope.cancel()
        }
    }

    override fun onStart() {
        Intent(context, BluetoothControlService::class.java).also {
            context.startService(it)
            context.bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        context.unbindService(connection)
    }

    override fun connect(address: String) {
        Intent(
            context,
            BluetoothControlService::class.java
        ).also { intent ->
            intent.putExtra(
                BluetoothControlService.BLUETOOTH_DEVICE_KEY,
                address
            )
            intent.action =
                BluetoothControlService.Actions.Connect.toString()
            context.startService(intent)
            context.bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun disconnect(address: String) {
        Intent(context, BluetoothControlService::class.java).also {
            it.putExtra(BluetoothControlService.BLUETOOTH_DEVICE_KEY, address)
            it.action = BluetoothControlService.Actions.Disconnect.toString()
            context.startService(it)
        }
    }

    override fun changeColor(value: Int) {
        Intent(context, BluetoothControlService::class.java).also {
            it.putExtra(BluetoothControlService.COLOR_KEY, value)
            it.action = BluetoothControlService.Actions.ChangeColor.toString()
            context.startService(it)
        }
    }

    override fun startAudioVisualizer() {
        Intent(context, BluetoothControlService::class.java).also {
            it.action = BluetoothControlService.Actions.StartAudioVisualizer.toString()
            context.startService(it)
        }
    }

    override fun stopAudioVisualizer() {
        Intent(context, BluetoothControlService::class.java).also {
            it.action = BluetoothControlService.Actions.StopAudioVisualizer.toString()
            context.startService(it)
        }
    }

    override fun changeVisualizerFrequency(value: Float) {
        Intent(context, BluetoothControlService::class.java).also {
            it.action = BluetoothControlService.Actions.ChangeVisualizerFrequency.toString()
            it.putExtra(BluetoothControlService.FREQUENCY_VALUE_KEY, value)
            context.startService(it)
        }
    }

    override fun changeBrightness(value: Float) {
        Intent(context, BluetoothControlService::class.java).also {
            it.action = BluetoothControlService.Actions.ChangeBrightness.toString()
            it.putExtra(BluetoothControlService.BRIGHTNESS_VALUE_KEY, value)
            context.startService(it)
        }
    }

    override fun addDevices(devices: Collection<String>) {
        Intent(context, BluetoothControlService::class.java).also {
            it.action = BluetoothControlService.Actions.AddDevices.toString()
            it.putExtra(BluetoothControlService.ADD_DEVICES_KEY, devices.toTypedArray())
            context.startService(it)
        }
    }
}