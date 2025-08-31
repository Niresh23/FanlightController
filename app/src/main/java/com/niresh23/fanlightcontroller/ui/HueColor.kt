package com.niresh23.fanlightcontroller.ui

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toRect
import kotlinx.coroutines.CoroutineScope

@Composable
fun HueColor(
    modifier: Modifier =
        Modifier
            .fillMaxWidth()
            .height(40.dp),
    setColor: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val cursorWidthDp = 5.dp
    val cursorWidthPx = LocalDensity.current.run { cursorWidthDp.toPx() }

    val pressOffset = remember {
        mutableStateOf(Offset.Zero)
    }

    val interactionSource = remember {
        MutableInteractionSource()
    }

    var minX by remember {
        mutableFloatStateOf(0f)
    }

    var maxX by remember {
        mutableFloatStateOf(0f)
    }

    var columnHeightPx by remember {
        mutableFloatStateOf(0f)
    }

    var columnWidthPx by remember {
        mutableFloatStateOf(0f)
    }

    var hueColors = IntArray(0)
    Box(modifier = modifier
        .emitDragGesture(interactionSource)
        ) {

        Canvas(
            modifier = Modifier.fillMaxSize().padding(vertical = 10.dp).clip(RoundedCornerShape(50))
                .onGloballyPositioned { coordinates ->
                    columnHeightPx = coordinates.size.height.toFloat()
                    columnWidthPx = coordinates.size.width.toFloat()

                    maxX = coordinates.boundsInWindow().right
                    minX = coordinates.boundsInWindow().left

                    pressOffset.value = Offset(coordinates.size.height.toFloat() / 2, 0f)
                }
        ) {
            val bitmap = createBitmap(size.width.toInt(), size.height.toInt())
            val hueCanvas = android.graphics.Canvas(bitmap)
            val huePanel = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            hueColors = IntArray(huePanel.width().toInt())

            var hue = 0f
            var saturation = 0f

            for (i in hueColors.indices) {
                if (i < (huePanel.height() / 2) + 1) {
                    hueColors[i] = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f))
                    saturation = i / (huePanel.height() / 2)
                } else {
                    hue += 360f / hueColors.size
                    hueColors[i] = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                }
            }

            val linePaint = android.graphics.Paint()
            linePaint.strokeWidth = 0F

            for (i in hueColors.indices) {
                linePaint.color = hueColors[i]
                hueCanvas.drawLine(i.toFloat(), 0F, i.toFloat(), huePanel.bottom, linePaint)
            }

            drawBitmap(
                bitmap = bitmap,
                panel = huePanel
            )
        }
        // Cursor
        Canvas(
            modifier = Modifier.fillMaxHeight().width(cursorWidthDp)
        ) {
            drawRect(
                color = hueColors[pressOffset.value.x.toInt()].toComposeColor(),
                topLeft = Offset(pressOffset.value.x, 0f),
                size = _root_ide_package_.androidx.compose.ui.geometry.Size(size.width, size.height)
            )
        }

        fun pointToHue(pointX: Float): Int {
            return hueColors[pointX.toInt()]
        }

        scope.collectForPress(interactionSource) { pressPosition ->
            val pressPos = pressPosition.x.coerceIn(0f, columnWidthPx - cursorWidthPx)
            pressOffset.value = Offset(pressPos, 0f)
            setColor(pointToHue(pressPos))
        }
    }
}

fun CoroutineScope.collectForPress(
    interactionSource: InteractionSource,
    setOffset: (Offset) -> Unit
) {
    launch {
        interactionSource.interactions.collect { interaction ->
            (interaction as? PressInteraction.Press)
                ?.pressPosition
                ?.let(setOffset)
        }
    }
}

private fun Modifier.emitDragGesture(
    interactionSource: MutableInteractionSource
): Modifier = composed {
    val scope = rememberCoroutineScope()

    pointerInput(Unit) {
        detectDragGestures { input, _ ->
            scope.launch {
                interactionSource.emit(PressInteraction.Press(input.position))
            }
        }
    }
}

private fun DrawScope.drawBitmap(
    bitmap: Bitmap,
    panel: RectF
) {
    drawIntoCanvas {
        it.nativeCanvas.drawBitmap(
            bitmap,
            null,
            panel.toRect(),
            null
        )
    }
}