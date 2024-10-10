package com.niresh23.fanlightcontroller

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.niresh23.fanlightcontroller.ble.FanlightBleController
import com.niresh23.fanlightcontroller.ui.BluetoothControlService
import com.niresh23.fanlightcontroller.ui.connection.ConnectionAction
import com.niresh23.fanlightcontroller.ui.home.HomeScreen
import com.niresh23.fanlightcontroller.ui.theme.FanlightControllerTheme
import com.niresh23.fanlightcontroller.ui.connection.ConnectionViewModel
import com.niresh23.fanlightcontroller.ui.extensions.startAppSettingIntent
import com.niresh23.fanlightcontroller.utils.SettingKey
import com.niresh23.fanlightcontroller.viewmodel.FanlightViewModel
import com.niresh23.fanlightcontroller.viewstate.StickActions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val connectionViewModel: ConnectionViewModel by viewModels()
    private val fanlightViewModel: FanlightViewModel by viewModels()
    private lateinit var mService: BluetoothControlService
    private var mBound: Boolean = false
    private var currentDeviceConnectionId = ""

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as BluetoothControlService.ServiceBinder
            mService = binder.getService()
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        mService.actionFlow.collectLatest { action ->
                            when(action) {
                                is FanlightBleController.DeviceActions.Connecting -> {
                                    fanlightViewModel.connecting()
                                }
                                is FanlightBleController.DeviceActions.Connected -> {
                                    connectionViewModel.onAction(ConnectionAction.DeviceConnected(action.address))
                                    fanlightViewModel.connected()
                                }
                                is FanlightBleController.DeviceActions.Disconnected -> {
                                    connectionViewModel.onAction(ConnectionAction.DeviceDisconnected(action.address))
                                    fanlightViewModel.disconnected()
                                }
                            }
                        }
                    }
                }
            }
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FanlightControllerTheme {

                var showAlertDialog by remember { mutableStateOf(false) }
                val requestConnectPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
                    showAlertDialog = false

                    if (isGranted) {
                        connectToDevice()
                    }
                }

                val scope = rememberCoroutineScope()

                LaunchedEffect(key1 = true) {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) !=  PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_ADMIN
                        ) !=  PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH
                        ) !=  PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) !=  PackageManager.PERMISSION_GRANTED
                    ) {
                        scope.launch {
//                            permissionViewState.show()
                        }
                    }

                    scope.launch {
                        flowOf(fanlightViewModel.actionFlow, connectionViewModel.actionFlow).flattenMerge().collectLatest { action ->
                            when(action) {
                                is StickActions.Connect -> {
                                    currentDeviceConnectionId = action.address
                                    if (ActivityCompat.checkSelfPermission(
                                            this@MainActivity,
                                            Manifest.permission.BLUETOOTH_CONNECT
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        connectToDevice()
                                    } else if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT)) {
                                        showAlertDialog = true
                                    } else {
                                        requestConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                    }
                                }
                                is StickActions.Disconnect -> {
                                    Intent(this@MainActivity, BluetoothControlService::class.java).also {
                                        it.putExtra(BluetoothControlService.BLUETOOTH_DEVICE_KEY, action.address)
                                        it.action = BluetoothControlService.Actions.Disconnect.toString()
                                        this@MainActivity.startService(it)
                                    }
                                }

                                is StickActions.ChangeColor -> {
                                    Intent(this@MainActivity, BluetoothControlService::class.java).also {
                                        it.putExtra(BluetoothControlService.COLOR_KEY, action.color)
                                        it.action = BluetoothControlService.Actions.ChangeColor.toString()
                                        this@MainActivity.startService(it)
                                    }
                                }

                                is StickActions.StartAudioVisualizer -> {
                                    Intent(this@MainActivity, BluetoothControlService::class.java).also {
                                        it.action = BluetoothControlService.Actions.StartAudioVisualizer.toString()
                                        this@MainActivity.startService(it)
                                    }
                                }

                                is StickActions.StopAudioVisualizer -> {
                                    Intent(this@MainActivity, BluetoothControlService::class.java).also {
                                        it.action = BluetoothControlService.Actions.StopAudioVisualizer.toString()
                                        this@MainActivity.startService(it)
                                    }
                                }

                                is StickActions.ChangeVisualizationFrequency -> {
                                    this@MainActivity.settingsDataStore.edit { settings ->
                                        val key = floatPreferencesKey(SettingKey.VISUALIZATION_FREQUENCY_KEY)
                                        settings[key] = action.value
                                    }
                                }

                                is StickActions.ChangeBrightnessValue -> {
                                    this@MainActivity.settingsDataStore.edit { settings ->
                                        val key = floatPreferencesKey(SettingKey.BRIGHTNESS_KEY)
                                        settings[key] = action.brightness
                                    }
                                }
                            }
                        }
                    }
                }

                HomeScreen(
                    viewModel = fanlightViewModel,
                    connectionViewModel = connectionViewModel
                )

                if (showAlertDialog) {
                    AlertDialog(
                        onDismissRequest = { showAlertDialog = false },
                        confirmButton = {
                            TextButton(onClick = { requestConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }) {
                                Text(text = "Ok")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAlertDialog = false }) {
                                Text(text = "Cancel")
                            }
                        },
                        title = {
                            Text(
                                text = stringResource(id = R.string.connect_permission_title),
                                style = MaterialTheme.typography.titleMedium
                            )

                        },
                        text = {
                            Column {
                                Text(
                                    text = stringResource(id = R.string.rational_message_connect_permission),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(onClick = {
                                    showAlertDialog = false
                                    this@MainActivity.startAppSettingIntent()
                                }) {
                                    Text(text = stringResource(id = R.string.go_to_app_settings))
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    private fun connectToDevice() {
        Intent(
            this@MainActivity,
            BluetoothControlService::class.java
        ).also { intent ->
            intent.putExtra(
                BluetoothControlService.BLUETOOTH_DEVICE_KEY,
                currentDeviceConnectionId
            )
            intent.action =
                BluetoothControlService.Actions.Connect.toString()
            this@MainActivity.startService(intent)
            this@MainActivity.bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStart() {
        super.onStart()
        Dexter.withContext(this).withPermission(
            Manifest.permission.POST_NOTIFICATIONS
        ).withListener(object : PermissionListener {
            override fun onPermissionGranted(var1: PermissionGrantedResponse?) {

            }

            override fun onPermissionDenied(response: PermissionDeniedResponse?) {

            }

            override fun onPermissionRationaleShouldBeShown(
                request: PermissionRequest?,
                token: PermissionToken?
            ) {

            }
        }).check()

        Intent(this@MainActivity, BluetoothControlService::class.java).also {
            this@MainActivity.startService(it)
            this@MainActivity.bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }
}