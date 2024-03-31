package com.niresh23.fanlightcontroller.ui.color

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.core.floatPreferencesKey
import com.niresh23.fanlightcontroller.R
import com.niresh23.fanlightcontroller.settingsDataStore
import com.niresh23.fanlightcontroller.ui.toIntHexColor
import com.niresh23.fanlightcontroller.utils.SettingKey
import com.niresh23.fanlightcontroller.viewmodel.FanlightViewModel
import kotlinx.coroutines.flow.map

@Composable
fun ColorScreen(
    viewModel: FanlightViewModel
) {
    val colorButtonsList = generateColorPlate()
    val context = LocalContext.current as Activity

    val sliderPosition = context.settingsDataStore.data.map {
        val key = floatPreferencesKey(SettingKey.BRIGHTNESS_KEY)
        it[key] ?: 1f
    }.collectAsState(initial = 1f)

    Column {
        Text(text = stringResource(id = R.string.brightness))
        Slider(
            value = sliderPosition.value,
            onValueChange = { viewModel.changeBrightness(it) },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            valueRange = 0f..1f
        )

        LazyVerticalGrid( columns = GridCells.Fixed(5)) {
            items(colorButtonsList) { color ->
                Box(
                    modifier = Modifier
                        .background(color)
                        .clickable {
                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.changeColor(color.toIntHexColor())
                            }
                        }
                        .height(50.dp)
                        .width(50.dp)
                )
            }
        }
    }
}

private fun generateColorPlate(): List<Color> {
    val list = arrayListOf<Color>()
    val saturation = 1f
    val lightness = 1f
    list.add(Color.White)

    for (i in 0 ..360 step 10) {
        val hsv = android.graphics.Color.HSVToColor(
            floatArrayOf(
                i.toFloat(),
                saturation,
                lightness
            )
        )

        list.add(Color(hsv))
    }

    return list
}

