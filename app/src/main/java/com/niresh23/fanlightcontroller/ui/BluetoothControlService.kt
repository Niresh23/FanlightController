package com.niresh23.fanlightcontroller.ui

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.niresh23.fanlightcontroller.R
import com.niresh23.fanlightcontroller.ble.DeviceEvent
import com.niresh23.fanlightcontroller.ble.FanlightBleController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothControlService: Service() {
    companion object {
        private const val NOTIFICATION_ID = 111
        const val NOTIFICATION_CHANNEL_ID = "audio_visualizer_channel_id"
        const val NOTIFICATION_CHANNEL_NAME = "audio_visualizer_notification_channel"
        const val BLUETOOTH_DEVICE_KEY = "bluetooth_device_key"
        const val COLOR_KEY = "color_key"
        const val FREQUENCY_VALUE_KEY = "frequency_value_key"
        const val BRIGHTNESS_VALUE_KEY = "brightness_value_key"
    }

    private val controllerMap = HashMap<String, FanlightBleController>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val _actionFlow = MutableSharedFlow<Flow<DeviceEvent>>()
    val actionFlow: Flow<DeviceEvent> = _actionFlow.asSharedFlow().flattenConcat()
    private val binder = ServiceBinder()

    private val _brightnessStateFlow = MutableStateFlow(1f)
    private val _frequencyStateFlow = MutableStateFlow(1f)
    private val _colorStateFlow = MutableStateFlow(0)

    init {
        scope.launch {
            combine(_colorStateFlow, _frequencyStateFlow, _brightnessStateFlow) { color, frequency, brightness ->
                ColorState(color = color, frequency = frequency, brightness = brightness)
            }.collect { colorState ->
                controllerMap.values.forEach {
                    it.stateChanged(colorState.color, colorState.brightness, colorState.frequency)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        scope.launch {
            actionFlow.collectLatest { event ->
                when (event) {
                    is DeviceEvent.Disconnected -> {
                        controllerMap.values.forEach {
                            if (it.isConnected) {
                                return@collectLatest
                            }
                        }

                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }

                    is DeviceEvent.Connected -> {
                        controllerMap.values.forEach {
                            if (_colorStateFlow.value != 0) {
                                scope.launch {
                                    delay(200L)
                                    it.stateChanged(
                                        _colorStateFlow.value,
                                        _brightnessStateFlow.value,
                                        _frequencyStateFlow.value
                                    )
                                }
                            }
                        }
                        startForeground()
                    }

                    else -> {}
                }
            }
        }
    }

    override fun onDestroy() {
        controllerMap.values.forEach { it.release() }
        controllerMap.clear()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        controllerMap.values.forEach {
            if (it.isConnected) {
                return@forEach
            }
        }

        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            Actions.Connect.toString() -> {
                val deviceAddress: String? = intent.extras?.getString(BLUETOOTH_DEVICE_KEY)
                deviceAddress?.let {
                    handleConnect(address = it)
                }
            }

            Actions.Disconnect.toString() -> {
                val deviceAddress: String? = intent.extras?.getString(BLUETOOTH_DEVICE_KEY)

                deviceAddress?.let {
                    controllerMap[it]?.disconnect()
                } ?: controllerMap.values.forEach { controller ->
                    controller.disconnect()
                }
            }

            Actions.ChangeColor.toString() -> {
                val color = intent.extras?.getInt(COLOR_KEY)
                color?.let {
                    _colorStateFlow.tryEmit(color)
                }
            }

            Actions.StartAudioVisualizer.toString() -> {
                controllerMap.values.forEach { controller ->
                    controller.startVisualizer()
                }

                startForeground(true)
            }

            Actions.StopAudioVisualizer.toString() -> {
                controllerMap.values.forEach { controller ->
                    controller.stopVisualizer()
                }

                startForeground(false)
            }

            Actions.ChangeBrightness.toString() -> {
                val value = intent.extras?.getFloat(BRIGHTNESS_VALUE_KEY)
                value?.let {
                    _brightnessStateFlow.tryEmit(it)
                }
            }

            Actions.ChangeVisualizerFrequency.toString() -> {
                val value = intent.extras?.getFloat(FREQUENCY_VALUE_KEY)
                value?.let {
                    _frequencyStateFlow.tryEmit(it)
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }


    private fun startForeground(audioVisualizerEnabled: Boolean = false) {
        val disconnectIntent = Intent(this, BluetoothControlService::class.java).apply {
            action = Actions.Disconnect.toString()
        }

        val startVisualizerIntent = Intent(this, BluetoothControlService::class.java).apply {
            action = Actions.StartAudioVisualizer.toString()
        }
        val stopVisualizerIntent = Intent(this, BluetoothControlService::class.java).apply {
            action = Actions.StopAudioVisualizer.toString()
        }

        val disconnectIntentPending = PendingIntent.getForegroundService(this, 0, disconnectIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val startAudioVisualizerPendingIntent = PendingIntent.getForegroundService(this, 0, startVisualizerIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stopAudioVisualizerPendingIntent = PendingIntent.getForegroundService(this, 0, stopVisualizerIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        try {
            val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.audio_visualizer_title))
                .setContentText(if (audioVisualizerEnabled) getString(R.string.visualizer_enabled_lbl) else getString(R.string.visualizer_disabled_lbl))
                .setSmallIcon(R.drawable.ic_notification)
                .addAction(R.drawable.ic_disconnect, getString(R.string.disconnect_lbl), disconnectIntentPending)
                .setSilent(true)
                .setAutoCancel(false)
                .setOngoing(true)

            if (audioVisualizerEnabled) {
                notificationBuilder.addAction(R.drawable.graphic_eq, getString(R.string.start_visualizer_lbl), startAudioVisualizerPendingIntent)
                notificationBuilder.addAction(R.drawable.graphic_eq, getString(R.string.stop_visualizer_lbl), stopAudioVisualizerPendingIntent)
            }

            ServiceCompat.startForeground(
                /* service = */ this,
                /* id = */ NOTIFICATION_ID, // Cannot be 0
                /* notification = */ notificationBuilder.build(),
                /* foregroundServiceType = */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if(audioVisualizerEnabled) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                }
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                Log.d("BluetoothControlService", e.message ?: "No Message")
            }
        }
    }

    private fun handleConnect(address: String) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            controllerMap.getOrPut(address) {
                val controller = FanlightBleController(context = this@BluetoothControlService, scope)

                scope.launch {
                    _actionFlow.emit(controller.bleDeviceEventFlow)
                }

                controller
            }.connect(address)
        }
    }

    enum class Actions {
        Connect,
        Disconnect,
        ChangeColor,
        StartAudioVisualizer,
        StopAudioVisualizer,
        ChangeVisualizerFrequency,
        ChangeBrightness
    }

    inner class ServiceBinder: Binder() {
        fun getService() = this@BluetoothControlService
    }

    data class ColorState(
        val color: Int,
        val brightness: Float,
        val frequency: Float
    )
}