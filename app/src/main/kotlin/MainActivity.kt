package xyz.meribold.snapscore

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var photoRequestMade = false
    private val photoFile: File by lazy {
        File(externalCacheDir, "photo.jpg").also {
            try {
                // If the file already exists, this returns `false`.  That's fine, though.
                it.createNewFile()
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "Creating file for photo failed.",
                               Toast.LENGTH_LONG).show()
            } catch (e: SecurityException) {
                Toast.makeText(applicationContext, "Creating file for photo was denied.",
                               Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        photoRequestMade = savedInstanceState?.getBoolean("photoRequestMade") ?: false
        if (savedInstanceState?.containsKey("score") == true) {
            scoreTV.text = "${savedInstanceState.getInt("score")}"
        }
        camFab.setOnClickListener { snap() }
    }

    override fun onStart() {
        super.onStart()
        if (!photoRequestMade) {
            snap()
        } else {
            showBitmap(photoFile.absolutePath)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("photoRequestMade", photoRequestMade)
        scoreTV.text.toString().toIntOrNull()?.let { outState.putInt("score", it) }
    }

    private fun snap() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) == null) {
            TODO()
        }
        // See <https://stackoverflow.com/a/44212615>.
        val photoUri: Uri = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            // I think this should work up to, but not on, Android 7.0.  See [1].  We only
            // do things this way on Android 4.4, though.
            Uri.fromFile(photoFile)
        } else {
            // This seems to work on Android 5.0 (API 21).  I thought it may not because
            // the docs for `FileProvider` say "added in version 22.1.0".  I guess that
            // doesn't refer to the API level.
            FileProvider.getUriForFile(this, "xyz.meribold.snapscore.fileprovider",
                                       photoFile)
        }
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(takePictureIntent, 1)
        photoRequestMade = true
    }
    // [1]: https://developer.android.com/training/camera/photobasics

    // This shows the bitmap without wasting too much memory.  See [1].
    private fun showBitmap(path: String) {
        // Get the display size.  What I really would like to have is the size the
        // `ImageView` *could* have in the current layout (i.e., the size it would have
        // when showing an arbitrarily big image), but I don't know how to get that.
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val availableWidth  = displayMetrics.widthPixels
        val availableHeight = displayMetrics.heightPixels

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        val orientation: Int = getImageOrientation(path)
        val swapWidthAndHeight = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                                 orientation == ExifInterface.ORIENTATION_ROTATE_270
        options.inSampleSize = if (swapWidthAndHeight) {
            calculateInSampleSize(options.outHeight, options.outWidth,
                                  availableWidth, availableHeight)
        } else {
            calculateInSampleSize(options.outWidth, options.outHeight,
                                  availableWidth, availableHeight)
        }
        options.inJustDecodeBounds = false
        val bitmap: Bitmap? = BitmapFactory.decodeFile(path, options)
        if (bitmap == null) {
            Toast.makeText(applicationContext, "Decoding bitmap failed.",
                           Toast.LENGTH_LONG).show()
        } else {
            imageView.setImageBitmap(when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90  -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                                                else -> bitmap
            })
        }
    }
    // [1]: https://developer.android.com/topic/performance/graphics/load-bitmap

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        assert(requestCode == 1)
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(applicationContext, "Failed to get a photo.",
                           Toast.LENGTH_LONG).show()
        } else {
            scoreTV.text = null
            NetworkTask(WeakReference(this)).execute(photoFile)
        }
    }
}

private fun getImageOrientation(path: String): Int =
    ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                        ExifInterface.ORIENTATION_NORMAL)

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// This is similar to the function with the same name at [1], but returns a value for more
// aggressive subsampling.  The function at [1] still resulted in bitmaps that failed to
// display on a Galaxy S4 GT-I9500 because of being too big (apparently that phone doesn't
// like bitmaps with more than 4096 pixels along one dimension).
fun calculateInSampleSize(width: Int, height: Int, availableWidth: Int,
                          availableHeight: Int): Int {
    var inSampleSize = 1

    // Calculate the largest `inSampleSize` value that is a power of 2 and keeps the
    // height or width larger than 75% of the available height or width, respectively.  We
    // only require one dimension to stay larger than (75% of) its respective available
    // size because preserving the bitmap's aspect ratio implies having to resize based on
    // whatever dimension requires more aggressive downscaling.
    while (height / (2 * inSampleSize) > 0.75 * availableHeight ||
            width / (2 * inSampleSize) > 0.75 * availableWidth) {
        inSampleSize *= 2
    }
    return inSampleSize
}
// [1]: https://developer.android.com/topic/performance/graphics/load-bitmap
