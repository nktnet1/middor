package org.nktnet.middor.services

import android.app.Activity
import android.graphics.Matrix
import android.view.TextureView
import android.widget.FrameLayout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MirrorCoordinateMapperTest {
    private val controller = Robolectric.buildActivity(Activity::class.java)
    private lateinit var texture: TextureView
    private var originX = 0f
    private var originY = 0f

    @Before
    fun setUp() {
        val activity = controller.setup().get()
        shadowOf(activity.windowManager.defaultDisplay).apply {
            setWidth(1000)
            setRealWidth(1000)
            setHeight(2000)
            setRealHeight(2000)
        }
        val root = FrameLayout(activity)
        texture = TextureView(activity)
        root.addView(texture, FrameLayout.LayoutParams(400, 800))
        activity.setContentView(root)
        root.layout(0, 0, 1000, 2000)
        texture.layout(60, 120, 460, 920)
        val origin = IntArray(2)
        root.getLocationOnScreen(origin)
        originX = origin[0] + 60f
        originY = origin[1] + 120f
    }

    @After
    fun tearDown() {
        controller.pause().stop().destroy()
    }

    @Test
    fun mapsUntransformedImageInsideAnOffsetWindow() {
        assertMaps(80f, 180f, 80f, 180f)
    }

    @Test
    fun horizontalFlipDoesNotClampEveryTapToTheRightEdge() {
        texture.scaleX = -1f
        assertMaps(320f, 180f, 80f, 180f)
        assertMaps(80f, 620f, 320f, 620f)
    }

    @Test
    fun rotationInvertsBothAxes() {
        texture.rotation = 180f
        assertMaps(320f, 620f, 80f, 180f)
    }

    @Test
    fun combinedFlipAndRotationInvertOnlyTheVerticalAxis() {
        texture.scaleX = -1f
        texture.rotation = 180f
        assertMaps(80f, 620f, 80f, 180f)
    }

    @Test
    fun cropAndZoomAreInvertedBeforeReturningToTheSourceScreen() {
        texture.setTransform(Matrix().apply {
            setScale(1.25f, 1.25f)
            postTranslate(-25f, -50f)
        })
        for (flip in listOf(false, true)) {
            for (rotate in listOf(false, true)) {
                texture.scaleX = if (flip) -1f else 1f
                texture.rotation = if (rotate) 180f else 0f
                // Source (100, 200) appears at (100, 200) after the crop and zoom.
                val x = if (flip xor rotate) 300f else 100f
                val y = if (rotate) 600f else 200f
                assertMaps(x, y, 100f, 200f)
                // A second point prevents a transform around one fixed point from passing.
                val secondX = if (flip xor rotate) 125f else 275f
                val secondY = if (rotate) 100f else 700f
                assertMaps(secondX, secondY, 240f, 600f)
            }
        }
    }

    @Test
    fun clampsDragsToTheUntransformedSourceBounds() {
        texture.scaleX = -1f
        assertMaps(-50f, -30f, 399f, 0f)
        assertMaps(450f, 850f, 0f, 799f)
    }

    @Test
    fun rejectsUnlaidOutOrNonInvertibleImages() {
        texture.layout(0, 0, 0, 0)
        assertFalse(MirrorCoordinateMapper(texture).isReady)
        texture.layout(60, 120, 460, 920)
        texture.scaleX = 0f
        assertFalse(MirrorCoordinateMapper(texture).isReady)
    }

    private fun assertMaps(rawX: Float, rawY: Float, sourceX: Float, sourceY: Float) {
        val mapper = MirrorCoordinateMapper(texture)
        assertTrue(mapper.isReady)
        val actual = mapper.map(originX + rawX, originY + rawY)
        assertEquals(originX + sourceX, actual.x, 0.01f)
        assertEquals(originY + sourceY, actual.y, 0.01f)
    }
}
