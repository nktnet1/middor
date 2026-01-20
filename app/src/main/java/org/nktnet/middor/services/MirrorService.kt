package org.nktnet.middor.services

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.IBinder
import android.view.Gravity
import android.view.Surface
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.graphics.toColorInt
import org.nktnet.middor.R
import org.nktnet.middor.config.UserSettings
import org.nktnet.middor.managers.CustomNotificationManager
import org.nktnet.middor.utils.MirrorUtils

class MirrorService : Service() {

    private var projection: MediaProjection? = null
    private var overlayView: FrameLayout? = null
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
                ) ?: return START_NOT_STICKY

                if (UserSettings.removeSystemBars.value) {
                    cropTop = intent.getIntExtra(EXTRA_CROP_TOP, 0)
                    cropBottom = intent.getIntExtra(EXTRA_CROP_BOTTOM, 0)
                    cropLeft = intent.getIntExtra(EXTRA_CROP_LEFT, 0)
                    cropRight = intent.getIntExtra(EXTRA_CROP_RIGHT, 0)
                }

                startFullScreenOverlay(resultCode, data)
                return START_STICKY
            }
            else -> Unit
        }
        return START_NOT_STICKY
    }

    private fun startFullScreenOverlay(resultCode: Int, data: Intent) {
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

        val ov = FrameLayout(this)
        overlayView = ov
        ov.addView(
            textureView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val closeButton = ImageButton(this).apply {
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(CLOSE_BUTTON_COLOUR)
            }
            background = shape
            setImageResource(R.drawable.close_24px)
            setColorFilter(Color.WHITE)
            setOnClickListener { stopSelf() }
        }
        ov.addView(
            closeButton,
            FrameLayout.LayoutParams(120, 120).apply {
                gravity = Gravity.END or Gravity.TOP
                // To keep old behaviour for v0.1.4 and below
                topMargin = if (UserSettings.removeSystemBars.value) 40 else 90
                rightMargin = 40
            }
        )

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.addView(
            ov,
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

                if (!UserSettings.removeSystemBars.value) {
                    // To keep old behaviour for v0.1.4 and below
                    flags = (
                        flags
                            or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    )
                }
            }
        )
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun removeOverlay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        MirrorUtils.releaseProjection(projection, virtualDisplay)
        overlayView?.let { wm.removeView(it) }
        overlayView = null
        virtualDisplay = null
        projection = null
    }
}
