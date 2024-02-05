package com.niresh23.fanlightcontroller.ui.scan

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niresh23.fanlightcontroller.viewstate.DeviceScanViewState

@Composable
fun DeviceScreen(
    state: DeviceScanViewState?,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onStartServer: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        when(state) {
            is DeviceScanViewState.ActiveScan -> {
                Text(text = "Scanning")
            }
            is DeviceScanViewState.Error -> {
                Text(text = state.message)
            }
            is DeviceScanViewState.AdvertisementNotSupported -> {
                Text(text = "Advertisement Not Supported")
            }
            is DeviceScanViewState.ScanResults -> {
                BluetoothDeviceList(
                    scannedDevices = state.scanResults.values.toList(),
                    onClick = onDeviceClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
            else -> {

            }
        }
        
        Spacer(modifier = Modifier)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            Button(onClick = onStartScan) {
                Text(text = "Start scan")
            }
            Button(onClick = onStopScan) {
                Text(text = "Stop scan")
            }
            Button(onClick = onStartServer) {
                Text(text = "Start server")
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
                text = "Scanned Devices",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(scannedDevices) { device ->
            Text(
                text = device.name ?: "(No name)",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(device) }
                    .padding(16.dp)
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
        onStartServer = { /*TODO*/ },
        onDeviceClick = fun(device) {}
    )
}