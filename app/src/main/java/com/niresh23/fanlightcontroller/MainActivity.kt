package com.niresh23.fanlightcontroller

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.niresh23.fanlightcontroller.ui.ControlScreen
import com.niresh23.fanlightcontroller.ui.scan.DeviceScreen
import com.niresh23.fanlightcontroller.ui.theme.FanlightControllerTheme
import com.niresh23.fanlightcontroller.viewmodel.DeviceScanViewModel
import com.niresh23.fanlightcontroller.viewmodel.FanlightViewModel
import com.niresh23.fanlightcontroller.viewstate.DeviceConnectionState

class MainActivity : ComponentActivity() {
    private val deviceScanViewModel: DeviceScanViewModel by viewModels()
    private val fanlightViewModel: FanlightViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FanlightControllerTheme {
                val result = remember { mutableStateOf<Int?>(100) }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result.value = it.resultCode
                }

                LaunchedEffect(key1 = true) {
                    Dexter.withContext(this@MainActivity)
                        .withPermissions(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                        ).withListener(object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                launcher.launch(intent)
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permissions: List<PermissionRequest?>?,
                                token: PermissionToken?
                            ) {}
                        }).check()
                }

                Scaffold(topBar = {
                    TopAppBar(
                        title = {
                            Text(text = "Fanlight Controller")
                        }
                    )
                }) {
                    it.calculateTopPadding()
                    it.calculateBottomPadding()
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 80.dp)
                    ) {

                        val deviceScanningState by deviceScanViewModel.viewState.observeAsState()
                        val deviceConnectionState by fanlightViewModel.connectionStateFlow.collectAsState()

                        when(deviceConnectionState) {
                            is DeviceConnectionState.Disconnected -> {
                                DeviceScreen(
                                    state = deviceScanningState,
                                    onStartScan = { deviceScanViewModel.startScan() },
                                    onStopScan = { deviceScanViewModel.stopScanning() },
                                    onDeviceClick = fun(device) {
                                        fanlightViewModel.connect(device)
                                    }
                                )
                            }
                            is DeviceConnectionState.Connecting -> {
                                CircularProgressIndicator()
                            }
                            is DeviceConnectionState.Connected -> {
                                ControlScreen(fanlightViewModel = fanlightViewModel)
                            }
                        }
                    }
                }
            }
        }

    }

    private inline fun <T> checkPermission(context: Context, permission: String, click: () -> T) {
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            click.invoke()
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FanlightControllerTheme {
        Greeting("Android")
    }
}