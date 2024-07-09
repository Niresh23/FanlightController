package com.niresh23.fanlightcontroller.ui.scan

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niresh23.fanlightcontroller.R
import com.niresh23.fanlightcontroller.viewstate.DeviceScanViewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    state: DeviceScanViewState?,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(text = "Find Device")
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
            var loading by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (loading) {
                    LinearProgressIndicator (
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
                Text(text = stringResource(id = R.string.scan_description_message))
                Card(modifier = Modifier.padding(all = 16.dp)) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Button(onClick = onStartScan) {
                                Text(text = stringResource(id = R.string.find_lbl))
                            }
                            Button(onClick = onStopScan) {
                                Text(text = stringResource(id = R.string.stop_lbl))
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(16.dp)
                        )

                        Box(modifier = Modifier.padding(bottom = 16.dp) ) {
                            when(state) {
                                is DeviceScanViewState.ActiveScan -> {
                                    loading = true
                                }
                                is DeviceScanViewState.Error -> {
                                    Text(text = state.message)
                                    loading = false
                                }
                                is DeviceScanViewState.ScanResults -> {
                                    loading = false
                                    BluetoothDeviceList(
                                        scannedDevices = state.scanResults.values.toList(),
                                        onClick = onDeviceClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    )
                                }
                                else -> {

                                }
                            }
                        }
                    }
                }

                Spacer(
                    Modifier
                        .fillMaxHeight()
                        .weight(1f))
            }
        }
    }
}

@Composable
fun BluetoothDeviceList(
    scannedDevices: List<BluetoothDevice>,
    onClick: (BluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        item {
            Text(
                text = stringResource(id = R.string.connect_to_device_explanation),
                modifier = Modifier.padding(16.dp)
            )
        }
        items(scannedDevices) { device ->
            Text(
                text = device.name ?: "(No name)",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(device) }
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
fun previewDeviceScreen() {
    DeviceScreen(
        state = null,
        onStartScan = { /*TODO*/ },
        onStopScan = { /*TODO*/ },
        onDeviceClick = fun(device) {}
    )
}