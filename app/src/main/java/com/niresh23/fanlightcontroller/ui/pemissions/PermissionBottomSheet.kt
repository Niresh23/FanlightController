package com.niresh23.fanlightcontroller.ui.pemissions

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.niresh23.fanlightcontroller.R
import com.niresh23.fanlightcontroller.ui.SimpleAlertDialog
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionBottomSheet(
    viewModel: PermissionViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current as Activity
    val locationRequest = LocationRequest.Builder(10000)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .build()
    val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
    val client: SettingsClient = LocationServices.getSettingsClient(context)
    val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
    task.addOnSuccessListener { locationSetting ->
        locationSetting.locationSettingsStates?.isLocationUsable?.let {
            viewModel.locationEnable(it)
        }
    }
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val viewState = viewModel.permissionViewStateStateFlow.collectAsState()
    val actionView = viewModel.actonViewFlow.collectAsState()
    var showWarningDialog by remember { mutableStateOf(false) }
    var dialogText = ""
    var confirmAction = fun () { viewModel.closeDialog() }
    
    val settingLocationResultRequest =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) {activityResult->
            if (activityResult.resultCode == RESULT_OK)
                viewModel.locationEnable(true)
            else {
                viewModel.locationEnable(false)
            }
        }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    val bluetoothPermissions = rememberMultiplePermissionsState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    )

    val launcherBluetooth = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            viewModel.bluetoothEnable(true)
        } else if (it.resultCode == RESULT_CANCELED) {
            viewModel.bluetoothEnable(false)
        }
    }

    viewModel.bluetoothPermissionGranted(bluetoothPermissions.allPermissionsGranted)
    viewModel.locationPermissionGranted(locationPermissions.allPermissionsGranted)
    bluetoothManager.adapter?.isEnabled?.let {
        viewModel.bluetoothEnable(it)
    }

    LaunchedEffect(Unit) {
        viewModel.actionFlow.collectLatest { action ->
            when(action) {
                is PermissionViewModel.Action.RequestLocationPermission -> {
                    locationPermissions.launchMultiplePermissionRequest()
                }

                is PermissionViewModel.Action.RequestBluetoothPermission -> {
                    bluetoothPermissions.launchMultiplePermissionRequest()
                }

                is PermissionViewModel.Action.EnableLocation -> {
                    task.addOnFailureListener { exception ->
                        if (exception is ResolvableApiException) {
                            try {
                                val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                                settingLocationResultRequest.launch(intentSenderRequest)
                            } catch (sendEx: IntentSender.SendIntentException) {
                                viewModel.showExceptionDialog(sendEx)
                            }
                        }
                    }
                }

                is PermissionViewModel.Action.EnableBluetooth -> {
                    launcherBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
            }
        }
    }

    when(actionView.value) {
        is PermissionViewModel.ActionView.Close -> {
            showWarningDialog = false
            dialogText = ""
            confirmAction = fun () {
                viewModel.closeDialog()
            }
        }

        is PermissionViewModel.ActionView.ShowBluetoothPermissionDialog -> {
            showWarningDialog = true
            dialogText = stringResource(id = R.string.request_bluetooth_permission)
            confirmAction = fun () {
                viewModel.closeDialog()
                viewModel.requestBluetoothPermissionClicked()
            }
        }

        is PermissionViewModel.ActionView.ShowLocationPermissionDialog -> {
            showWarningDialog = true
            dialogText = stringResource(id = R.string.request_location_permission)
            confirmAction = fun () {
                viewModel.closeDialog()
                viewModel.requestLocationPermissionClicked()
            }
        }

        is PermissionViewModel.ActionView.ShowErrorDialog -> {
            showWarningDialog = true
            dialogText = (actionView.value as PermissionViewModel.ActionView.ShowErrorDialog).message
        }
    }

    if (showWarningDialog) {
        SimpleAlertDialog(
            onDismissRequest = { viewModel.closeDialog() },
            onConfirmation = { confirmAction.invoke() },
            dialogTitle = stringResource(id = R.string.warning_dialog_title),
            dialogText = dialogText,
            icon = Icons.Outlined.WarningAmber,
            contentDescription = ""
        )
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.permissions_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.bluetooth_permission_explanation),
            style = MaterialTheme.typography.titleLarge
        )

        //Bluetooth btn
        PermissionButtonView(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            text = stringResource(id = R.string.bluetooth_lbl),
            icon = Icons.Rounded.Bluetooth,
            iconTintColor = if (viewState.value.bluetoothPermissionGranted) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Gray
            },
            contentDescription = stringResource(id = R.string.bluetooth_permission_btn_description)
        ) {
            viewModel.requestBluetoothPermissionClicked()
        }

        //Location btn
        PermissionButtonView(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            text = stringResource(id = R.string.location_lbl),
            icon = Icons.Rounded.LocationOn,
            iconTintColor = if (viewState.value.locationPermissionGranted) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Gray
            },
            contentDescription = stringResource(id = R.string.location_permission_btn_description)
        ) {
            viewModel.requestLocationPermissionClicked()
        }
        //Enable bluetooth
        EnableFeatureButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.bluetooth_lbl),
            status = viewState.value.bluetoothIsEnabled,
            onClicked = {
                viewModel.enableBluetoothClicked()
            }
        )
        //Enable location
        EnableFeatureButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.location_lbl),
            status = viewState.value.locationIsEnabled,
            onClicked = {
                viewModel.enableLocationClicked()
            }
        )

        Button( modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.CenterHorizontally)
            .padding(20.dp),
            onClick = {
            if(viewState.value.isGranted) {
                onClose.invoke()
            }
        }) {
            Text(stringResource(id = R.string.dialog_confirm_btn_lbl))
        }
    }
}

@Composable
fun PermissionButtonView(modifier: Modifier, text: String, icon: ImageVector, iconTintColor: Color, contentDescription: String, onClick: () -> Unit) {
    Row(modifier = modifier.clickable {
        onClick.invoke()
    }) {
        Text(text = text, modifier = Modifier.align(Alignment.CenterVertically))
        Spacer(modifier = Modifier
            .weight(1f)
            .fillMaxWidth())
        Image(
            modifier = Modifier.align(Alignment.CenterVertically).fillMaxHeight(),
            imageVector = icon,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(color = iconTintColor)
        )
    }
}

@Composable
fun EnableFeatureButton(modifier: Modifier, text: String, status: Boolean, onClicked: (changed: Boolean) -> Unit) {
    Row(modifier = modifier) {
        Text(
            modifier = Modifier.align(Alignment.CenterVertically),
            text = text)
        Spacer(modifier = Modifier
            .weight(1f)
            .fillMaxWidth())
        Switch(
            checked = status,
            onCheckedChange = onClicked
        )
    }
}