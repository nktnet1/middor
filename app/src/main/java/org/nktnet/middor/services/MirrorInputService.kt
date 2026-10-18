package org.nktnet.middor.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import java.lang.ref.WeakReference

/** Only replays explicit user gestures while the user has enabled touch controls and mirroring. */
// The policy inspection is advisory. No node access, autonomous actions, or settings changes.
@SuppressLint("AccessibilityPolicy")
class MirrorInputService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private var mirror: MirrorOverlayView? = null
    private var input: View? = null
    private var inputParams: WindowManager.LayoutParams? = null
    private var generation = 0

    companion object {
        private const val TAG = "MirrorInputService"

        @Volatile
        private var connection = WeakReference<MirrorInputService>(null)

        val connected: MirrorInputService?
            get() = connection.get()

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
        connection = WeakReference(this)
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

    /** Keep the opaque video and its close control in one permanently untouchable window. */
    fun showMirror(texture: TextureView, onClose: () -> Unit): Boolean {
        hideMirror()
        return try {
            val overlay = MirrorOverlayView(this).apply { setup(texture, onClose) }
            mirror = overlay
            windowManager.addView(
                overlay,
                MirrorOverlayView.layoutParams(
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, touchable = false
                )
            )

            val controller = MirrorTouchController(texture, overlay, onClose, ::replayGesture)
            input = View(this).apply {
                setOnTouchListener { view, event ->
                    controller.onTouch(event)
                    if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
                    true
                }
            }.also {
                val params = MirrorOverlayView.layoutParams(
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, touchable = true
                ).apply { format = PixelFormat.TRANSLUCENT }
                inputParams = params
                windowManager.addView(it, params)
            }
            true
        } catch (e: RuntimeException) {
            Log.e(TAG, "Unable to attach accessibility overlay", e)
            hideMirror()
            false
        }
    }

    /** Stop injecting and remove both windows before releasing the projection surface. */
    fun hideMirror() {
        generation++
        input?.let { removeWindow(it) }
        input = null
        inputParams = null
        mirror?.let {
            removeWindow(it)
            it.removeAllViews()
        }
        mirror = null
    }

    private fun removeWindow(view: View) {
        // addView can fail before attachment. A detached input is also normal during replay.
        if (view.parent != null) windowManager.removeViewImmediate(view)
    }

    private fun disconnect() {
        val wasMirroring = mirror != null
        hideMirror()
        if (connected === this) connection.clear()
        if (wasMirroring) stopService(Intent(this, MirrorService::class.java))
    }

    private fun replayGesture(gesture: GestureDescription) {
        val view = input ?: return
        val request = ++generation

        // Synchronously remove the input channel instead of guessing when flag changes arrive.
        // The close control belongs to the untouchable video, so replay cannot activate it.
        removeWindow(view)
        handler.post {
            if (generation != request || input !== view) return@post
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
        }
    }

    private fun restoreInput(view: View, request: Int) {
        // Wait until the final injected event has finished dispatching before reattaching.
        handler.post {
            if (generation != request || input !== view || view.parent != null) return@post
            val params = inputParams ?: return@post
            try {
                windowManager.addView(view, params)
            } catch (e: RuntimeException) {
                Log.e(TAG, "Unable to restore mirrored touch controls", e)
                disconnect()
            }
        }
    }
}
