package org.nktnet.middor.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.TouchInteractionController
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.MotionEvent
import android.view.TextureView
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import java.lang.ref.WeakReference
import org.nktnet.middor.R
import org.nktnet.middor.managers.ToastManager

/** Only replays explicit user gestures while the user has enabled touch controls and mirroring. */
// The policy inspection is advisory. No node access, autonomous actions, or settings changes.
@SuppressLint("AccessibilityPolicy")
class MirrorInputService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private var mirror: MirrorOverlayView? = null
    private var touchController: TouchInteractionController? = null
    private var session: MirrorTouchSession? = null
    private var previousFlags = 0
    private var previousEventTypes = 0
    private val accessibilityManager by lazy {
        getSystemService(AccessibilityManager::class.java)
    }
    private val explorationListener = AccessibilityManager.TouchExplorationStateChangeListener {
        if (!it && mirror != null) stopForInputFailure()
    }
    private val touchCallback = object : TouchInteractionController.Callback {
        override fun onMotionEvent(event: MotionEvent) {
            val active = session
            if (active != null) {
                active.onMotionEvent(event)
            } else if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                // The screen-sharing picker and everything outside mirroring keep normal input.
                try {
                    touchController?.requestDelegating()
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Unable to delegate touch outside the mirror", e)
                    releaseInput()
                }
            }
        }

        override fun onStateChanged(state: Int) {
            session?.onStateChanged(state)
        }
    }

    companion object {
        private const val TAG = "MirrorInputService"

        @Volatile
        private var connection = WeakReference<MirrorInputService>(null)

        val connected: MirrorInputService?
            get() = connection.get()

        fun isEnabled(context: Context): Boolean {
            val manager = context.getSystemService(AccessibilityManager::class.java)
            return manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            ).any {
                it.resolveInfo.serviceInfo.packageName == context.packageName &&
                    it.resolveInfo.serviceInfo.name == MirrorInputService::class.java.name
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        accessibilityManager.addTouchExplorationStateChangeListener(explorationListener)
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
        accessibilityManager.removeTouchExplorationStateChangeListener(explorationListener)
        disconnect()
        super.onDestroy()
    }

    /** Enable interception before opening the screen-sharing picker, never between taps. */
    fun prepareInput(): Boolean {
        if (touchController != null) return true
        val info = serviceInfo ?: return false
        if (info.capabilities and
            AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION == 0
        ) return false
        return try {
            previousFlags = info.flags
            previousEventTypes = info.eventTypes
            val controller = getTouchInteractionController(Display.DEFAULT_DISPLAY)
            touchController = controller
            controller.registerCallback(null, touchCallback)
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            // Android ignores touch exploration requests from a service with no event types.
            info.eventTypes = info.eventTypes or AccessibilityEvent.TYPE_TOUCH_INTERACTION_START or
                AccessibilityEvent.TYPE_TOUCH_INTERACTION_END
            serviceInfo = info
            true
        } catch (e: RuntimeException) {
            Log.e(TAG, "Unable to prepare mirrored touch interception", e)
            releaseInput()
            false
        }
    }

    fun releaseInput() {
        if (mirror != null) {
            removeMirror()
            stopService(Intent(this, MirrorService::class.java))
        }
        val controller = touchController ?: return
        session?.close()
        session = null
        try {
            serviceInfo?.let {
                val flag = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
                it.flags = (it.flags and flag.inv()) or (previousFlags and flag)
                it.eventTypes = previousEventTypes
                serviceInfo = it
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "Unable to release mirrored touch interception", e)
        }
        try {
            controller.unregisterCallback(touchCallback)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Unable to unregister mirrored touch callbacks", e)
        }
        touchController = null
    }

    /** Physical input is intercepted upstream; the video window stays untouchable. */
    fun showMirror(texture: TextureView, onClose: () -> Unit): Boolean {
        removeMirror()
        if (!prepareInput()) return false
        if (!accessibilityManager.isTouchExplorationEnabled) {
            Log.e(TAG, "Touch interception is not active; refusing an untouchable mirror")
            releaseInput()
            return false
        }
        return try {
            val overlay = MirrorOverlayView(this).apply { setup(texture, onClose) }
            val controller = touchController ?: return false
            lateinit var active: MirrorTouchSession
            val recorder = MirrorTouchController(texture, overlay, onClose) { active.enqueue(it) }
            active = MirrorTouchSession(
                recorder,
                overlay::containsInput,
                controller::requestDelegating,
                { gesture, callback -> dispatchGesture(gesture, callback, handler) },
                ::stopForInputFailure
            )
            session = active
            mirror = overlay
            windowManager.addView(
                overlay,
                MirrorOverlayView.layoutParams(
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, touchable = false
                )
            )
            true
        } catch (e: RuntimeException) {
            Log.e(TAG, "Unable to attach accessibility overlay", e)
            hideMirror()
            false
        }
    }

    fun hideMirror() {
        removeMirror()
        releaseInput()
    }

    private fun removeMirror() {
        session?.close()
        session = null
        mirror?.let {
            if (it.parent != null) windowManager.removeViewImmediate(it)
            it.removeAllViews()
        }
        mirror = null
    }

    private fun disconnect() {
        val wasMirroring = mirror != null
        hideMirror()
        if (connected === this) connection.clear()
        if (wasMirroring) stopService(Intent(this, MirrorService::class.java))
    }

    private fun stopForInputFailure() {
        ToastManager.show(this, getString(R.string.settings_touch_routing_error))
        hideMirror()
        stopService(Intent(this, MirrorService::class.java))
    }
}
