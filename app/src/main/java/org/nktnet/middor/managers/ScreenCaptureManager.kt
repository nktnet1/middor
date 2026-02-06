package org.nktnet.middor.managers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import org.nktnet.middor.R

class ScreenCaptureManager(
    private val activity: ComponentActivity,
    private val updateIsRequesting: (isRequesting: Boolean) -> Unit,
    private val onCaptureResult: (resultCode: Int, data: Intent) -> Unit
) {
    private val launcher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                onCaptureResult(result.resultCode, data)
            }
            updateIsRequesting(false)
        }

    fun requestCapture() {
        updateIsRequesting(true)
        try {
            val mpm = activity.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager
            launcher.launch(
                mpm.createScreenCaptureIntent(
                    MediaProjectionConfig.createConfigForUserChoice()
                )
            )
        } catch (e: Exception) {
            updateIsRequesting(false)
            ToastManager.show(
                activity,
                activity.getString(
                    R.string.toast_error,
                    e.message
                )
            )
        }
    }
}
