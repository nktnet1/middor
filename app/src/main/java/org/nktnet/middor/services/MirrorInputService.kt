package org.nktnet.middor.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import android.widget.ImageButton
import org.nktnet.middor.R
import org.nktnet.middor.config.UserSettings

/** Owns trusted overlay windows and replays touch gestures at the mirrored coordinates. */
class MirrorInputService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private var mirror: FrameLayout? = null
    private var input: MirrorTouchView? = null
    private var inputParams: WindowManager.LayoutParams? = null
    private var closeButton: ImageButton? = null
    private var closeParams: WindowManager.LayoutParams? = null
    private var generation = 0

    companion object {
        private const val TAG = "MirrorInputService"
        private const val INPUT_RELEASE_DELAY_MS = 32L

        @Volatile
        var connected: MirrorInputService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val manager = context.getSystemService(AccessibilityManager::class.java)
            return manager.getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            ).any {
                it.resolveInfo.serviceInfo.packageName == context.packageName &&
                    it.resolveInfo.serviceInfo.name == MirrorInputService::class.java.name
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        disconnect()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    /** Attach the video and controls to trusted accessibility windows, at full opacity. */
    fun showMirror(texture: TextureView, onClose: () -> Unit): Boolean {
        hideMirror()
        return try {
            mirror = FrameLayout(this).apply {
                setBackgroundColor(Color.BLACK)
                addView(
                    texture,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }.also {
                windowManager.addView(
                    it,
                    layoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        PixelFormat.OPAQUE
                    )
                )
            }

            input = MirrorTouchView(this, texture) { gesture -> replayGesture(gesture) }.also {
                val params = layoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                )
                inputParams = params
                windowManager.addView(it, params)
            }

            closeButton = ImageButton(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(MirrorService.CLOSE_BUTTON_COLOUR)
                }
                setImageResource(R.drawable.close_24px)
                setColorFilter(Color.WHITE)
                setOnClickListener { onClose() }
            }.also {
                val params = layoutParams(
                    120,
                    120,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                ).apply {
                    gravity = Gravity.END or Gravity.TOP
                    y = if (UserSettings.removeSystemBars.value) 40 else 90
                    x = 40
                }
                closeParams = params
                windowManager.addView(it, params)
            }
            true
        } catch (e: RuntimeException) {
            Log.e(TAG, "Unable to attach accessibility overlay", e)
            hideMirror()
            false
        }
    }

    /** Stop injecting and remove all windows before releasing the projection surface. */
    fun hideMirror() {
        generation++
        input?.let { removeWindow(it) }
        input = null
        inputParams = null
        closeButton?.let { removeWindow(it) }
        closeButton = null
        closeParams = null
        mirror?.let {
            removeWindow(it)
            it.removeAllViews()
        }
        mirror = null
    }

    private fun removeWindow(view: View) {
        // addView can throw before a view is attached. Cleaning up must be safe in both cases.
        if (view.parent != null) windowManager.removeViewImmediate(view)
    }

    private fun disconnect() {
        val wasMirroring = mirror != null
        hideMirror()
        if (connected === this) connected = null
        if (wasMirroring) {
            stopService(Intent(this, MirrorService::class.java))
        }
    }

    private fun replayGesture(gesture: GestureDescription) {
        val view = input ?: return
        val params = inputParams ?: return
        val request = ++generation

        // The touch catcher must not receive the synthetic gesture we dispatch.
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        windowManager.updateViewLayout(view, params)
        // A mirrored app target can lie beneath our close control, too.
        setCloseTouchable(false)

        // Allow InputDispatcher to observe the new window flags before injecting.
        handler.postDelayed({
            if (generation != request || input !== view) return@postDelayed
            try {
                val accepted = dispatchGesture(
                    gesture,
                    object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription) {
                            restoreInput(view, request)
                        }

                        override fun onCancelled(gestureDescription: GestureDescription) {
                            restoreInput(view, request)
                        }
                    },
                    handler
                )
                if (!accepted) restoreInput(view, request)
            } catch (e: RuntimeException) {
                Log.e(TAG, "Unable to dispatch mirrored gesture", e)
                restoreInput(view, request)
            }
        }, INPUT_RELEASE_DELAY_MS)
    }

    private fun restoreInput(view: View, request: Int) {
        if (generation != request || input !== view) return
        val params = inputParams ?: return
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        windowManager.updateViewLayout(view, params)
        setCloseTouchable(true)
    }

    private fun setCloseTouchable(touchable: Boolean) {
        val button = closeButton ?: return
        val params = closeParams ?: return
        params.flags = if (touchable) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        windowManager.updateViewLayout(button, params)
    }

    private fun layoutParams(
        width: Int,
        height: Int,
        flags: Int,
        format: Int = PixelFormat.TRANSLUCENT
    ) =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            format
        ).apply {
            alpha = 1f
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            if (!UserSettings.removeSystemBars.value) {
                this.flags = this.flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }
        }
}
