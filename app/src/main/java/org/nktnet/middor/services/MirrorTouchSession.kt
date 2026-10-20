package org.nktnet.middor.services

import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.TouchInteractionController
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.KeyCharacterMap
import android.view.MotionEvent

/** Intercept physical input continuously; delegate only the synthetic replay to Android. */
internal class MirrorTouchSession(
    private val recorder: MirrorTouchController,
    private val containsInput: (Float, Float) -> Boolean,
    private val delegate: () -> Unit,
    private val inject: (GestureDescription, GestureResultCallback) -> Boolean,
    private val onFailure: () -> Unit
) : TouchInteractionController.Callback {
    private class Replay(val gesture: GestureDescription) {
        val starts = mutableMapOf<Int, GestureDescription.StrokeDescription>()
        val initialPoints = mutableListOf<PointF>()
        var injectedDownTime: Long? = null
        var started = false
        var delegated = false
        var delegationRequested = false
        var continuing = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private val queue = ArrayDeque<GestureDescription>()
    private var current: Replay? = null
    private var physicalDown = false
    private var state = TouchInteractionController.STATE_CLEAR
    private var closed = false

    override fun onMotionEvent(event: MotionEvent) {
        if (closed) return
        if (isReplayEvent(event)) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                current?.delegationRequested = true
                delegateSafely()
            }
            return
        }
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            // Android cancels injected input when a physical gesture starts. Never retry it:
            // part of it may already have reached the app. This new gesture is still intercepted.
            current = null
            physicalDown = true
            if (!containsInput(event.rawX, event.rawY)) {
                physicalDown = false
                delegateSafely()
                return
            }
        }
        recorder.onTouch(event)
        if (event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_CANCEL
        ) {
            physicalDown = false
            handler.post { drain() }
        }
    }

    override fun onStateChanged(state: Int) {
        if (closed) return
        this.state = state
        if (state == TouchInteractionController.STATE_DELEGATING) {
            current?.let { if (it.delegationRequested) it.delegated = true }
            continueReplay()
        } else if (state == TouchInteractionController.STATE_CLEAR) {
            physicalDown = false
            recorder.cancel()
            handler.post { drain() }
        }
    }

    fun enqueue(gesture: GestureDescription) {
        if (closed) return
        queue.addLast(gesture)
        handler.post { drain() }
    }

    fun close() {
        closed = true
        queue.clear()
        current = null
        recorder.cancel()
        handler.removeCallbacksAndMessages(null)
    }

    private fun isReplayEvent(event: MotionEvent): Boolean {
        val replay = current ?: return false
        if (event.deviceId != KeyCharacterMap.VIRTUAL_KEYBOARD) return false
        if (event.actionMasked != MotionEvent.ACTION_DOWN) {
            return replay.injectedDownTime == event.downTime
        }
        // Virtual input can also be user input (for example adb/emulator taps). A device ID
        // alone must never bypass mapping or close handling. Match only our pending held DOWN.
        if (replay.injectedDownTime != null || replay.continuing) return false
        val matchesStart = replay.initialPoints.any {
            kotlin.math.abs(it.x - event.rawX) < 1f &&
                kotlin.math.abs(it.y - event.rawY) < 1f
        }
        if (matchesStart) replay.injectedDownTime = event.downTime
        return matchesStart
    }

    private fun drain() {
        if (closed || physicalDown || current != null || queue.isEmpty() ||
            state == TouchInteractionController.STATE_DELEGATING ||
            state == TouchInteractionController.STATE_TOUCH_EXPLORING
        ) return
        val replay = Replay(queue.removeFirst())
        current = replay
        val start = GestureDescription.Builder().setDisplayId(replay.gesture.displayId)
        for (index in 0 until replay.gesture.strokeCount) {
            val stroke = replay.gesture.getStroke(index)
            if (stroke.startTime != 0L) continue
            val point = FloatArray(2)
            if (!PathMeasure(stroke.path, false).getPosTan(0f, point, null)) {
                val bounds = RectF()
                stroke.path.computeBounds(bounds, true)
                point[0] = bounds.left
                point[1] = bounds.top
            }
            // Hold DOWN until delegation is acknowledged. A short complete tap can finish
            // before the request reaches Android, leaving a late DOWN with no matching UP.
            val initial = GestureDescription.StrokeDescription(
                Path().apply {
                    moveTo(point[0], point[1])
                    lineTo(point[0], point[1])
                }, 0, 1, true
            )
            replay.initialPoints.add(PointF(point[0], point[1]))
            replay.starts[index] = initial
            start.addStroke(initial)
        }
        send(replay, start.build(), starting = true)
        // A timeout stops the mirror; it never guesses that Android has delegated the stream.
        handler.postDelayed({
            if (current === replay && !replay.continuing) fail()
        }, HANDOFF_TIMEOUT_MS)
    }

    private fun continueReplay() {
        val replay = current ?: return
        if (!replay.started || !replay.delegated || replay.continuing || physicalDown) return
        replay.continuing = true
        val continuation = GestureDescription.Builder().setDisplayId(replay.gesture.displayId)
        for (index in 0 until replay.gesture.strokeCount) {
            val stroke = replay.gesture.getStroke(index)
            continuation.addStroke(
                replay.starts[index]?.continueStroke(stroke.path, 0, stroke.duration, false)
                    ?: stroke
            )
        }
        send(replay, continuation.build(), starting = false)
    }

    private fun send(replay: Replay, gesture: GestureDescription, starting: Boolean) {
        try {
            val accepted = inject(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    if (closed || current !== replay) return
                    if (starting) {
                        replay.started = true
                        continueReplay()
                    } else {
                        current = null
                        handler.post { drain() }
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    if (closed || current !== replay) return
                    current = null
                    handler.post { drain() }
                }
            })
            if (!accepted) fail()
        } catch (_: RuntimeException) {
            fail()
        }
    }

    private fun delegateSafely() {
        try {
            delegate()
        } catch (_: RuntimeException) {
            fail()
        }
    }

    private fun fail() {
        if (closed) return
        close()
        onFailure()
    }

    companion object {
        private const val HANDOFF_TIMEOUT_MS = 1500L
    }
}
