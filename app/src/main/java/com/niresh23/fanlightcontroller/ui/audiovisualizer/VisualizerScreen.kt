package com.niresh23.fanlightcontroller.ui.audiovisualizer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.datastore.preferences.core.floatPreferencesKey
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.niresh23.fanlightcontroller.R
import com.niresh23.fanlightcontroller.settingsDataStore
import com.niresh23.fanlightcontroller.ui.SimpleAlertDialog
import com.niresh23.fanlightcontroller.ui.extensions.startAppSettingIntent
import com.niresh23.fanlightcontroller.utils.SettingKey
import com.niresh23.fanlightcontroller.viewmodel.FanlightViewModel
import kotlinx.coroutines.flow.map


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VisualizerScreen(viewModel: FanlightViewModel) {
    val context = LocalContext.current as Activity
    var showWarningDialog by remember { mutableStateOf(false) }
    val sliderPosition = context.settingsDataStore.data.map {
        val key = floatPreferencesKey(SettingKey.VISUALIZATION_FREQUENCY_KEY)
        it[key] ?: 1f
    }.collectAsState(initial = 1f)

    val recordAudioPermission = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if(isGranted) {
            viewModel.startAudioVisualizer()
        } else {
            showWarningDialog = true
        }
    }


    if (showWarningDialog) {
        SimpleAlertDialog(
            onDismissRequest = { showWarningDialog = false },
            onConfirmation = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                startActivity(context, intent, null)
                showWarningDialog = false
            },
            dialogTitle = stringResource(id = R.string.warning_dialog_title),
            dialogText = {
                Column {
                    Text(
                        text = stringResource(id = R.string.rational_record_permission_request_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = {
                        showWarningDialog = false
                        context.startAppSettingIntent()
                    }) {
                        Text(text = stringResource(id = R.string.go_to_app_settings))
                    }
                }
            },
            icon = Icons.Outlined.WarningAmber,
            contentDescription = ""
        )
    }

    Column {
        Text(text = stringResource(id = R.string.visualizer_description))
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(16.dp))
        Text(text = stringResource(id = R.string.frequency))
        Slider(
            value = sliderPosition.value,
            onValueChange = { viewModel.changeFrequency(it) },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            valueRange = 1f..20f
        )
        Row {
            Button(onClick = {
                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.startAudioVisualizer()
                } else if(recordAudioPermission.status.shouldShowRationale) {
                    showWarningDialog = true
                } else {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }) {
                Text(stringResource(id = R.string.start_visualizer_lbl))
            }
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .weight(1f))
            Button(onClick = {
                viewModel.stopAudioVisualizer()
            }) {
                Text(stringResource(id = R.string.stop_visualizer_lbl))
            }
        }
    }
}