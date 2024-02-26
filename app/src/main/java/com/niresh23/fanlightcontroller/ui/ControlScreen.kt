package com.niresh23.fanlightcontroller.ui

import android.graphics.Color.HSVToColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.niresh23.fanlightcontroller.viewmodel.FanlightViewModel

@Composable
fun ControlScreen(
    fanlightViewModel: FanlightViewModel
) {
    val colorState = fanlightViewModel.colorFlowState.collectAsState()
    val colorButtonsList = generateColorPlate()

    Column {
        Text(text = "Connected")
        LazyVerticalGrid( columns = GridCells.Fixed(5)) {
            items(colorButtonsList) { color ->
                Box(
                    modifier = Modifier
                        .background(color)
                        .clickable {
                            val colorValue = color.value
                            val result = (colorValue and 0x00FFFFFFFFFFFFFFUL shr 32).toString()
                            fanlightViewModel.colorChangeMapping(result.toInt())
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

    for (i in 0 ..360 step 10) {
        val hsv = HSVToColor(
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
// 16711680 red
// 65280 green
// 255 blue
data class ColorData(val color: Int)
