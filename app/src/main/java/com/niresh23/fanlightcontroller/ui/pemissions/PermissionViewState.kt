package com.niresh23.fanlightcontroller.ui.pemissions

data class PermissionViewState(
    val locationIsEnabled: Boolean = false,
    val bluetoothIsEnabled: Boolean = false,
    val bluetoothPermissionGranted: Boolean = false,
    val locationPermissionGranted: Boolean = false,
) {
    val isGranted = locationIsEnabled && bluetoothIsEnabled && bluetoothPermissionGranted && locationPermissionGranted
}
