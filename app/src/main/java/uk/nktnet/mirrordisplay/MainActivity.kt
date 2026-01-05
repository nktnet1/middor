package uk.nktnet.mirrordisplay

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi

class MainActivity : ComponentActivity() {

    private val REQ_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestScreenCapture()
    }

    private fun requestScreenCapture() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE)
    }

    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQ_CAPTURE) return
        if (resultCode != Activity.RESULT_OK || data == null) {
            finish()
            return
        }

        // Start the mirror service
        val serviceIntent = Intent(this, MirrorService::class.java).apply {
            putExtra("code", resultCode)
            putExtra("data", data)
        }

        startForegroundService(serviceIntent)
        finish() // Close activity immediately
    }
}
