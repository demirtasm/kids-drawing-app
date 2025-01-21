package com.example.kidsdrawingapp.model

import kotlin.math.pow

data class TouchEvent(
    var x: Float = 0f,
    var y: Float = 0f,
    var p: Float = 0f
) {
    fun set(event: TouchEvent) {
        x = event.x
        y = event.y
        p = event.p
    }

    fun distanceTo(event: TouchEvent): Float {
        return Math.sqrt(((x - event.x).toDouble().pow(2) + (y - event.y).toDouble().pow(2))).toFloat()
    }
}
