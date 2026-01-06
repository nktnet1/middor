package uk.nktnet.middor

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.RequiresApi
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager

class MirrorService : Service() {

    private var projection: MediaProjection? = null
    private var overlayView: FrameLayout? = null
    private var isMinimized = false
    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()

        startForeground(
            1,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )

        val resultCode = intent.getIntExtra("code", Activity.RESULT_CANCELED)
        val data = intent.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY

        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(resultCode, data) ?: return START_NOT_STICKY

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startFullScreenOverlay() {
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)

        overlayView = FrameLayout(this)

        // TextureView for mirrored screen
        val textureView = TextureView(this)
        textureView.scaleX = -1f
        textureView.post {
            textureView.pivotX = textureView.width / 2f
            textureView.pivotY = textureView.height / 2f
        }
        overlayView?.addView(
            textureView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Close icon top-right (red)
        val closeIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.RED)
            setOnClickListener { stopSelf() }
        }
        val iconParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END
        ).apply {
            topMargin = 50
            marginEnd = 50
        }
        overlayView?.addView(closeIcon, iconParams)

        // Back button closes overlay
        overlayView?.isFocusableInTouchMode = true
        overlayView?.requestFocus()
        overlayView?.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                stopSelf()
                true
            } else false
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        wm.addView(overlayView, params)

        // SurfaceTextureListener to start virtual display
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

                // Launch target app under overlay
                val pm = packageManager
                val intent = pm.getLaunchIntentForPackage("uk.nktnet.webviewkiosk")
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent != null) startActivity(intent)
            }

            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture) = true
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }

        // Make overlay draggable to minimize
        overlayView?.setOnTouchListener(OverlayDragListener())
    }

    private inner class OverlayDragListener : View.OnTouchListener {
        private var lastX = 0f
        private var lastY = 0f
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (isMinimized) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.rawX
                        lastY = event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - lastX
                        val dy = event.rawY - lastY
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        wm.updateViewLayout(overlayView, params)
                        lastX = event.rawX
                        lastY = event.rawY
                    }
                }
                return true
            }
            return false
        }
    }

    // Minimize overlay into small bubble
    @RequiresApi(Build.VERSION_CODES.O)
    private fun minimizeOverlay() {
        if (overlayView == null || isMinimized) return
        isMinimized = true
        params.width = 150
        params.height = 150
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 50
        params.y = 50
        wm.updateViewLayout(overlayView, params)

        // Keep only a small icon
        overlayView?.removeAllViews()
        val bubble = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.RED)
            setOnClickListener {
                restoreOverlay()
            }
        }
        overlayView?.addView(bubble)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun restoreOverlay() {
        if (overlayView == null || !isMinimized) return
        isMinimized = false
        wm.removeView(overlayView)
        startFullScreenOverlay()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildNotification(): Notification =
        Notification.Builder(this, "mirror")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Screen mirroring active")
            .build()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "mirror",
            "Screen Mirroring",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        projection?.stop()
        projection = null
        overlayView?.let { wm.removeView(it) }
        overlayView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
