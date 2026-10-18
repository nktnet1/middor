package org.nktnet.middor.services

import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.view.TextureView

/** Snapshot the inverse of the displayed image for the lifetime of a physical gesture. */
internal class MirrorCoordinateMapper(texture: TextureView) {
    private val screenToView = Matrix()
    private val viewToSource = Matrix()
    private val sourceToScreen = Matrix()
    private val width = texture.width
    private val height = texture.height
    private val displaySize = Point().also { texture.display?.getRealSize(it) }

    val isReady: Boolean

    init {
        val viewToScreen = Matrix().also { texture.transformMatrixToGlobal(it) }
        val inverseView = Matrix()
        isReady = width > 0 && height > 0 && displaySize.x > 0 && displaySize.y > 0 &&
            viewToScreen.invert(screenToView) && texture.matrix.invert(inverseView) &&
            texture.getTransform(Matrix()).invert(viewToSource)

        // getLocationOnScreen includes the flip/rotation: its origin can be the bottom-right
        // corner. Remove the view's own matrix to recover the source's unflipped screen frame.
        sourceToScreen.setConcat(viewToScreen, inverseView)
    }

    fun map(rawX: Float, rawY: Float): PointF {
        check(isReady)
        val point = floatArrayOf(rawX, rawY)
        screenToView.mapPoints(point)
        viewToSource.mapPoints(point)
        point[0] = point[0].coerceIn(0f, (width - 1).toFloat())
        point[1] = point[1].coerceIn(0f, (height - 1).toFloat())
        sourceToScreen.mapPoints(point)
        return PointF(
            point[0].coerceIn(0f, (displaySize.x - 1).toFloat()),
            point[1].coerceIn(0f, (displaySize.y - 1).toFloat())
        )
    }
}
