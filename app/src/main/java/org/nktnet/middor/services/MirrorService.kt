package org.nktnet.middor.services

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.IBinder
import android.view.Surface
import androidx.core.graphics.toColorInt
import org.nktnet.middor.R
import org.nktnet.middor.config.UserSettings
import org.nktnet.middor.managers.CustomNotificationManager
import org.nktnet.middor.managers.ToastManager
import org.nktnet.middor.utils.MirrorUtils

class MirrorService : Service() {

    private var projection: MediaProjection? = null
    private var inputService: MirrorInputService? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var cropTop = 0
    private var cropBottom = 0
    private var cropLeft = 0
    private var cropRight = 0

    companion object {
        val CLOSE_BUTTON_COLOUR = "#80FF0000".toColorInt()
        const val ACTION_STOP_SERVICE = "org.nktnet.middor.action.STOP_SERVICE"
        const val ACTION_START_OVERLAY = "org.nktnet.middor.action.START_OVERLAY"
        const val VIRTUAL_DISPLAY_NAME = "mirror"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_INTENT = "extra_result_intent"
        const val EXTRA_CROP_TOP = "extra_crop_top"
        const val EXTRA_CROP_BOTTOM = "extra_crop_bottom"
        const val EXTRA_CROP_LEFT = "extra_crop_left"
        const val EXTRA_CROP_RIGHT = "extra_crop_right"
    }

    override fun onCreate() {
        super.onCreate()
        CustomNotificationManager.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                removeOverlay()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_OVERLAY -> {
                startForeground(
                    1,
                    CustomNotificationManager.buildNotification(this),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )

                val resultCode = intent.getIntExtra(
                    EXTRA_RESULT_CODE, Activity.RESULT_CANCELED
                )
                val data = intent.getParcelableExtra(
                    EXTRA_RESULT_INTENT, Intent::class.java
                ) ?: run {
                    stopSelf()
                    return START_NOT_STICKY
                }

                if (UserSettings.removeSystemBars.value) {
                    cropTop = intent.getIntExtra(EXTRA_CROP_TOP, 0)
                    cropBottom = intent.getIntExtra(EXTRA_CROP_BOTTOM, 0)
                    cropLeft = intent.getIntExtra(EXTRA_CROP_LEFT, 0)
                    cropRight = intent.getIntExtra(EXTRA_CROP_RIGHT, 0)
                }

                startFullScreenOverlay(resultCode, data)
                return START_NOT_STICKY
            }
            else -> Unit
        }
        return START_NOT_STICKY
    }

    private fun startFullScreenOverlay(resultCode: Int, data: Intent) {
        val accessibility = MirrorInputService.connected
        if (accessibility == null) {
            ToastManager.show(this, getString(R.string.settings_accessibility_not_connected))
            stopSelf()
            return
        }

        val textureView = MirrorUtils.setupTextureView(
            context = this,
            cropTop = cropTop,
            cropBottom = cropBottom,
            cropLeft = cropLeft,
            cropRight = cropRight
        ) { tv ->
            virtualDisplay = projection?.let {
                MirrorUtils.createVirtualDisplay(
                    it,
                    tv,
                    VIRTUAL_DISPLAY_NAME,
                    resources.displayMetrics.densityDpi,
                    cropTop,
                    cropBottom,
                    cropLeft,
                    cropRight
                )
            }
        }

        val densityDpi = resources.displayMetrics.densityDpi
        projection = MirrorUtils.obtainProjection(
            context = this,
            resultCode = resultCode,
            data = data,
            cropTop = cropTop,
            cropBottom = cropBottom,
            cropLeft = cropLeft,
            cropRight = cropRight,
            onStop = { stopSelf() },
            onResize = { w, h ->
                virtualDisplay?.resize(w, h, densityDpi)
                textureView.surfaceTexture?.let { st ->
                    virtualDisplay?.surface = Surface(st)
                }
            }
        )

        if (accessibility.showMirror(textureView) { stopSelf() }) {
            inputService = accessibility
            return
        }
        ToastManager.show(this, getString(R.string.settings_accessibility_overlay_error))
        stopSelf()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun removeOverlay() {
        inputService?.hideMirror()
        inputService = null
        MirrorUtils.releaseProjection(projection, virtualDisplay)
        virtualDisplay = null
        projection = null
    }
}
