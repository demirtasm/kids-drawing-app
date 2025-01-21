package com.example.kidsdrawingapp

enum class BrushType(
    val textureResId: Int?,
    val size: Float,
    val spacing: Float,
    val flow: Float,
    val rotationRandomness: Float = 0f ,
    val opacity: Int
) {
    PENCIL(R.drawable.stamp_pencil, size = 0.1f, spacing = 0.15f, flow = 1.0f, rotationRandomness = 1f, opacity = 255),
    MARKER(R.drawable.stamp_marker, size = 0.4f, spacing = 0.15f, flow = 1f, opacity = 2),
    WATER_COLOUR(R.drawable.stamp_pencil, size = 0.3f, spacing = 0.2f, flow = 0.5f, opacity = 150)
}