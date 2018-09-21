package xyz.meribold.snapscore

import android.app.Activity
import android.content.Intent
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
        if (!photoRequestMade) {
            snap()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("photoRequestMade", photoRequestMade)
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
        imageView.setImageURI(photoUri)
    }
}
