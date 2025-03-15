package com.niresh23.fanlightcontroller.ui.connection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import com.niresh23.fanlightcontroller.R
import com.niresh23.fanlightcontroller.ui.extensions.getActivity
import com.niresh23.fanlightcontroller.ui.extensions.startAppSettingIntent
import com.niresh23.fanlightcontroller.viewmodel.ControllerAction

@Composable
fun ConnectionScreen(
    viewState: ConnectionViewState,
    controllerAction: (ControllerAction) -> Unit,
    action: (ConnectionAction) -> Unit
) {
    var showScanAlertDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val requestScanPermission = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        showScanAlertDialog = false

        if(isGranted) {
            action.invoke(ConnectionAction.Scan)
        } else {
            showScanAlertDialog = true
        }
    }
    var connectToDevice: (() -> Unit)? = null
    var showConnectAlertDialog by remember { mutableStateOf(false) }
    val requestConnectPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        showConnectAlertDialog = false

        if (isGranted) {
            connectToDevice?.invoke()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Find Device"
        )

        Text(text = stringResource(id = R.string.scan_description_message))

        if (viewState.scanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        BluetoothDeviceList(
            scannedDevices = viewState.deviceList,
            onConnect = { address ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        controllerAction.invoke(ControllerAction.Connect(address))

                    } else if(
                        context.getActivity()?.let { activity -> ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.BLUETOOTH_CONNECT) } == true
                    ) {
                        showConnectAlertDialog = true
                    } else {
                        connectToDevice = {
                            controllerAction.invoke(ControllerAction.Connect(address))
                        }
                        requestScanPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                }
            },
            onDisconnect = { address ->
                controllerAction.invoke(ControllerAction.Disconnect(address))
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeContentPadding(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        requestScanPermission.launch(Manifest.permission.BLUETOOTH_SCAN)
                    } else if(
                        context.getActivity()?.let { activity -> ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.BLUETOOTH_SCAN) } == true
                    ) {
                        showScanAlertDialog = true
                    } else {
                        action.invoke(ConnectionAction.Scan)
                    }
                }
            }) {
                Text(text = stringResource(id = R.string.find_lbl))
            }
            Button(onClick = {
                action.invoke(ConnectionAction.StopScan)
            }) {
                Text(text = stringResource(id = R.string.stop_lbl))
            }
        }
    }

    if (showConnectAlertDialog) {
        AlertDialog(
            onDismissRequest = { showConnectAlertDialog = false },
            confirmButton = {
                TextButton(onClick = { requestConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }) {
                    Text(text = "Ok")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectAlertDialog = false }) {
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
                        showConnectAlertDialog = false
                        context.startAppSettingIntent()
                    }) {
                        Text(text = stringResource(id = R.string.go_to_app_settings))
                    }
                }
            }
        )
    }

    if (showScanAlertDialog) {
        AlertDialog(
            onDismissRequest = { showConnectAlertDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    requestScanPermission.launch(Manifest.permission.BLUETOOTH_SCAN)
                }) {
                    Text(text = "Ok")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectAlertDialog = false }) {
                    Text(text = "Cancel")
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.scan_permission_title),
                    style = MaterialTheme.typography.titleMedium
                )

            },
            text = {
                Column {
                    Text(
                        text = stringResource(id = R.string.rational_message_scan_permission),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = {
                        showConnectAlertDialog = false
                        context.startAppSettingIntent()
                    }) {
                        Text(text = stringResource(id = R.string.go_to_app_settings))
                    }
                }
            }
        )
    }

    if (viewState.error != null) {
        AlertDialog(
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            onDismissRequest = {  },
            confirmButton = {
                TextButton(onClick = {
                    action.invoke(ConnectionAction.StopScan)
                }) {
                    Text(text = "Ok")
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.warning_dialog_title),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Text(
                        text = viewState.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        )
    }
}

@Composable
fun BluetoothDeviceList(
    scannedDevices: List<DeviceViewState>,
    onConnect: (String) -> Unit,
    onDisconnect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.connect_to_device_explanation),
                modifier = Modifier.padding(16.dp)
            )
        }

        items(scannedDevices) { device ->
            Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(
                            modifier = Modifier.weight(1f)
                        )
                        if (device.status == DeviceConnectionStatus.CONNECTED) {
                            Battery(level = device.batteryLevel)
                        }
                    }

                    Row {
                        Image(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .width(24.dp)
                                .height(24.dp),
                            painter = painterResource(id = R.drawable.logoe_exo),
                            contentDescription = "",
                            colorFilter = ColorFilter.tint(
                                when(device.status) {
                                    DeviceConnectionStatus.CONNECTED -> {
                                        Color.Green
                                    }
                                    DeviceConnectionStatus.DISCONNECTED -> {
                                        Color.Red
                                    }
                                    else -> {
                                        Color.Yellow
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        OutlinedButton(
                            modifier = Modifier
                                .wrapContentSize(),
                            onClick = {
                                when(device.status) {
                                    DeviceConnectionStatus.DISCONNECTED -> {
                                        onConnect.invoke(device.address)
                                    }
                                    DeviceConnectionStatus.CONNECTED -> {
                                        onDisconnect.invoke(device.address)
                                    }
                                    else -> {}
                                }
                            }) {
                            Text(
                                text = if (device.status == DeviceConnectionStatus.CONNECTED) "Disconnect" else "Connect"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Battery(modifier: Modifier = Modifier, level: Int) {
    Row {
        Image(
            imageVector = when(level) {
                in 0..5 -> {
                    Icons.Filled.Battery0Bar
                }
                in 5..20 -> {
                    Icons.Filled.Battery1Bar
                }
                in 20..35 -> {
                    Icons.Filled.Battery2Bar
                }
                in 35..50 -> {
                    Icons.Filled.Battery3Bar
                }
                in 50..65 -> {
                    Icons.Filled.Battery5Bar
                }
                in 65..90 -> {
                    Icons.Filled.Battery6Bar
                }
                in 95..100 -> {
                    Icons.Filled.BatteryFull
                }
                else -> {
                    Icons.Filled.BatteryAlert
                }
            },
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = when(level) {
                in 0..15 -> Color.Red
                in 15..40 -> Color.Yellow
                in 40..100 -> Color.Green
                else -> Color.DarkGray
            })
        )
        Text(
            modifier = Modifier.align(Alignment.CenterVertically),
            text = "$level%",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview
@Composable
fun ConnectionScreenPreview() {
    Surface {
        ConnectionScreen(
            viewState = ConnectionViewState(
                deviceList = listOf(
                    DeviceViewState("Exo Lightstick ver. 3", "EB:B6:A1:CA:B9:18", status = DeviceConnectionStatus.CONNECTED, batteryLevel = 100),
                    DeviceViewState("Exo Lightstick ver. 3", "EB:B6:A1:CA:B9:18", status = DeviceConnectionStatus.CONNECTING, batteryLevel = 35),
                    DeviceViewState("Exo Lightstick ver. 3", "EB:B6:A1:CA:B9:19", batteryLevel = 5)
                ),
                scanning = true
            ), {} , {})
    }
}