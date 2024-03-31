package com.niresh23.fanlightcontroller.ui.audiovisualizer

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.core.floatPreferencesKey
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.niresh23.fanlightcontroller.R
import com.niresh23.fanlightcontroller.settingsDataStore
import com.niresh23.fanlightcontroller.ui.SimpleAlertDialog
import com.niresh23.fanlightcontroller.utils.SettingKey
import com.niresh23.fanlightcontroller.viewmodel.FanlightViewModel
import kotlinx.coroutines.flow.map

@Composable
fun VisualizerScreen(viewModel: FanlightViewModel) {
    val context = LocalContext.current as Activity
    var showWarningDialog by remember { mutableStateOf(false) }
    var onDialogConfirmation: () -> Unit by remember { mutableStateOf({}) }
    val sliderPosition = context.settingsDataStore.data.map {
        val key = floatPreferencesKey(SettingKey.VISUALIZATION_FREQUENCY_KEY)
        it[key] ?: 1f
    }.collectAsState(initial = 1f)

    if (showWarningDialog) {
        SimpleAlertDialog(
            onDismissRequest = { showWarningDialog = false },
            onConfirmation = {
                onDialogConfirmation.invoke()
            },
            dialogTitle = stringResource(id = R.string.warning_dialog_title),
            dialogText = stringResource(id = R.string.rational_record_permission_request_message),
            icon = Icons.Outlined.WarningAmber,
            contentDescription = ""
        )
    }
    Column {
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
                Dexter.withContext(context).withPermission(
                    Manifest.permission.RECORD_AUDIO
                ).withListener(object : PermissionListener {
                    override fun onPermissionGranted(var1: PermissionGrantedResponse?) {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        viewModel.startAudioVisualizer()
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                        onDialogConfirmation = fun() {
                            showWarningDialog = false
                        }
                        showWarningDialog = true
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        request: PermissionRequest?,
                        token: PermissionToken?
                    ) {
                        onDialogConfirmation = fun() {
                            showWarningDialog = false
                            token?.continuePermissionRequest()
                        }
                        showWarningDialog = true
                    }
                }).check()
            }) {
                Text("Start Visualizer")
            }
            Button(onClick = {
                viewModel.stopAudioVisualizer()
            }) {
                Text("Stop Visualizer")
            }
        }
    }
}