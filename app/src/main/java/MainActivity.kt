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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        photoRequestMade = savedInstanceState?.getBoolean("photoRequestMade") ?: false
        photoUri = savedInstanceState?.getParcelable("photoUri")
        if (!photoRequestMade) {
            snap()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("photoRequestMade", photoRequestMade)
        outState.putParcelable("photoUri", photoUri)
    }

    // See <https://developer.android.com/training/camera/photobasics>.
    private fun snap() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) == null) {
            TODO()
        }
        val photoFile: File?
        try {
            photoFile = File.createTempFile("photo_", ".jpg", getExternalFilesDir(null))
        } catch (e: IOException) {
            TODO()
        }
        // See <https://stackoverflow.com/a/44212615>.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            photoUri = Uri.fromFile(photoFile)
        } else {
            photoUri = FileProvider.getUriForFile(this,
                "xyz.meribold.snapscore.fileprovider", photoFile)
        }
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(takePictureIntent, 1)
        photoRequestMade = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        assert(requestCode == 1)
        if (resultCode != Activity.RESULT_OK) {
            finish()
        }
        if (photoUri == null) {
            finish()
        }
        val bitmap = BitmapFactory.decodeFile(photoUri!!.getPath())
        imageView.setImageBitmap(fixBitmapOrientation(bitmap))
    }

    private fun fixBitmapOrientation(bitmap: Bitmap): Bitmap {
        val exif = ExifInterface(photoUri!!.path)
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
}
