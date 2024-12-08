package com.niresh23.fanlightcontroller.ui.connection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.niresh23.fanlightcontroller.R
import com.niresh23.fanlightcontroller.ui.extensions.getActivity
import com.niresh23.fanlightcontroller.ui.extensions.startAppSettingIntent

@Composable
fun ConnectionScreen(
    viewState: ConnectionViewState,
    devicesListState: SnapshotStateMap<String, DeviceViewState>,
    action: (ConnectionAction) -> Unit
) {
    var showAlertDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val requestScanPermission = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        showAlertDialog = false

        if(isGranted) {
            action.invoke(ConnectionAction.Scan)
        } else {
            showAlertDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
            scannedDevices = devicesListState,
            onConnect = { address ->
                action.invoke(ConnectionAction.Connect(address))
            },
            onDisconnect = { address ->
                action.invoke(ConnectionAction.Disconnect(address))
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(
            Modifier
                .fillMaxHeight()
                .weight(1f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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
                        showAlertDialog = true
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

    if (showAlertDialog) {
        AlertDialog(
            onDismissRequest = { showAlertDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    requestScanPermission.launch(Manifest.permission.BLUETOOTH_SCAN)
                }) {
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
                        showAlertDialog = false
                        context.startAppSettingIntent()
                    }) {
                        Text(text = stringResource(id = R.string.go_to_app_settings))
                    }
                }
            }
        )
    }
}

@Composable
fun BluetoothDeviceList(
    scannedDevices: Map<String, DeviceViewState>,
    onConnect: (String) -> Unit,
    onDisconnect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.connect_to_device_explanation),
                modifier = Modifier.padding(16.dp)
            )
        }

        items(scannedDevices.map { it.value }) { device ->
            Card {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier
                            .height(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = device.name
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = device.address
                        )
                    }
                    OutlinedButton(
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.End),
                        onClick = {
                            if (device is DeviceViewState.Connected) {
                                onDisconnect.invoke(device.address)
                            } else {
                                onConnect.invoke(device.address)
                            }
                        }) {
                        Text(
                            text = if (device is DeviceViewState.Connected) "Disconnect" else "Connect"
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ConnectionScreenPreview() {
    Surface {
        ConnectionScreen(viewState = ConnectionViewState(scanning = true), devicesListState = remember {
            mutableStateMapOf(
                Pair("EB:B6:A1:CA:B9:18", DeviceViewState.Disconnected("Exo Light stick", "EB:B6:A1:CA:B9:18")),
                Pair("EB:B6:A1:CA:B9:19", DeviceViewState.Disconnected("Exo Light stick", "EB:B6:A1:CA:B9:19"))
                )
        }) {

        }
    }
}