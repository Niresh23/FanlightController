package com.niresh23.fanlightcontroller

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.niresh23.fanlightcontroller.ui.home.HomeScreen
import com.niresh23.fanlightcontroller.ui.pemissions.PermissionBottomSheet
import com.niresh23.fanlightcontroller.ui.pemissions.PermissionViewModel
import com.niresh23.fanlightcontroller.ui.scan.DeviceScreen
import com.niresh23.fanlightcontroller.ui.theme.FanlightControllerTheme
import com.niresh23.fanlightcontroller.ui.scan.DeviceScanViewModel
import com.niresh23.fanlightcontroller.utils.SettingKey
import com.niresh23.fanlightcontroller.viewmodel.FanlightViewModel
import com.niresh23.fanlightcontroller.viewstate.DeviceConnectionState
import com.niresh23.fanlightcontroller.viewstate.StickActions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val deviceScanViewModel: DeviceScanViewModel by viewModels()
    private val fanlightViewModel: FanlightViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()
    private lateinit var mService: BluetoothControlService
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as BluetoothControlService.ServiceBinder
            mService = binder.getService()
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch { fanlightViewModel.actionFlow.collectLatest { action ->
                        when(action) {
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
                    } }
                    launch {
                        mService.actionFlow.collectLatest {  action ->
                            when(action) {
                                FanlightBleController.DeviceActions.Connecting -> {
                                    fanlightViewModel.connecting()
                                }
                                FanlightBleController.DeviceActions.Connected -> {
                                    fanlightViewModel.connected()
                                }
                                FanlightBleController.DeviceActions.Disconnected -> {
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FanlightControllerTheme {
                val permissionViewState = rememberModalBottomSheetState(
                    skipPartiallyExpanded  = true,
                    confirmValueChange = { false }
                )
                val scope = rememberCoroutineScope()
                var showPermissionView by remember { mutableStateOf(false) }
                val deviceScanningState by deviceScanViewModel.viewState.collectAsState()
                val deviceConnectionState by fanlightViewModel.connectionStateFlow.collectAsState()

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
                    showPermissionView = true
                }

                if(showPermissionView) {
                    ModalBottomSheet(
                        onDismissRequest = {},
                        sheetState = permissionViewState
                    ) {
                        PermissionBottomSheet(permissionViewModel) {
                            scope.launch { permissionViewState.hide() }.invokeOnCompletion {
                                if (!permissionViewState.isVisible) {
                                    showPermissionView = false
                                }
                            }
                        }
                    }
                }


                when(deviceConnectionState) {
                    is DeviceConnectionState.Disconnected -> {
                        DeviceScreen(
                            state = deviceScanningState,
                            onStartScan = {
                                if(Build.VERSION.SDK_INT >= VERSION_CODES.S) {
                                    checkPermission(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN) {
                                        deviceScanViewModel.startScan()
                                    }
                                } else {
                                    deviceScanViewModel.startScan()
                                }
                            }
                            ,
                            onStopScan = { deviceScanViewModel.stopScanning() },
                            onDeviceClick = fun(device) {
                                Intent(this@MainActivity, BluetoothControlService::class.java).also {
                                    it.putExtra(BluetoothControlService.BLUETOOTH_DEVICE_KEY, device)
                                    it.action = BluetoothControlService.Actions.Connect.toString()
                                    this@MainActivity.startService(it)
                                    this@MainActivity.bindService(it, connection, Context.BIND_AUTO_CREATE)
                                }
                            }
                        )
                    }
                    is DeviceConnectionState.Connecting -> {
                        Intent()
                        Scaffold {
                            it.calculateTopPadding()
                            it.calculateBottomPadding()
                            Surface {
                                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()){
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        }
                    }
                    is DeviceConnectionState.Connected -> {
                        HomeScreen(viewModel = fanlightViewModel)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    override fun onStart() {
        super.onStart()
        Dexter.withContext(this).withPermission(
            Manifest.permission.POST_NOTIFICATIONS
        ).withListener(object : PermissionListener {
            override fun onPermissionGranted(var1: PermissionGrantedResponse?) {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
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

inline fun <T> checkPermission(context: Context, permission: String, click: () -> T) {
    if (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
        click.invoke()
    } else {

    }
}