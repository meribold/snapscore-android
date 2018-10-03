package xyz.meribold.snapscore

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
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
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import java.io.File
import java.io.IOException
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    // Did we request taking a photo at least once since the app was started?
    private var photoRequestMade = false
    // Did we get any photo since the app was started?
    private var newPhotoReceived = false
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
    private val model: MainViewModel by lazy {
        // See [2].  This gives us back the same `ViewModel` object we used to have if the
        // activity is recreated after configuration changes and such stuff.
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoRequestMade = savedInstanceState?.getBoolean("photoRequestMade") ?: false
        if (!photoRequestMade) {
            // Do this as early as possible because it may result in the activity being
            // destroyed.  In this case, `onCreate` will be called again and everything we
            // did so far was redundant.
            snap()
        }
        newPhotoReceived = savedInstanceState?.getBoolean("newPhotoReceived") ?: false
        setContentView(R.layout.activity_main)
        if (savedInstanceState?.containsKey("score") == true) {
            // This requires that we already called `setContentView`.  Else we can't use
            // `scoreTV` etc.
            scoreTV.text = "${savedInstanceState.getInt("score")}"
        }
        // "If LiveData already has data set, it will be delivered to the observer." [3]
        model.scoringPhase.observe(this, Observer { phase ->
            when (phase) {
                ScoringPhase.INACTIVE -> {
                    progressBar.visibility = GONE
                }
                ScoringPhase.CONNECTING -> {
                    progressBar.setIndeterminate(true)
                    progressBar.visibility = VISIBLE
                }
                ScoringPhase.UPLOADING -> {
                    progressBar.setIndeterminate(false)
                    progressBar.visibility = VISIBLE
                }
                ScoringPhase.AWAITING -> {
                    progressBar.setIndeterminate(true)
                    progressBar.visibility = VISIBLE
                }
            }
        })
        model.progress.observe(this, Observer { progress ->
            if (progress != null) {
                progressBar.progress = progress
            }
        })
        model.score.observe(this, Observer { score ->
            when (score) {
                null -> scoreTV.text = null
                else -> scoreTV.text = "$score"
            }
        })
        camFab.setOnClickListener { snap() }
    }

    override fun onStart() {
        super.onStart()
        if (photoRequestMade) {
            showBitmap(photoFile.absolutePath)
        }
    }

    // We still need saved instance state despite also using a `ViewModel`.  Our process
    // may be killed by the system causing all the data in the `ViewModel` to be lost.
    // The fact that this happened should not be visible to the user, though, and saved
    // instance state is one way to recover.  See [4] for more information.
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("photoRequestMade", photoRequestMade)
        outState.putBoolean("newPhotoReceived", newPhotoReceived)
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
        photoRequestMade = true
        // It may be best to have `startActivityForResult` be the last thing we do,
        // because maybe we can be killed as soon as we call it?
        startActivityForResult(takePictureIntent, 1)
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
            // The back button was probably pressed while the camera activity was active.
            if (!newPhotoReceived) {
                // If the app was freshly started and no photo was taken yet, the user
                // never saw this activity and it makes no sense to "return" to it.  We
                // close the app in this case.
                finish()
            } else {
                Toast.makeText(applicationContext, "Failed to get a photo.",
                               Toast.LENGTH_LONG).show()
            }
        } else {
            newPhotoReceived = true
            model.score.value = null
            model.kickOffScoring(photoFile)
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
// [2]: https://developer.android.com/topic/libraries/architecture/viewmodel
// [3]: https://developer.android.com/reference/android/arch/lifecycle/LiveData.html#observe
// [4]: https://developer.android.com/topic/libraries/architecture/saving-states
