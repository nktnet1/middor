package org.nktnet.middor.services

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import android.view.ViewConfiguration
import org.nktnet.middor.config.UserSettings
import kotlin.math.abs

/** Records physical input using the inverse of the image actually displayed under the finger. */
internal class MirrorTouchController(
    private val texture: TextureView,
    private val overlay: MirrorOverlayView,
    private val onClose: () -> Unit,
    private val onGesture: (GestureDescription) -> Unit
) {
    private data class Stroke(
        val path: Path,
        val start: Long,
        var end: Long,
        var x: Float,
        var y: Float,
        var samples: Int = 1
    )

    private val strokes = mutableListOf<Stroke>()
    private val active = mutableMapOf<Int, Stroke>()
    private val touchSlop = ViewConfiguration.get(texture.context).scaledTouchSlop
    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f
    private var closing = false
    private var closeTap = false
    private var moved = false
    private var invalid = false
    private var mapper: MirrorCoordinateMapper? = null

    fun onTouch(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            reset()
            downTime = event.eventTime
            downX = event.rawX
            downY = event.rawY
            closing = overlay.containsClose(downX, downY)
            closeTap = closing
            mapper = MirrorCoordinateMapper(texture)
        }
        val distance = abs(event.rawX - downX) + abs(event.rawY - downY)
        if (distance > touchSlop || event.pointerCount > 1) {
            closeTap = false
            moved = true
        }
        if (closing) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val shouldClose = closeTap && overlay.containsClose(event.rawX, event.rawY)
                reset()
                if (shouldClose) onClose()
            } else if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                reset()
            }
            return true
        }
        if (!UserSettings.touchControls.value) return true
        overlay.showTouch(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> addPointer(event, event.actionIndex)
            MotionEvent.ACTION_POINTER_DOWN -> {
                samplePointers(event)
                addPointer(event, event.actionIndex)
            }
            MotionEvent.ACTION_MOVE -> samplePointers(event)
            MotionEvent.ACTION_POINTER_UP -> {
                samplePointers(event, force = true)
                active.remove(event.getPointerId(event.actionIndex))
            }
            MotionEvent.ACTION_UP -> {
                samplePointers(event, force = true)
                try {
                    replay()
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Invalid mirrored touch gesture", e)
                } finally {
                    reset()
                }
            }
            MotionEvent.ACTION_CANCEL -> reset()
        }
        return true
    }

    private fun addPointer(event: MotionEvent, index: Int) {
        if (mapper?.isReady != true) return
        if (strokes.size >= GestureDescription.getMaxStrokeCount()) {
            invalid = true
            return
        }
        val point = point(event, index)
        val stroke = Stroke(
            Path().apply { moveTo(point.x, point.y) },
            (event.eventTime - downTime).coerceAtLeast(0),
            event.eventTime,
            point.x,
            point.y
        )
        strokes.add(stroke)
        active[event.getPointerId(index)] = stroke
    }

    private fun samplePointers(event: MotionEvent, force: Boolean = false) {
        for (index in 0 until event.pointerCount) {
            val stroke = active[event.getPointerId(index)] ?: continue
            val point = point(event, index)
            val distance = abs(stroke.x - point.x) + abs(stroke.y - point.y)
            if ((force || distance >= 2f) && stroke.samples < MAX_PATH_SAMPLES) {
                stroke.path.lineTo(point.x, point.y)
                stroke.x = point.x
                stroke.y = point.y
                stroke.samples++
            }
            stroke.end = event.eventTime
        }
    }

    private fun replay() {
        if (invalid || strokes.isEmpty()) return
        val builder = GestureDescription.Builder()
        val maxDuration = GestureDescription.getMaxGestureDuration()
        for ((path, start, end) in strokes) {
            if (start >= maxDuration) return
            val elapsed = (end - downTime - start).coerceAtLeast(1)
            val isTap = strokes.size == 1 && !moved &&
                elapsed < ViewConfiguration.getLongPressTimeout()
            val duration = if (isTap) UserSettings.tapDuration.value.toLong() else elapsed
            builder.addStroke(
                GestureDescription.StrokeDescription(
                    path, start, duration.coerceIn(1, maxDuration - start)
                )
            )
        }
        onGesture(builder.build())
    }

    private fun point(event: MotionEvent, index: Int): PointF =
        mapper?.map(event.getRawX(index), event.getRawY(index)) ?: PointF(0f, 0f)

    fun cancel() = reset()

    private fun reset() {
        strokes.clear()
        active.clear()
        mapper = null
        closing = false
        closeTap = false
        moved = false
        invalid = false
        overlay.clearTouch()
    }

    companion object {
        private const val TAG = "MirrorTouchController"
        private const val MAX_PATH_SAMPLES = 4096
    }
}
