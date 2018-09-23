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
import android.widget.Toast
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
        photoFile = savedInstanceState?.getSerializable("photoFile") as File? ?:
                    createPhotoFile()
        if (photoFile != null && !photoRequestMade) {
            snap()
        } else {
            photoFile?.absolutePath?.let { showBitmap(it) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("photoRequestMade", photoRequestMade)
        outState.putParcelable("photoUri", photoUri)
        outState.putSerializable("photoFile", photoFile)
    }

    private fun createPhotoFile(): File? {
        val file: File = File(getExternalCacheDir(), "photo.jpg")
        try {
            // If the file already exists, this returns `false`.  That's fine, though.
            file.createNewFile()
        } catch (e: IOException) {
            Toast.makeText(getApplicationContext(), "Creating file for photo failed.",
                           Toast.LENGTH_LONG).show()
            return null
        } catch (e: SecurityException) {
            Toast.makeText(getApplicationContext(), "Creating file for photo was denied.",
                           Toast.LENGTH_LONG).show()
            return null
        }
        return file
    }

    private fun snap() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) == null) {
            TODO()
        }
        // See <https://stackoverflow.com/a/44212615>.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            // I think this should work up to, but not on, Android 7.0.  See [1].  We only
            // do things this way on Android 4.4, though.
            photoUri = Uri.fromFile(photoFile!!)
        } else {
            // This seems to work on Android 5.0 (API 21).  I thought it may not because
            // the docs for `FileProvider` say "added in version 22.1.0".  I guess that
            // doesn't refer to the API level.
            photoUri = FileProvider.getUriForFile(this,
                "xyz.meribold.snapscore.fileprovider", photoFile!!)
        }
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(takePictureIntent, 1)
        photoRequestMade = true
    }
    // [1]: https://developer.android.com/training/camera/photobasics

    private fun showBitmap(path: String) {
        val bitmap: Bitmap? = BitmapFactory.decodeFile(path)
        if (bitmap == null) {
            Toast.makeText(getApplicationContext(), "Decoding bitmap failed.",
                           Toast.LENGTH_LONG).show()
        } else {
            imageView.setImageBitmap(fixBitmapOrientation(bitmap, path))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        assert(requestCode == 1)
        assert(photoUri != null)
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(getApplicationContext(), "Failed to get a photo.",
                           Toast.LENGTH_LONG).show()
        } else {
            photoFile?.absolutePath?.let { showBitmap(it) }
        }
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
