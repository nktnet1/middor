package org.nktnet.middor.services

import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import kotlin.math.abs

/** Records a whole gesture before replaying it to the app beneath the mirror. */
internal class MirrorTouchView(
    context: Context,
    private val texture: TextureView,
    private val onGesture: (GestureDescription) -> Unit
) : View(context) {

    private data class Stroke(
        val path: Path,
        val start: Long,
        var end: Long,
        var x: Float,
        var y: Float,
        var samples: Int = 1
    )

    private val strokes = linkedMapOf<Int, Stroke>()
    private var downTime = 0L
    private var mapper: MirrorCoordinateMapper? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                reset()
                downTime = event.eventTime
                mapper = MirrorCoordinateMapper(texture)
                if (mapper?.isReady == true) addPointer(event, event.actionIndex)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                samplePointers(event)
                addPointer(event, event.actionIndex)
            }
            MotionEvent.ACTION_MOVE -> samplePointers(event)
            MotionEvent.ACTION_POINTER_UP -> {
                samplePointers(event, force = true)
                finishPointer(event, event.actionIndex)
            }
            MotionEvent.ACTION_UP -> {
                samplePointers(event, force = true)
                finishPointer(event, event.actionIndex)
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
        if (mapper?.isReady != true ||
            strokes.size >= GestureDescription.getMaxStrokeCount()
        ) return
        val point = point(event, index)
        strokes[event.getPointerId(index)] = Stroke(
            Path().apply { moveTo(point.x, point.y) },
            (event.eventTime - downTime).coerceAtLeast(0),
            event.eventTime,
            point.x,
            point.y
        )
    }

    private fun samplePointers(event: MotionEvent, force: Boolean = false) {
        for (index in 0 until event.pointerCount) {
            val stroke = strokes[event.getPointerId(index)] ?: continue
            val point = point(event, index)
            val distance = abs(stroke.x - point.x) + abs(stroke.y - point.y)
            if ((force || distance >= 2f) &&
                stroke.samples < MAX_PATH_SAMPLES
            ) {
                stroke.path.lineTo(point.x, point.y)
                stroke.x = point.x
                stroke.y = point.y
                stroke.samples++
            }
            stroke.end = event.eventTime
        }
    }

    private fun finishPointer(event: MotionEvent, index: Int) {
        strokes[event.getPointerId(index)]?.end = event.eventTime
    }

    private fun replay() {
        if (strokes.isEmpty()) return
        val builder = GestureDescription.Builder()
        var addedStrokes = 0
        for (stroke in strokes.values) {
            val elapsed = (stroke.end - downTime).coerceAtLeast(stroke.start + 1)
            val maxDuration = GestureDescription.getMaxGestureDuration().toLong()
            if (stroke.start >= maxDuration) continue
            val duration = (elapsed - stroke.start).coerceAtLeast(MIN_TAP_DURATION_MS)
                .coerceAtMost(maxDuration - stroke.start)
            builder.addStroke(
                GestureDescription.StrokeDescription(stroke.path, stroke.start, duration)
            )
            addedStrokes++
        }
        if (addedStrokes > 0) onGesture(builder.build())
    }

    private fun point(event: MotionEvent, index: Int): PointF =
        mapper?.map(event.getRawX(index), event.getRawY(index)) ?: PointF(0f, 0f)

    private fun reset() {
        strokes.clear()
        mapper = null
    }

    companion object {
        private const val TAG = "MirrorTouchView"
        private const val MAX_PATH_SAMPLES = 4096
        private const val MIN_TAP_DURATION_MS = 40L
    }
}

/** Invert both the view flip/rotation and the TextureView's crop/zoom transform. */
internal class MirrorCoordinateMapper(texture: TextureView) {
    private val location = IntArray(2).also { texture.getLocationOnScreen(it) }
    private val inverseView = Matrix().also { texture.matrix.invert(it) }
    private val inverseCrop = Matrix().also {
        texture.getTransform(Matrix()).invert(it)
    }
    private val width = texture.width
    private val height = texture.height
    private val displaySize = Point().also { texture.display?.getRealSize(it) }
    private val left = location[0].toFloat().coerceAtLeast(0f)
    private val top = location[1].toFloat().coerceAtLeast(0f)
    private val right = (location[0] + width - 1).toFloat()
        .coerceAtMost((displaySize.x - 1).toFloat()).coerceAtLeast(left)
    private val bottom = (location[1] + height - 1).toFloat()
        .coerceAtMost((displaySize.y - 1).toFloat()).coerceAtLeast(top)

    val isReady = width > 0 && height > 0 && displaySize.x > 0 && displaySize.y > 0

    fun map(rawX: Float, rawY: Float): PointF {
        val point = floatArrayOf(rawX - location[0], rawY - location[1])
        inverseView.mapPoints(point)
        inverseCrop.mapPoints(point)
        return PointF(
            (point[0] + location[0]).coerceIn(left, right),
            (point[1] + location[1]).coerceIn(top, bottom)
        )
    }
}
