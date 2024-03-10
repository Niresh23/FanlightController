package com.niresh23.fanlightcontroller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@Composable
fun ExoLightHead(modifier: Modifier = Modifier, color: Color = Color.White, strokeWidth: Dp = 5.dp) {
    Canvas(modifier = modifier) {
        val maxSize = if (size.width > size.height) {
            size.width
        } else {
            size.height
        }

        val xOffset = if (size.width > size.height) {
            (size.width - size.height) / 2
        } else {
            0
        }

        val yOffset = if (size.height > size.width) {
            (size.height - size.width) / 2
        } else {
            0
        }

        val t = maxSize / 2

        val delta = 10.dp.value


        //draw E
        //draw /
        val k = maxSize / 4
        drawLine(color = color, strokeWidth = strokeWidth.value, start = Offset(0f, maxSize / 2), end = Offset(k - delta, delta))
        //draw \
        drawLine(color = color, strokeWidth = strokeWidth.value, start = Offset(0f, maxSize / 2), end = Offset(k - delta, maxSize - delta))
        //draw -
        drawLine(color = color, strokeWidth = strokeWidth.value, start = Offset(0f, maxSize / 2), end = Offset(k, maxSize / 2))

        //draw X
        //draw \
        drawLine(color = color, strokeWidth = strokeWidth.value, start = Offset(k, 0f), end = Offset(k + t, maxSize))
        //draw /
        drawLine(color = color, strokeWidth = strokeWidth.value, start = Offset(k, maxSize), end = Offset(k + t, 0f))

        //draw >
        //draw \
        drawLine(color = color, strokeWidth = strokeWidth.value, start = Offset(k + t + delta, delta), end = Offset(maxSize, maxSize / 2))
        //draw /
        drawLine(color = color, strokeWidth = strokeWidth.value, start = Offset(k + t + delta, maxSize - delta), end = Offset(maxSize, maxSize / 2))
    }
}