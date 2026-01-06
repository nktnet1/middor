package uk.nktnet.middor

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.graphics.toColorInt
import uk.nktnet.middor.managers.CustomNotificationManager

val CLOSE_BUTTON_COLOUR = "#80FF0000".toColorInt()

class MirrorService : Service() {
    private var projection: MediaProjection? = null
    private var overlayView: FrameLayout? = null
    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == CustomNotificationManager.ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        CustomNotificationManager.createNotificationChannel(this)

        startForeground(
            1,
            CustomNotificationManager.buildNotification(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )

        val resultCode = intent.getIntExtra("code", Activity.RESULT_CANCELED)
        val data = intent.getParcelableExtra("data", Intent::class.java)
            ?: return START_NOT_STICKY

        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(resultCode, data)
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                stopSelf()
            }
        }, null)

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startFullScreenOverlay()

        return START_STICKY
    }

    private fun startFullScreenOverlay() {
        val windowMetrics = wm.currentWindowMetrics
        val bounds = windowMetrics.bounds
        val metrics = DisplayMetrics().apply {
            widthPixels = bounds.width()
            heightPixels = bounds.height()
            densityDpi = resources.displayMetrics.densityDpi
        }

        overlayView = FrameLayout(this)
        val textureView = TextureView(this).apply { scaleX = -1f }
        overlayView?.addView(textureView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        val closeButton = ImageButton(this).apply {
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(CLOSE_BUTTON_COLOUR)
            }
            background = shape
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            setOnClickListener { stopSelf() }
        }
        val closeParams = FrameLayout.LayoutParams(120, 120).apply {
            gravity = Gravity.END or Gravity.TOP
            topMargin = 40
            rightMargin = 40
        }
        overlayView?.addView(closeButton, closeParams)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        wm.addView(overlayView, params)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                projection?.createVirtualDisplay(
                    "mirror",
                    metrics.widthPixels,
                    metrics.heightPixels,
                    metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    Surface(st),
                    null,
                    null
                )
            }
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture) = true
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }
    }

    override fun onDestroy() {
        projection?.stop()
        overlayView?.let { wm.removeView(it) }
        overlayView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
