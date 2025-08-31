package com.niresh23.fanlightcontroller.ui.audiovisualizer

import android.Manifest
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.niresh23.fanlightcontroller.R
import com.niresh23.fanlightcontroller.ui.SimpleAlertDialog
import com.niresh23.fanlightcontroller.ui.extensions.startAppSettingIntent
import com.niresh23.fanlightcontroller.viewmodel.ControllerAction
import com.niresh23.fanlightcontroller.visualizer.AudioVisualizer

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VisualizerScreen(
    viewState: VisualizerViewState,
    onAction: (ControllerAction) -> Unit
) {
    val context = LocalActivity.current

    var showWarningDialog by remember { mutableStateOf(false) }
    val recordAudioPermission = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO,
        onPermissionResult = { isGranted ->
            if (isGranted) {
                showWarningDialog = false
                onAction.invoke(ControllerAction.StartVisualizer)
            } else {
                showWarningDialog = true
            }
        }
    )

    if (showWarningDialog) {
        SimpleAlertDialog(
            onDismissRequest = { showWarningDialog = false },
            onConfirmation = {
                showWarningDialog = false
                recordAudioPermission.launchPermissionRequest()
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
                        context?.startAppSettingIntent()
                    }) {
                        Text(text = stringResource(id = R.string.go_to_app_settings))
                    }
                }
            },
            icon = Icons.Outlined.WarningAmber,
            contentDescription = ""
        )
    }

    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        Text(text = stringResource(id = R.string.visualizer_description))
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(16.dp))
        Text(text = stringResource(id = R.string.frequency))
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = viewState.frequency,
            onValueChange = { onAction.invoke(ControllerAction.ChangeFrequency(it)) },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            valueRange = 0f..1f
        )

        Row {
            FilterChip(
                modifier = Modifier.fillMaxWidth().weight(1f),
                onClick = {
                    onAction.invoke(
                        ControllerAction.ChangeVisualizerParam(
                            param = viewState.param.copy(mode = AudioVisualizer.Mode.CLASSIC)
                        )
                    )
                },
                selected = viewState.param.mode == AudioVisualizer.Mode.CLASSIC,
                label = {
                    Text(
                        "Classic",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            )
            Spacer(modifier = Modifier.width(10.dp))
            FilterChip(
                modifier = Modifier.fillMaxWidth().weight(1f),
                onClick = {
                    onAction.invoke(
                        ControllerAction.ChangeVisualizerParam(
                            param = viewState.param.copy(mode = AudioVisualizer.Mode.MODERN)
                        )
                    )
                },
                selected = viewState.param.mode == AudioVisualizer.Mode.MODERN,
                label = {
                    Text(
                        "New",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            )
        }

        when(viewState.param.mode) {
            AudioVisualizer.Mode.CLASSIC -> {}
            AudioVisualizer.Mode.MODERN -> {
                Text("Bas")
                Slider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    value = viewState.param.bassAmplifier,
                    onValueChange = { onAction.invoke(
                        ControllerAction.ChangeVisualizerParam(
                            viewState.param.copy(bassAmplifier = it)
                        )) },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    valueRange = 0f..1f
                )
                Text("Mid")
                Slider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    value = viewState.param.midAmplifier,
                    onValueChange = { onAction.invoke(
                        ControllerAction.ChangeVisualizerParam(
                            viewState.param.copy(midAmplifier = it)
                        )
                    ) },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    valueRange = 0f..1f
                )
                Text("Tremble")
                Slider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    value = viewState.param.trembleAmplifier,
                    onValueChange = { onAction.invoke(
                        ControllerAction.ChangeVisualizerParam(
                            viewState.param.copy(trembleAmplifier = it)
                        )
                    ) },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    valueRange = 0f..1f
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Phase", style = MaterialTheme.typography.headlineMedium)
                Text("Bass")
                Slider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    value = viewState.param.bassPhaseAmplifier,
                    onValueChange = { onAction.invoke(
                        ControllerAction.ChangeVisualizerParam(
                            viewState.param.copy(bassPhaseAmplifier = it)
                        )
                    ) },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    valueRange = 0f..1f
                )
                Text("Mid")
                Slider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    value = viewState.param.midPhaseAmplifier,
                    onValueChange = { onAction.invoke(
                        ControllerAction.ChangeVisualizerParam(
                            viewState.param.copy(midPhaseAmplifier = it)
                        )
                    ) },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    valueRange = 0f..1f
                )
                Text("Tremble")
                Slider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    value = viewState.param.tremblePhaseAmplifier,
                    onValueChange = { onAction.invoke(
                        ControllerAction.ChangeVisualizerParam(
                            viewState.param.copy(tremblePhaseAmplifier = it)
                        )
                    ) },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    valueRange = 0f..1f
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row {
            Button(onClick = {
                if (recordAudioPermission.status.isGranted) {
                    onAction.invoke(ControllerAction.StartVisualizer)
                } else if(recordAudioPermission.status.shouldShowRationale) {
                    showWarningDialog = true
                } else {
                    recordAudioPermission.launchPermissionRequest()
                }
            }) {
                Text(stringResource(id = R.string.start_visualizer_lbl))
            }
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .weight(1f))
            Button(onClick = {
                onAction.invoke(ControllerAction.StopVisualizer)
            }) {
                Text(stringResource(id = R.string.stop_visualizer_lbl))
            }
        }
    }
}