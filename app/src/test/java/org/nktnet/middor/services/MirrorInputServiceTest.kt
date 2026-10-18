package org.nktnet.middor.services

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.Gravity
import android.view.KeyCharacterMap
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.nktnet.middor.config.CloseButtonPosition
import org.nktnet.middor.config.UserSettings
import org.nktnet.middor.utils.MirrorUtils
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWindowManagerImpl

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MirrorInputServiceTest {
    private val controller = Robolectric.buildService(MirrorInputService::class.java)
    private lateinit var service: MirrorInputService
    private lateinit var windows: ShadowWindowManagerImpl
    private lateinit var texture: TextureView
    private lateinit var overlay: MirrorOverlayView
    private lateinit var input: View
    private var closeCount = 0

    @Before
    fun setUp() {
        UserSettings.touchControls.value = true
        UserSettings.touchFeedback.value = true
        UserSettings.tapDuration.value = 40
        UserSettings.closeButtonPosition.value = CloseButtonPosition.TOP_RIGHT
        service = controller.create().get()
        windows = Shadow.extract(service.getSystemService(WindowManager::class.java))
        shadowOf(service).setCanDispatchGestures(true)
        showMirror()
    }

    @After
    fun tearDown() {
        controller.destroy()
        UserSettings.touchControls.value = true
        UserSettings.tapDuration.value = 40
        UserSettings.closeButtonPosition.value = CloseButtonPosition.TOP_RIGHT
    }

    @Test
    fun mirrorUsesAnOpaqueHardwareAcceleratedTrustedWindow() {
        val params = overlay.layoutParams as WindowManager.LayoutParams
        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, params.type)
        assertEquals(PixelFormat.OPAQUE, params.format)
        assertEquals(1f, params.alpha, 0f)
        assertEquals(Color.BLACK, (overlay.background as ColorDrawable).color)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED != 0)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        // No separate touchable close window can intercept a replayed gesture.
        assertEquals(2, windows.views.size)
        assertTrue(windows.views.none { it is ImageButton })
    }

    @Test
    fun topLeftTapReplaysToTheVisibleTargetWithoutClosingTheMirror() {
        texture.setTransform(MirrorUtils.imageTransform(
            320, 470, 0, 0, 0, 0, flip = true, rotate = false
        ))
        tap(80f, 130f)
        assertFalse(input.isAttachedToWindow)
        assertTrue(overlay.isAttachedToWindow)
        idle()
        val dispatch = shadowOf(service).gesturesDispatched.single()
        val bounds = RectF()
        dispatch.description().getStroke(0).path.computeBounds(bounds, true)
        assertEquals(240f, bounds.left, 0.01f)
        assertEquals(130f, bounds.top, 0.01f)
        assertTrue(overlay.containsClose(bounds.left, bounds.top))
        assertEquals(0, closeCount)
        dispatch.callback().onCompleted(dispatch.description())
        idle()
        assertTrue(input.isAttachedToWindow)
        assertEquals(0, closeCount)
    }

    @Test
    fun onlyTheVisibleCloseButtonClosesForEveryCornerAndTransform() {
        for (corner in CloseButtonPosition.entries) {
            for (flip in listOf(false, true)) {
                for (rotate in listOf(false, true)) {
                    UserSettings.closeButtonPosition.value = corner
                    showMirror()
                    overlay.layoutDirection = View.LAYOUT_DIRECTION_RTL
                    layoutOverlay()
                    texture.setTransform(MirrorUtils.imageTransform(
                        320, 470, 0, 0, 0, 0, flip, rotate
                    ))
                    val close = (0 until overlay.childCount).map { overlay.getChildAt(it) }
                        .filterIsInstance<ImageButton>().single()
                    val location = IntArray(2)
                    close.getLocationOnScreen(location)
                    val horizontal = corner.gravity and Gravity.HORIZONTAL_GRAVITY_MASK
                    val vertical = corner.gravity and Gravity.VERTICAL_GRAVITY_MASK
                    assertEquals(if (horizontal == Gravity.RIGHT) 160 else 40, close.left)
                    assertEquals(if (vertical == Gravity.TOP) 90 else 310, close.top)
                    val before = closeCount
                    tap(location[0] + close.width / 2f, location[1] + close.height / 2f)
                    assertEquals(before + 1, closeCount)
                    assertTrue(shadowOf(service).gesturesDispatched.isEmpty())
                }
            }
        }
    }

    @Test
    fun injectedTouchCannotCloseOrBeRecordedAgain() {
        tap(220f, 130f, deviceId = KeyCharacterMap.VIRTUAL_KEYBOARD)
        idle()
        assertEquals(0, closeCount)
        assertTrue(shadowOf(service).gesturesDispatched.isEmpty())
        assertTrue(input.isAttachedToWindow)
    }

    @Test
    fun dragFromCloseDoesNotCloseOrReplay() {
        touch(MotionEvent.ACTION_DOWN, 220f, 130f, 0)
        touch(MotionEvent.ACTION_MOVE, 80f, 180f, 20)
        touch(MotionEvent.ACTION_UP, 220f, 130f, 50)
        idle()
        assertEquals(0, closeCount)
        assertTrue(shadowOf(service).gesturesDispatched.isEmpty())
    }

    @Test
    fun disabledTouchControlsConsumeInputButKeepCloseAvailable() {
        UserSettings.touchControls.value = false
        tap()
        assertTrue(input.isAttachedToWindow)
        assertTrue(shadowOf(service).gesturesDispatched.isEmpty())
        tap(220f, 130f)
        assertEquals(1, closeCount)
    }

    @Test
    fun tapDurationIsConfigurableWhileLongPressKeepsItsDuration() {
        UserSettings.tapDuration.value = 75
        tap()
        idle()
        val tap = shadowOf(service).gesturesDispatched.single()
        assertEquals(75L, tap.description().getStroke(0).duration)
        tap.callback().onCompleted(tap.description())
        idle()
        touch(MotionEvent.ACTION_DOWN, 80f, 180f, 100)
        touch(MotionEvent.ACTION_UP, 80f, 180f, 900)
        idle()
        val longPress = shadowOf(service).gesturesDispatched.last()
        assertEquals(800L, longPress.description().getStroke(0).duration)
    }

    @Test
    fun swipeKeepsItsPathAndTiming() {
        texture.setTransform(MirrorUtils.imageTransform(
            320, 470, 0, 0, 0, 0, flip = true, rotate = false
        ))
        touch(MotionEvent.ACTION_DOWN, 40f, 200f, 0)
        touch(MotionEvent.ACTION_MOVE, 120f, 240f, 100)
        touch(MotionEvent.ACTION_UP, 200f, 280f, 300)
        idle()
        val stroke = shadowOf(service).gesturesDispatched.single().description().getStroke(0)
        val bounds = RectF()
        stroke.path.computeBounds(bounds, true)
        assertEquals(120f, bounds.left, 0.01f)
        assertEquals(280f, bounds.right, 0.01f)
        assertEquals(200f, bounds.top, 0.01f)
        assertEquals(280f, bounds.bottom, 0.01f)
        assertEquals(300L, stroke.duration)
    }

    @Test
    fun reusedPointerIdsPreserveAllMultitouchStrokes() {
        multiTouch(MotionEvent.ACTION_DOWN, 0, listOf(0 to 80f))
        multiTouch(MotionEvent.ACTION_POINTER_DOWN or (1 shl 8), 20,
            listOf(0 to 80f, 1 to 160f))
        multiTouch(MotionEvent.ACTION_POINTER_UP or (1 shl 8), 60,
            listOf(0 to 80f, 1 to 160f))
        multiTouch(MotionEvent.ACTION_POINTER_DOWN or (1 shl 8), 100,
            listOf(0 to 80f, 1 to 240f))
        multiTouch(MotionEvent.ACTION_POINTER_UP or (1 shl 8), 140,
            listOf(0 to 80f, 1 to 240f))
        multiTouch(MotionEvent.ACTION_UP, 200, listOf(0 to 80f))
        idle()
        val gesture = shadowOf(service).gesturesDispatched.single().description()
        assertEquals(3, gesture.strokeCount)
        assertEquals(200L, gesture.getStroke(0).duration)
        assertEquals(20L, gesture.getStroke(1).startTime)
        assertEquals(40L, gesture.getStroke(1).duration)
        assertEquals(100L, gesture.getStroke(2).startTime)
        assertEquals(40L, gesture.getStroke(2).duration)
    }

    @Test
    fun cancelledAndRejectedReplayRestoreInput() {
        tap()
        idle()
        val dispatch = shadowOf(service).gesturesDispatched.single()
        dispatch.callback().onCancelled(dispatch.description())
        idle()
        assertTrue(input.isAttachedToWindow)
        shadowOf(service).setCanDispatchGestures(false)
        tap()
        idle()
        assertTrue(input.isAttachedToWindow)
    }

    @Test
    fun stoppingBeforeDispatchDropsPendingInput() {
        tap()
        service.hideMirror()
        idle()
        assertTrue(windows.views.isEmpty())
        assertTrue(shadowOf(service).gesturesDispatched.isEmpty())
    }

    @Test
    fun lateCompletionAfterStopCannotReattachInput() {
        tap()
        idle()
        val dispatch = shadowOf(service).gesturesDispatched.single()
        service.hideMirror()
        dispatch.callback().onCompleted(dispatch.description())
        idle()
        assertTrue(windows.views.isEmpty())
    }

    @Test
    fun missingAccessibilityStopsWhenTouchControlsAreEnabled() {
        service.hideMirror()
        val mirrorController = Robolectric.buildService(MirrorService::class.java).create()
        val mirrorService = mirrorController.get()
        val intent = Intent(mirrorService, MirrorService::class.java).apply {
            action = MirrorService.ACTION_START_OVERLAY
            putExtra(MirrorService.EXTRA_RESULT_CODE, Activity.RESULT_OK)
            putExtra(MirrorService.EXTRA_RESULT_INTENT, Intent())
        }
        mirrorService.onStartCommand(intent, 0, 1)
        assertTrue(shadowOf(mirrorService).isStoppedBySelf)
        assertTrue(windows.views.isEmpty())
        mirrorController.destroy()
    }

    @Test
    fun viewOnlyModeStaysOpaqueAndBlocksTheHiddenAppWithoutAccessibility() {
        service.hideMirror()
        UserSettings.touchControls.value = false
        val mirrorController = Robolectric.buildService(MirrorService::class.java).create()
        val mirrorService = mirrorController.get()
        val intent = Intent(mirrorService, MirrorService::class.java).apply {
            action = MirrorService.ACTION_START_OVERLAY
            putExtra(MirrorService.EXTRA_RESULT_CODE, Activity.RESULT_OK)
            putExtra(MirrorService.EXTRA_RESULT_INTENT, Intent())
        }
        try {
            mirrorService.onStartCommand(intent, 0, 1)
            val viewOnly = windows.views.filterIsInstance<MirrorOverlayView>().single()
            val params = viewOnly.layoutParams as WindowManager.LayoutParams
            assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, params.type)
            assertEquals(PixelFormat.OPAQUE, params.format)
            assertEquals(1f, params.alpha, 0f)
            assertEquals(0, params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 80f, 180f, 0)
            assertTrue(viewOnly.dispatchTouchEvent(event))
            event.recycle()
            assertFalse(shadowOf(mirrorService).isStoppedBySelf)
            assertTrue(shadowOf(service).gesturesDispatched.isEmpty())
        } finally {
            mirrorController.destroy()
        }
        assertTrue(windows.views.isEmpty())
    }

    private fun showMirror() {
        texture = TextureView(service)
        assertTrue(service.showMirror(texture) { closeCount++ })
        idle()
        overlay = windows.views.filterIsInstance<MirrorOverlayView>().single()
        layoutOverlay()
        input = windows.views.single { it !== overlay }
        assertTrue(MirrorCoordinateMapper(texture).isReady)
    }

    private fun layoutOverlay() {
        overlay.measure(
            View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(470, View.MeasureSpec.EXACTLY)
        )
        overlay.layout(0, 0, 320, 470)
    }

    private fun tap(x: Float = 80f, y: Float = 180f, deviceId: Int = 0) {
        touch(MotionEvent.ACTION_DOWN, x, y, 0, deviceId)
        touch(MotionEvent.ACTION_UP, x, y, 50, deviceId)
    }

    private fun touch(action: Int, x: Float, y: Float, time: Long, deviceId: Int = 0) {
        val event = MotionEvent.obtain(
            0, time, action, x, y, 1f, 1f, 0, 1f, 1f, deviceId, 0
        )
        input.dispatchTouchEvent(event)
        event.recycle()
    }

    private fun multiTouch(action: Int, time: Long, pointers: List<Pair<Int, Float>>) {
        val properties = pointers.map { (id, _) ->
            MotionEvent.PointerProperties().apply {
                this.id = id
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }.toTypedArray()
        val coordinates = pointers.map { (_, x) ->
            MotionEvent.PointerCoords().apply {
                this.x = x
                y = 250f
                pressure = 1f
                size = 1f
            }
        }.toTypedArray()
        val event = MotionEvent.obtain(
            0, time, action, pointers.size, properties, coordinates, 0, 0,
            1f, 1f, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        input.dispatchTouchEvent(event)
        event.recycle()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
}
