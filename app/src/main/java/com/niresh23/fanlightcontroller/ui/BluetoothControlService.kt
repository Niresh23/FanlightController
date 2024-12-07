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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BluetoothControlService: Service(), FanlightBleController.DeviceCallback {
    companion object {
        private const val NOTIFICATION_ID = 111
        const val NOTIFICATION_CHANNEL_ID = "audio_visualizer_channel_id"
        const val NOTIFICATION_CHANNEL_NAME = "audio_visualizer_notification_channel"
        const val BLUETOOTH_DEVICE_KEY = "bluetooth_device_key"
        const val COLOR_KEY = "color_key"
        const val FREQUENCY_VALUE_KEY = "frequency_value_key"
        const val BRIGHTNESS_VALUE_KEY = "brightness_value_key"
    }

    private lateinit var fanlightBleController: FanlightBleController
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val _actionFlow = MutableSharedFlow<DeviceEvent>()
    val actionFlow = _actionFlow.asSharedFlow()
    private val binder = ServiceBinder()

    override fun onCreate() {
        super.onCreate()
        fanlightBleController = FanlightBleController(this, this)
    }

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            Actions.Connect.toString() -> {
                val deviceAddress: String? = intent.extras?.getString(BLUETOOTH_DEVICE_KEY, "")
                deviceAddress?.let {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startForeground()
                        fanlightBleController.connect(it)
                    }
                }
            }

            Actions.Disconnect.toString() -> {
                fanlightBleController.disconnect()
                stopForeground(STOP_FOREGROUND_REMOVE)
            }

            Actions.ChangeColor.toString() -> {
                val color = intent.extras?.getInt(COLOR_KEY)
                color?.let {
                    fanlightBleController.colorChange(color)
                }
            }

            Actions.StartAudioVisualizer.toString() -> {
                fanlightBleController.startVisualizer()
                startForeground(true)
            }

            Actions.StopAudioVisualizer.toString() -> {
                fanlightBleController.stopVisualizer()
                startForeground(false)
            }

            Actions.ChangeBrightness.toString() -> {
                val value = intent.extras?.getFloat(BRIGHTNESS_VALUE_KEY)
                value?.let {
                    fanlightBleController.setBrightness(it)
                }
            }

            Actions.ChangeVisualizerFrequency.toString() -> {
                val value = intent.extras?.getFloat(FREQUENCY_VALUE_KEY)
                value?.let {
                    fanlightBleController.setFrequencyValue(value)
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

    override fun onDeviceAction(action: DeviceEvent) {
        scope.launch {
            _actionFlow.emit(action)
        }
    }
}