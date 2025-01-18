package com.example.kidsdrawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    var customProgressDialog: Dialog? = null
    var customBackgroundView: FrameLayout? = null
    private var isSaveAction = false
    var penWeight: View? = null
    var colorSelectionPopupItems: View? = null
    var icDrawEraser: ImageButton? = null
    var icDrawColor: LottieAnimationView? = null
    var icDrawPen: ImageButton? = null
    var icDrawLineWeight: LottieAnimationView? = null
    var paintSettingsLayout: View? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBackground: ImageView = findViewById(R.id.iv_background)
                imageBackground.setImageURI(result.data?.data)
            }
        }

    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    Toast.makeText(
                        this,
                        "Permission granted",
                        Toast.LENGTH_LONG
                    ).show()
                    if (isSaveAction) {
                        saveDrawing()
                    } else {
                        val pickIntent =
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)
                    }

                } else {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "OOps you jus denide the permission",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawingView)
        drawingView?.setSizeForBrush(10.toFloat())
        paintSettingsLayout = findViewById(R.id.paint_settings)
        icDrawLineWeight = paintSettingsLayout?.findViewById(R.id.ic_draw_line_weight)
        icDrawPen = paintSettingsLayout?.findViewById(R.id.ic_draw_pen)
        icDrawColor = paintSettingsLayout?.findViewById(R.id.lottie_draw_color)
        icDrawEraser = paintSettingsLayout?.findViewById(R.id.ic_draw_eraser)
        colorSelectionPopupItems = findViewById(R.id.color_selection_popup_items)
        penWeight = findViewById(R.id.brush_size)

        val gridLayoutColors: GridLayout = findViewById(R.id.grid_color_palette)
        customBackgroundView = findViewById(R.id.customBackgroundView)

        mImageButtonCurrentPaint = gridLayoutColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_pressed
            )
        )

        val ibGallery: ImageButton = findViewById(R.id.ib_save)
        ibGallery.setOnClickListener {
            isSaveAction = false
            if (isReadStorageAllowed()) {
                val pickIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickIntent)
            } else {
                requestStoragePermission()
            }
        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {
            isSaveAction = true
            if (isReadStorageAllowed()) {
                saveDrawing()
            } else {
                requestStoragePermission()
            }
        }


        icDrawColor?.setAnimation(R.raw.lottie_select_color)
        icDrawColor?.setOnClickListener {
            icDrawColor?.playAnimation()
            customBackgroundView?.visibility = View.VISIBLE
            colorSelectionPopupItems?.visibility = View.VISIBLE
            penWeight?.visibility = View.GONE
        }
        icDrawLineWeight?.post {
            icDrawLineWeight?.setProgress(1.0f)
        }
        icDrawLineWeight?.setAnimation(R.raw.lottie_line)
        icDrawLineWeight?.setOnClickListener {
            icDrawLineWeight?.playAnimation()
            showBrushSizeChooserDialog()
            customBackgroundView?.visibility = View.VISIBLE
            colorSelectionPopupItems?.visibility = View.GONE
            penWeight?.visibility = View.VISIBLE
        }

    }

    private fun saveDrawing() {
        showProgressDialog()
        lifecycleScope.launch {
            val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
            saveBitmapFile(getBitmapFromView(flDrawingView))
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            )
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                showRationalDialog(
                    "Kids Drawing App",
                    "This app needs access to your storage to load images."
                )
            } else {
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun showBrushSizeChooserDialog() {

        val smallBtn = penWeight?.findViewById<ImageView>(R.id.ib_small_brush)
        val mediumBtn = penWeight?.findViewById<ImageView>(R.id.ib_medium_brush)
        val largeBtn = penWeight?.findViewById<ImageView>(R.id.ib_large_brush)


        smallBtn?.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            customBackgroundView?.visibility = View.INVISIBLE

        }
        mediumBtn?.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            customBackgroundView?.visibility = View.INVISIBLE
        }
        largeBtn?.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            customBackgroundView?.visibility = View.INVISIBLE
        }
    }

    fun paintClicked(view: View) {
        if (view != mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.colorForBrush(colorTag)

            //selected
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))

            //unselected
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )

            mImageButtonCurrentPaint = view
        }
        customBackgroundView?.visibility = View.INVISIBLE
    }

    private fun showRationalDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()

    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f =
                        File(
                            externalCacheDir?.absoluteFile.toString()
                                    + File.separator + "KidDrawingApp_"
                                    + System.currentTimeMillis() / 1000
                                    + ".png"
                        )
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully: $result",
                                Toast.LENGTH_SHORT
                            ).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Someting went wrong while saving the file",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String) {
        MediaScannerConnection.scanFile(this, arrayOf(result), null) { path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }
}