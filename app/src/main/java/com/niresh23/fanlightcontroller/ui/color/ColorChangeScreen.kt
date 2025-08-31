package com.niresh23.fanlightcontroller.ui.color

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.niresh23.fanlightcontroller.R
import com.niresh23.fanlightcontroller.ui.HueColor
import com.niresh23.fanlightcontroller.ui.toIntHexColor
import com.niresh23.fanlightcontroller.viewmodel.ControllerAction

@Composable
fun ColorScreen(
    viewState: ColorViewState,
    onAction: (ControllerAction) -> Unit
) {
    val colorButtonsList = generateColorPlate()
    val context = LocalActivity.current ?: return

    Column {
        Text(text = stringResource(id = R.string.brightness))
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = viewState.brightness,
            onValueChange = { onAction.invoke(ControllerAction.ChangeBrightness(it)) },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            valueRange = 0f..1f
        )

        HueColor(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp).height(40.dp)
        ) { color ->
            handleColorChange(context = context, color = color, onAction = onAction)
        }

        LazyVerticalGrid( columns = GridCells.Fixed(5)) {
            items(colorButtonsList) { color ->
                Box(
                    modifier = Modifier
                        .background(color)
                        .clickable {
                            handleColorChange(context = context, color = color.toIntHexColor(), onAction = onAction)
                        }
                        .height(50.dp)
                        .width(50.dp)
                )
            }
        }
    }
}

fun handleColorChange(context: Context, color: Int, onAction: (ControllerAction) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    onAction.invoke(ControllerAction.ChangeColor(color))
}

private fun generateColorPlate(): List<Color> {
    val list = arrayListOf<Color>()
    val saturation = 1f
    val lightness = 1f
    list.add(Color.White)

    for (i in 0 ..360 step 42) {
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

