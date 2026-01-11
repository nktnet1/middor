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
import org.nktnet.middor.MirrorActivity
import org.nktnet.middor.R
import org.nktnet.middor.config.MirrorModeOption
import org.nktnet.middor.config.UserSettings
import org.nktnet.middor.managers.CustomNotificationManager
import org.nktnet.middor.utils.MirrorUtils

class MirrorService : Service() {

    private var projection: MediaProjection? = null
    private var overlayView: FrameLayout? = null
    private var virtualDisplay: VirtualDisplay? = null

    companion object {
        val CLOSE_BUTTON_COLOUR = "#80FF0000".toColorInt()
        const val ACTION_STOP_SERVICE = "org.nktnet.middor.action.STOP_SERVICE"
        const val ACTION_START_OVERLAY = "org.nktnet.middor.action.START_OVERLAY"
        const val VIRTUAL_DISPLAY_NAME = "mirror"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_INTENT = "extra_result_intent"
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

                if (UserSettings.mirrorMode.value == MirrorModeOption.ACTIVITY) {
                    startActivityOverlay(resultCode, data)
                } else {
                    startFullScreenOverlay(resultCode, data)
                }
                return START_STICKY
            }
            else -> Unit
        }
        return START_NOT_STICKY
    }

    private fun startFullScreenOverlay(resultCode: Int, data: Intent) {
        val textureView = MirrorUtils.setupTextureView(context = this) { tv ->
            virtualDisplay = projection?.let {
                MirrorUtils.createVirtualDisplay(
                    it,
                    tv,
                    VIRTUAL_DISPLAY_NAME,
                    resources.displayMetrics.densityDpi,
                )
            }
        }

        val densityDpi = resources.displayMetrics.densityDpi
        projection = MirrorUtils.obtainProjection(
            context = this,
            resultCode = resultCode,
            data = data,
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
                topMargin = 40
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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
        )
    }

    private fun startActivityOverlay(resultCode: Int, data: Intent) {
        val activityIntent = Intent(this, MirrorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_RESULT_CODE, resultCode)
            putExtra(EXTRA_RESULT_INTENT, data)
        }
        startActivity(activityIntent)
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
