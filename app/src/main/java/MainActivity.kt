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
import java.io.File
import java.io.IOException
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var photoRequestMade = false
    private var photoUri: Uri? = null
    private var photoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        photoRequestMade = savedInstanceState?.getBoolean("photoRequestMade") ?: false
        photoUri = savedInstanceState?.getParcelable("photoUri")
        photoFile = savedInstanceState?.getSerializable("photoFile") as File?
        if (!photoRequestMade) {
            snap()
        }
        photoFile?.absolutePath?.let { showBitmap(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("photoRequestMade", photoRequestMade)
        outState.putParcelable("photoUri", photoUri)
        outState.putSerializable("photoFile", photoFile)
    }

    // See <https://developer.android.com/training/camera/photobasics>.
    private fun snap() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) == null) {
            TODO()
        }
        photoFile = File(getExternalCacheDir(), "photo.jpg").apply {
            try {
                // If the file already exists, this returns `false`.  We don't care.
                createNewFile()
            } catch (e: IOException) {
                TODO()
            }
        }
        // See <https://stackoverflow.com/a/44212615>.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            photoUri = Uri.fromFile(photoFile!!)
        } else {
            // This seems to work on Android 5.0 (API 21).
            photoUri = FileProvider.getUriForFile(this,
                "xyz.meribold.snapscore.fileprovider", photoFile!!)
        }
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(takePictureIntent, 1)
        photoRequestMade = true
    }

    private fun showBitmap(path: String) {
        val bitmap: Bitmap? = BitmapFactory.decodeFile(path)
        bitmap?.let { imageView.setImageBitmap(fixBitmapOrientation(it, path)) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        assert(requestCode == 1)
        if (resultCode != Activity.RESULT_OK) {
            finish()
        }
        if (photoUri == null) {
            finish()
        }
        photoFile?.absolutePath?.let { showBitmap(it) }
    }
}

private fun fixBitmapOrientation(bitmap: Bitmap, path: String): Bitmap {
    val exif = ExifInterface(path)
    val orientation: Int = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                                ExifInterface.ORIENTATION_NORMAL)
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90  -> rotateBitmap(bitmap, 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                                        else -> bitmap
    }
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
