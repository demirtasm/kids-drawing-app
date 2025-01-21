package com.example.kidsdrawingapp.handler

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.Log
import com.example.kidsdrawingapp.BrushType
import com.example.kidsdrawingapp.model.TouchEvent
import kotlin.math.ceil
import kotlin.math.max

class CustomTouchHandler(
    private val step: Float= 10f ,
    private val nextHandler: TouchHandler,
    private val drawCanvas: Canvas,
    private val drawPaint: Paint,
    private var brushType: BrushType,
    private val pencilTexture: Bitmap?,
    private var color: Int
) : TouchHandler {

    private val event0 = TouchEvent()
    private val event1 = TouchEvent()
    private val event2 = TouchEvent()

    private val interpolatedEvent = TouchEvent()
    private val scaledBitmapCache = mutableMapOf<Float, Bitmap>()

    override fun handleFirstTouch(event: TouchEvent) {
        drawPaint.alpha = brushType.opacity
       // drawPaint.strokeWidth = brushType.size * 100
        event0.set(event)
        event1.set(event)
        nextHandler.handleFirstTouch(event)
    }

    override fun handleTouch(event: TouchEvent) {
        drawPaint.color = color
        when (brushType) {
            BrushType.WATER_COLOUR -> {
                drawPath(event, isWaterColor = true)
            }
            BrushType.PENCIL -> {
                drawPath(event, isWaterColor = false)
            }
            BrushType.MARKER -> {
                drawMarkerPath(event)
            }
            else -> {
                Log.e("TGGX","ELSE")

            }
        }
    }
    private fun drawPath(event: TouchEvent, isWaterColor: Boolean) {
        event2.x = (event1.x + event.x) / 2f
        event2.y = (event1.y + event.y) / 2f

        val pointCount = max(1, ceil(event0.distanceTo(event2) / step).toInt())
        for (n in 1 until pointCount step 2) {
            val t = n.toFloat() / pointCount.toFloat()
            val tSqr = t * t
            val tPrime = 1 - t
            val tPrimeSqr = tPrime * tPrime

            interpolatedEvent.x = tSqr * event2.x + 2 * t * tPrime * event1.x + tPrimeSqr * event0.x
            interpolatedEvent.y = tSqr * event2.y + 2 * t * tPrime * event1.y + tPrimeSqr * event0.y

            val scaledTexture = if (brushType != BrushType.MARKER) {
                getScaledTexture(drawPaint.strokeWidth)
            } else null

            val randomOffsetX = if (!isWaterColor && brushType != BrushType.MARKER) {
                (Math.random().toFloat() - 0.5f) * drawPaint.strokeWidth * 0.4f
            } else 0f
            val randomOffsetY = if (!isWaterColor && brushType != BrushType.MARKER) {
                (Math.random().toFloat() - 0.5f) * drawPaint.strokeWidth * 0.4f
            } else 0f

            val coloredTexture = if (scaledTexture != null) {
                getColoredBitmap(scaledTexture, drawPaint.color)
            } else null

            if (coloredTexture != null) {
                drawCanvas.drawBitmap(
                    coloredTexture,
                    interpolatedEvent.x - scaledTexture?.width!! / 2 + randomOffsetX,
                    interpolatedEvent.y - scaledTexture.height / 2 + randomOffsetY,
                    drawPaint
                )
            } else {
                // Marker için düz çizim
                drawCanvas.drawCircle(
                    interpolatedEvent.x,
                    interpolatedEvent.y,
                    drawPaint.strokeWidth * brushType.size, // Marker boyutu
                    drawPaint
                )
            }
        }

        nextHandler.handleTouch(event2)
        event0.set(event2)
        event1.set(event)
    }
    private fun drawMarkerPath(event: TouchEvent) {
        event2.x = (event1.x + event.x) / 2f
        event2.y = (event1.y + event.y) / 2f

        val pointCount = max(1, ceil(event0.distanceTo(event2) / step).toInt())
        for (n in 1 until pointCount step 2) {
            val t = n.toFloat() / pointCount.toFloat()
            val tSqr = t * t
            val tPrime = 1 - t
            val tPrimeSqr = tPrime * tPrime

            interpolatedEvent.x = tSqr * event2.x + 2 * t * tPrime * event1.x + tPrimeSqr * event0.x
            interpolatedEvent.y = tSqr * event2.y + 2 * t * tPrime * event1.y + tPrimeSqr * event0.y

            val markerTexture = getMarkerTexture(drawPaint.strokeWidth)

            // Marker dokusunu çiz
            drawCanvas.drawBitmap(
                markerTexture,
                interpolatedEvent.x - markerTexture.width / 2,
                interpolatedEvent.y - markerTexture.height / 2,
                drawPaint
            )
        }

        nextHandler.handleTouch(event2)
        event0.set(event2)
        event1.set(event)
    }
    private fun getMarkerTexture(brushThickness: Float): Bitmap {
        val width = (brushThickness * 5).toInt()
        val height = (brushThickness * 1.5).toInt()
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val paint = Paint().apply {
                color = this@CustomTouchHandler.color
                alpha = 100
                isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint) // Dikdörtgen doku
        }
    }

    private fun getColoredBitmap(originalBitmap: Bitmap, color: Int): Bitmap {
        val coloredBitmap = Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        // Yeni bitmap üzerine çizim yapmak için bir canvas oluştur
        val canvas = Canvas(coloredBitmap)

        // Renk filtresiyle renklendirmek için bir paint nesnesi
        val paint = Paint().apply {
            isAntiAlias = true // Daha düzgün çizim için anti-aliasing etkinleştir
            colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }

        // Orijinal bitmap'i boyanmış haliyle yeni bitmap üzerine çiz
        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)

        return coloredBitmap
    }

    private fun getScaledTexture(brushThickness: Float): Bitmap {
        return scaledBitmapCache[brushThickness] ?: synchronized(this) {
            scaledBitmapCache[brushThickness] ?: Bitmap.createScaledBitmap(
                pencilTexture!!, // Dokunun orijinal Bitmap'i
                (brushThickness * 5).toInt(), // Genişlik: Fırça kalınlığına göre ölçekleniyor
                (brushThickness * 5).toInt(), // Yükseklik: Fırça kalınlığına göre ölçekleniyor
                false // Daha yüksek kalitede ölçekleme
            ).also {
                scaledBitmapCache[brushThickness] = it // Ölçeklenmiş dokuyu önbelleğe kaydediyoruz
            }
        }
    }

    override fun handleLastTouch(event: TouchEvent) {

        val pointCount = ceil(event0.distanceTo(event) / step).toInt()
        for (n in 1 until pointCount step 2) {
            val t = n.toFloat() / pointCount.toFloat()
            val tSqr = t * t
            val tPrime = 1 - t
            val tPrimeSqr = tPrime * tPrime

            interpolatedEvent.x = tSqr * event.x + 2 * t * tPrime * event1.x + tPrimeSqr * event0.x
            interpolatedEvent.y = tSqr * event.y + 2 * t * tPrime * event1.y + tPrimeSqr * event0.y
            nextHandler.handleTouch(interpolatedEvent)
            drawCanvas.drawCircle(interpolatedEvent.x, interpolatedEvent.y, drawPaint.strokeWidth, drawPaint)
        }
        nextHandler.handleLastTouch(event)
    }
}
