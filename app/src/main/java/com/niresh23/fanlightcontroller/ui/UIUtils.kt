package com.niresh23.fanlightcontroller.ui

import androidx.compose.ui.graphics.Color

//convert compose UI color to hex color 0x000000
fun Color.toIntHexColor(): Int {
    val colorValue = this.value
    return (colorValue and 0x00FFFFFFFFFFFFFFUL shr 32).toInt()
}

fun Int.toComposeColor(): Color {
    return Color(this or 0xFF000000.toInt())
}