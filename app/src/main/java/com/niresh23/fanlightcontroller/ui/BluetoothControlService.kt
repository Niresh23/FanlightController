package com.niresh23.fanlightcontroller.ui

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
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
import androidx.datastore.preferences.core.floatPreferencesKey
import com.niresh23.fanlightcontroller.R
import com.niresh23.fanlightcontroller.ble.FanlightBleController
import com.niresh23.fanlightcontroller.settingsDataStore
import com.niresh23.fanlightcontroller.utils.SettingKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BluetoothControlService: Service(), FanlightBleController.DeviceCallback {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "audio_visualizer_channel_id"
        const val NOTIFICATION_CHANNEL_NAME = "audio_visualizer_notification_channel"
        const val BLUETOOTH_DEVICE_KEY = "bluetooth_device_key"
        const val COLOR_KEY = "color_key"
    }
    private val fanlightBleController = FanlightBleController(this, this)
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val binder = ServiceBinder()
    private val _actionFlow = MutableStateFlow(FanlightBleController.DeviceActions.Disconnected)
    val actionFlow = _actionFlow.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            val key = floatPreferencesKey(SettingKey.BRIGHTNESS_KEY)
            this@BluetoothControlService.settingsDataStore.data.map {
                it[key] ?: 1f
            }.collectLatest {
                if (ActivityCompat.checkSelfPermission(
                        this@BluetoothControlService,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fanlightBleController.setBrightness(it)
                }
            }
        }

        scope.launch {
            val key = floatPreferencesKey(SettingKey.VISUALIZATION_FREQUENCY_KEY)
            this@BluetoothControlService.settingsDataStore.data.map {
                it[key] ?: 1f
            }.collectLatest {
                fanlightBleController.setFrequencyValue(it)
            }
        }
    }
    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            Actions.Connect.toString() -> {
                startForeground()
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.extras?.getParcelable(BLUETOOTH_DEVICE_KEY, BluetoothDevice::class.java)
                    } else {
                        intent.extras?.getParcelable(BLUETOOTH_DEVICE_KEY)
                    }
                    device?.let {
                        fanlightBleController.connect(it)
                    }
                } else {
                    stopSelf()
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
            }
            Actions.StopAudioVisualizer.toString() -> {
                fanlightBleController.stopVisualizer()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }


    private fun startForeground() {

        val disconnectIntent = Intent(this, BluetoothControlService::class.java).apply {
            action = Actions.Disconnect.toString()
        }

        val disconnectIntentPending = PendingIntent.getForegroundService(this, 0, disconnectIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        try {
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.audio_visualizer_title))
                .setSmallIcon(R.drawable.graphic_eq)
                .addAction( R.drawable.ic_disonnect, getString(R.string.disconnect_lbl), disconnectIntentPending)
                .setSilent(true)
                .setAutoCancel(false)
                .setOngoing(true)
                .build()

            ServiceCompat.startForeground(
                /* service = */ this,
                /* id = */ 111, // Cannot be 0
                /* notification = */ notification,
                /* foregroundServiceType = */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                },
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
        StopAudioVisualizer
    }

    inner class ServiceBinder: Binder() {
        fun getService() = this@BluetoothControlService
    }

    override fun onAction(action: FanlightBleController.DeviceActions) {
        _actionFlow.value = action
    }
}