package com.example.kidsdrawingapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.example.kidsdrawingapp.handler.CustomTouchHandler
import com.example.kidsdrawingapp.handler.TouchHandler
import com.example.kidsdrawingapp.model.TouchEvent

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var brushType: BrushType?= null
    private var drawPath: CustomPath? = null
    private var drawPaint: Paint? = null
    private var canvasPaint: Paint? = null
    private var canvasBitmap: Bitmap? = null
    private var drawCanvas: Canvas? = null

    private var brushSize: Float = 0f
    private var brushColor = Color.BLACK
    private val paths = ArrayList<CustomPath>()
    private val scaledBitmapCache = mutableMapOf<Float, Bitmap>()

    private var pencilTexture: Bitmap? = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
        eraseColor(Color.TRANSPARENT)
    }
    private var touchHandler: TouchHandler? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setupDrawing()
        drawCanvas = Canvas()
        drawPaint = Paint().apply {
            color = brushColor
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        touchHandler = CustomTouchHandler(
            step = 5f,
            nextHandler = object : TouchHandler {
                override fun handleFirstTouch(event: TouchEvent) {}
                override fun handleTouch(event: TouchEvent) {}
                override fun handleLastTouch(event: TouchEvent) {}
            },
            drawCanvas = drawCanvas!!,
            drawPaint = drawPaint!!,
            brushType = BrushType.PENCIL,
            pencilTexture = pencilTexture!!,
            color = Color.BLACK
        )

    }

   private fun setupDrawing() {

        drawPath = CustomPath().apply {
            color = brushColor
            brushThickness = brushSize
            type = brushType
        }
   }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (touchHandler == null) {
                    touchHandler = CustomTouchHandler(
                        step = 5f,
                        nextHandler = object : TouchHandler {
                            override fun handleFirstTouch(event: TouchEvent) {}
                            override fun handleTouch(event: TouchEvent) {}
                            override fun handleLastTouch(event: TouchEvent) {}
                        },
                        drawCanvas = drawCanvas!!,
                        drawPaint = drawPaint!!,
                        brushType = brushType!!,
                        pencilTexture = pencilTexture!!,
                        color = brushColor
                    )
                }
                touchHandler?.handleFirstTouch(TouchEvent(event.x, event.y))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    // Son dokunuşu işleyin
                    touchHandler?.handleLastTouch(TouchEvent(event.x, event.y))
                } else {
                    // Dokunuşu iptal et
                   // touchHandler?.cancel()
                }
                touchHandler = null
            }
            else -> {
                // Diğer dokunuşları işleyin
                touchHandler?.handleTouch(TouchEvent(event.x, event.y))
            }
        }
        invalidate()
        return true
    }


    fun setBrushSize(newSize: Float) {
        brushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics
        )
        drawPaint?.strokeWidth = brushSize
        drawPath?.brushThickness = brushSize
    }

    fun setBrushColor(newColor: String) {
        brushColor = Color.parseColor(newColor)
    }

    fun setBrushType(type: BrushType) {
        brushType = type
        pencilTexture = type.textureResId?.let {
            BitmapFactory.decodeResource(context.resources, it).let { bitmap ->
                val maxSize = 256
                val scale = minOf(maxSize / bitmap.width.toFloat(), maxSize / bitmap.height.toFloat())
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            }
        }
    }


    inner class CustomPath : Path() {
        var brushThickness: Float = 0f
        var color: Int = Color.BLACK
        var type: BrushType? = null
    }
}
