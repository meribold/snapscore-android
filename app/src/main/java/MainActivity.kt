package xyz.meribold.snapscore

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.graphics.Bitmap
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var photo: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (photo == null) {
            snap()
        }
    }

    // See <https://developer.android.com/training/camera/photobasics>.
    private fun snap() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        assert(requestCode == 1)
        if (resultCode != Activity.RESULT_OK) {
            finish()
        }
        photo = data?.extras?.get("data") as Bitmap?
        if (photo == null) {
            finish()
        }
        imageView.setImageBitmap(photo)
    }
}
