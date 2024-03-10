package com.niresh23.fanlightcontroller.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color.HSVToColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.niresh23.fanlightcontroller.viewmodel.FanlightViewModel

@Composable
fun ControlScreen(
    fanlightViewModel: FanlightViewModel
) {
    val colorButtonsList = generateColorPlate()
    val context = LocalContext.current
    val colorState = fanlightViewModel.colorFlowState.value
    Column {
        Text(text = "Connected")
        ExoLightHead(modifier = Modifier.height(50.dp).width(50.dp), color = colorState.toComposeColor())
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
                        fanlightViewModel.startVisualizer()
                    }

                    override fun onPermissionDenied(var1: PermissionDeniedResponse?) {

                    }

                    override fun onPermissionRationaleShouldBeShown(
                        var1: PermissionRequest?,
                        var2: PermissionToken?
                    ) {

                    }
                }).check()
            }) {
                Text("Start Visualizer")
            }
            Button(onClick = {
                fanlightViewModel.stopVisualizer()
            }) {
                Text("Stop Visualizer")
            }
            Button(onClick = { fanlightViewModel.testFunction(4995634) }) {
                Text(text = "Test color")
            }
        }

        LazyVerticalGrid( columns = GridCells.Fixed(5)) {
            items(colorButtonsList) { color ->
                Box(
                    modifier = Modifier
                        .background(color)
                        .clickable {
                            fanlightViewModel.colorChangeMapping(color.toIntHexColor())
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
