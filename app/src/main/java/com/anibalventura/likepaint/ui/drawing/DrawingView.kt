package com.anibalventura.likepaint.ui.drawing

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import java.io.OutputStream

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {


    private var canvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null

    private var drawPaint: Paint? = null
    private var canvasPaint: Paint? = null

    private var drawPath: CustomPath? = null
    private var paths = ArrayList<CustomPath>()
    private var undonePaths = ArrayList<CustomPath>()

    private var brushSize: Float = 10.toFloat()
    private var eraserSize: Float = 10.toFloat()
    private var brushColor: Int = Color.BLACK

    lateinit var result: String

    init {
        setUpDrawing()
    }

    // This method initializes the attributes of the View for DrawingView class.
    private fun setUpDrawing() {
        drawPaint = Paint()
        drawPath = CustomPath(brushColor, brushSize)

        drawPaint!!.color = brushColor
        drawPaint!!.style = Paint.Style.STROKE
        drawPaint!!.strokeJoin = Paint.Join.ROUND
        drawPaint!!.strokeCap = Paint.Cap.ROUND

        canvasPaint = Paint(Paint.DITHER_FLAG)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)
    }

    /**
     * This method is called when a stroke is drawn on the canvas
     * as a part of the painting.
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        /**
         * Draw the specified bitmap, with its top/left corner at (x,y), using the specified paint,
         * transformed by the current matrix.
         *
         *If the bitmap and canvas have different densities, this function will take care of
         * automatically scaling the bitmap to draw at the same density as the canvas.
         *
         * @param bitmap The bitmap to be drawn
         * @param left The position of the left side of the bitmap being drawn
         * @param top The position of the top side of the bitmap being drawn
         * @param paint The paint used to draw the bitmap (may be null)
         */
        canvas!!.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)

        for (path in paths) {
            drawPaint!!.strokeWidth = path.brushThickness
            drawPaint!!.color = path.color
            canvas.drawPath(path, drawPaint!!)
        }

        if (!drawPath!!.isEmpty) {
            drawPaint!!.strokeWidth = drawPath!!.brushThickness
            drawPaint!!.color = drawPath!!.color
            canvas.drawPath(drawPath!!, drawPaint!!)
        }
    }

    /**
     * This method acts as an event listener when a touch
     * event is detected on the device.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath!!.color = brushColor
                drawPath!!.brushThickness = brushSize

                // Clear any lines and curves from the path, making it empty.
                drawPath!!.reset()
                // Set the beginning of the next contour to the point (x,y).
                drawPath!!.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> {
                // Add a line from the last point to the specified point (x,y).
                drawPath!!.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                paths.add(drawPath!!)
                drawPath = CustomPath(brushColor, brushSize)
            }
            else -> return false
        }

        invalidate()
        return true
    }

    fun setBrushSize(newSize: Float) {
        // Set size based on screen dimension.
        brushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics
        )

        drawPaint!!.strokeWidth = brushSize
        drawPaint!!.color = brushColor
    }

    fun setBrushColor(newColor: Int) {
        brushColor = newColor
        drawPaint!!.color = brushColor
    }

    fun setEraserSize(newSize: Float) {
        // Set size based on screen dimension.
        eraserSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics
        )

        drawPaint!!.strokeWidth = eraserSize
    }

    fun undoPath() {
        when {
            paths.size > 0 -> {
                undonePaths.add(paths.removeAt(paths.size - 1))
                invalidate()
            }
        }
    }

    fun redoPath() {
        when {
            undonePaths.size > 0 -> {
                paths.add(undonePaths.removeAt(undonePaths.size - 1))
                invalidate()
            }
        }
    }

    fun saveBitmap(bitmap: Bitmap): String {
        val resolver = context.contentResolver
        val fileName = "LikePaint_${System.currentTimeMillis() / 1000}.png"
        val saveLocation = Environment.DIRECTORY_PICTURES

        Toast.makeText(context, "Drawing saved on: $saveLocation", Toast.LENGTH_LONG)
            .show()

        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            saveDrawingToStream(bitmap, resolver.openOutputStream(uri))
            context.contentResolver.update(uri, values, null, null)
            result = uri.toString()
        }

        return result
    }

    fun getBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawing = view.background

        when {
            bgDrawing != null -> bgDrawing.draw(canvas)
            else -> canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return bitmap
    }

    private fun saveDrawingToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                Log.e("**Exception", "Could not write to stream")
                e.printStackTrace()
            }
        }
    }

    // An inner class for custom path with two params as color and stroke size.
    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path()
}