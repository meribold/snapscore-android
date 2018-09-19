package xyz.meribold.snapscore

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.graphics.Bitmap

class CamActivity : AppCompatActivity() {
    private var photo: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cam)
        if (photo == null){
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
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val extras = data!!.extras
            photo = extras!!.get("data") as Bitmap
        }
    }
}
