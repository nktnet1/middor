package org.nktnet.middor.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.TouchInteractionController
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
import java.time.Duration
import java.util.concurrent.Executor
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
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAccessibilityService.GestureDispatch
import org.robolectric.shadows.ShadowAccessibilityService
import org.robolectric.shadows.ShadowWindowManagerImpl
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], shadows = [
    MirrorInputServiceTest.TouchControllerShadow::class,
    MirrorInputServiceTest.InputServiceShadow::class
])
class MirrorInputServiceTest {
    private val controller = Robolectric.buildService(MirrorInputService::class.java)
    private lateinit var service: MirrorInputService
    private lateinit var windows: ShadowWindowManagerImpl
    private lateinit var texture: TextureView
    private lateinit var overlay: MirrorOverlayView
    private lateinit var touch: TouchControllerShadow
    private var closeCount = 0

    @Before
    fun setUp() {
        UserSettings.touchControls.value = true
        UserSettings.touchFeedback.value = false
        UserSettings.tapDuration.value = 40
        UserSettings.closeButtonPosition.value = CloseButtonPosition.TOP_RIGHT
        service = controller.create().get()
        service.serviceInfo = AccessibilityServiceInfo().also {
            ReflectionHelpers.setField(
                it, "mCapabilities",
                AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION
            )
        }
        windows = Shadow.extract(service.getSystemService(WindowManager::class.java))
        shadowOf(service).setCanDispatchGestures(true)
        touch = Shadow.extract(service.getTouchInteractionController(0))
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
    fun oneOpaqueTrustedWindowStaysUntouchableThroughoutReplay() {
        val params = overlay.layoutParams as WindowManager.LayoutParams
        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, params.type)
        assertEquals(PixelFormat.OPAQUE, params.format)
        assertEquals(1f, params.alpha, 0f)
        assertEquals(Color.BLACK, (overlay.background as ColorDrawable).color)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED != 0)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertEquals(listOf(overlay), windows.views)
        tap()
        idle()
        assertEquals(listOf(overlay), windows.views)
        val replay = acknowledgeReplay()
        assertEquals(listOf(overlay), windows.views)
        finish(replay)
        assertEquals(listOf(overlay), windows.views)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
    }

    @Test
    fun topLeftTapUsesTheVisibleTransformWithoutActivatingClose() {
        texture.setTransform(MirrorUtils.imageTransform(
            320, 470, 0, 0, 0, 0, flip = true, rotate = false
        ))
        tap(80f, 130f)
        idle()
        val replay = acknowledgeReplay()
        val bounds = RectF()
        replay.description().getStroke(0).path.computeBounds(bounds, true)
        assertEquals(240f, bounds.left, 0.01f)
        assertEquals(130f, bounds.top, 0.01f)
        assertTrue(overlay.containsClose(bounds.left, bounds.top))
        assertEquals(0, closeCount)
        finish(replay)
        assertEquals(0, closeCount)
    }

    @Test
    fun replayCannotEndUntilAndroidAcknowledgesDelegation() {
        tap()
        idle()
        val start = shadowOf(service).gesturesDispatched.single()
        assertTrue(start.description().getStroke(0).willContinue())
        start.callback().onCompleted(start.description())
        idle()
        assertEquals(1, shadowOf(service).gesturesDispatched.size)
        touchEvent(MotionEvent.ACTION_DOWN, 80f, 180f, 100, KeyCharacterMap.VIRTUAL_KEYBOARD)
        idle()
        assertEquals(1, touch.delegations)
        assertEquals(1, shadowOf(service).gesturesDispatched.size)
        touch.state(TouchInteractionController.STATE_DELEGATING)
        assertEquals(2, shadowOf(service).gesturesDispatched.size)
        assertFalse(shadowOf(service).gesturesDispatched.last().description()
            .getStroke(0).willContinue())
    }

    @Test
    fun delegationBeforeBootstrapCompletionAlsoWaitsForBothAcknowledgements() {
        tap()
        idle()
        val start = shadowOf(service).gesturesDispatched.single()
        touchEvent(MotionEvent.ACTION_DOWN, 80f, 180f, 100, KeyCharacterMap.VIRTUAL_KEYBOARD)
        touch.state(TouchInteractionController.STATE_DELEGATING)
        assertEquals(1, shadowOf(service).gesturesDispatched.size)
        start.callback().onCompleted(start.description())
        assertEquals(2, shadowOf(service).gesturesDispatched.size)
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
    fun injectedTouchIsDelegatedWithoutClosingOrBeingRecordedAgain() {
        tap(220f, 130f, deviceId = KeyCharacterMap.VIRTUAL_KEYBOARD)
        idle()
        assertEquals(0, closeCount)
        assertEquals(1, touch.delegations)
        assertTrue(shadowOf(service).gesturesDispatched.isEmpty())
    }

    @Test
    fun physicalTouchesDuringHandoffRemainInterceptedAndInvalidateOldCallbacks() {
        texture.setTransform(MirrorUtils.imageTransform(
            320, 470, 0, 0, 0, 0, flip = true, rotate = false
        ))
        tap(80f, 180f)
        idle()
        val first = shadowOf(service).gesturesDispatched.single()
        touch.state(TouchInteractionController.STATE_CLEAR)
        touchEvent(MotionEvent.ACTION_DOWN, 240f, 250f, 60)
        first.callback().onCompleted(first.description())
        touchEvent(MotionEvent.ACTION_UP, 240f, 250f, 120)
        idle()
        assertEquals(0, touch.delegations)
        assertEquals(2, shadowOf(service).gesturesDispatched.size)
        val second = shadowOf(service).gesturesDispatched.last()
        val bounds = RectF()
        second.description().getStroke(0).path.computeBounds(bounds, true)
        assertEquals(80f, bounds.left, 0.01f)
        assertEquals(250f, bounds.top, 0.01f)
        assertEquals(listOf(overlay), windows.views)
    }

    @Test
    fun rapidTapsAreQueuedAndEachReplayedOnceAtItsMappedPosition() {
        texture.setTransform(MirrorUtils.imageTransform(
            320, 470, 0, 0, 0, 0, flip = true, rotate = false
        ))
        tap(80f, 180f)
        touchEvent(MotionEvent.ACTION_DOWN, 240f, 250f, 60)
        idle()
        assertTrue(shadowOf(service).gesturesDispatched.isEmpty())
        touchEvent(MotionEvent.ACTION_UP, 240f, 250f, 110)
        idle()
        val first = acknowledgeReplay()
        val firstBounds = RectF()
        first.description().getStroke(0).path.computeBounds(firstBounds, true)
        assertEquals(240f, firstBounds.left, 0.01f)
        finish(first)
        val second = acknowledgeReplay()
        val secondBounds = RectF()
        second.description().getStroke(0).path.computeBounds(secondBounds, true)
        assertEquals(80f, secondBounds.left, 0.01f)
        finish(second)
        assertEquals(4, shadowOf(service).gesturesDispatched.size)
        assertEquals(listOf(overlay), windows.views)
    }

    @Test
    fun dragFromCloseDoesNotCloseOrReplay() {
        touchEvent(MotionEvent.ACTION_DOWN, 220f, 130f, 0)
        touchEvent(MotionEvent.ACTION_MOVE, 80f, 180f, 20)
        touchEvent(MotionEvent.ACTION_UP, 220f, 130f, 50)
        idle()
        assertEquals(0, closeCount)
        assertTrue(shadowOf(service).gesturesDispatched.isEmpty())
    }

    @Test
    fun disabledTouchControlsConsumeInputButKeepCloseAvailable() {
        UserSettings.touchControls.value = false
        tap()
        assertTrue(shadowOf(service).gesturesDispatched.isEmpty())
        tap(220f, 130f)
        assertEquals(1, closeCount)
    }

    @Test
    fun tapsLongPressesAndSwipesKeepTheirTimingAndPathsAfterHandoff() {
        UserSettings.tapDuration.value = 75
        tap()
        idle()
        val tap = acknowledgeReplay()
        assertEquals(75L, tap.description().getStroke(0).duration)
        finish(tap)
        touchEvent(MotionEvent.ACTION_DOWN, 80f, 180f, 100)
        touchEvent(MotionEvent.ACTION_UP, 80f, 180f, 900)
        idle()
        val longPress = acknowledgeReplay()
        assertEquals(800L, longPress.description().getStroke(0).duration)
        finish(longPress)
        touchEvent(MotionEvent.ACTION_DOWN, 40f, 200f, 1000)
        touchEvent(MotionEvent.ACTION_MOVE, 120f, 240f, 1100)
        touchEvent(MotionEvent.ACTION_UP, 200f, 280f, 1300)
        idle()
        val swipe = acknowledgeReplay().description().getStroke(0)
        val bounds = RectF()
        swipe.path.computeBounds(bounds, true)
        assertEquals(40f, bounds.left, 0.01f)
        assertEquals(200f, bounds.right, 0.01f)
        assertEquals(200f, bounds.top, 0.01f)
        assertEquals(280f, bounds.bottom, 0.01f)
        assertEquals(300L, swipe.duration)
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
        val gesture = acknowledgeReplay().description()
        assertEquals(3, gesture.strokeCount)
        assertEquals(200L, gesture.getStroke(0).duration)
        assertEquals(20L, gesture.getStroke(1).startTime)
        assertEquals(40L, gesture.getStroke(1).duration)
        assertEquals(100L, gesture.getStroke(2).startTime)
        assertEquals(40L, gesture.getStroke(2).duration)
    }

    @Test
    fun handoffTimeoutStopsInsteadOfLeakingTouchesOrGuessingReadiness() {
        tap()
        idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1500))
        assertTrue(windows.views.isEmpty())
        assertEquals(0, service.serviceInfo.flags and
            AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE)
        assertEquals(1, shadowOf(service).gesturesDispatched.size)
    }

    @Test
    fun rejectedReplayStopsInsteadOfFallingBackToUntransformedInput() {
        shadowOf(service).setCanDispatchGestures(false)
        tap()
        idle()
        assertTrue(windows.views.isEmpty())
        assertEquals(0, service.serviceInfo.flags and
            AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE)
    }

    @Test
    fun stopDropsPendingReplayAndLateCallbacksWithoutReattachingWindows() {
        tap()
        idle()
        val start = shadowOf(service).gesturesDispatched.single()
        service.hideMirror()
        start.callback().onCompleted(start.description())
        idle()
        assertTrue(windows.views.isEmpty())
        assertEquals(1, shadowOf(service).gesturesDispatched.size)
        assertTrue(touch.callback == null)
    }

    @Test
    fun preparationDelegatesPickerTouchesAndReleaseRestoresFlags() {
        service.hideMirror()
        assertTrue(service.prepareInput())
        touchEvent(MotionEvent.ACTION_DOWN, 80f, 180f, 0)
        assertEquals(1, touch.delegations)
        assertTrue(service.serviceInfo.flags and
            AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE != 0)
        service.releaseInput()
        assertEquals(0, service.serviceInfo.flags and
            AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE)
        assertTrue(touch.callback == null)
    }

    @Test
    fun releasingInputCannotLeaveAnOpaqueMirrorWithUntransformedTouches() {
        service.releaseInput()
        assertTrue(windows.views.isEmpty())
        assertTrue(touch.callback == null)
        assertEquals(0, service.serviceInfo.flags and
            AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE)
    }

    @Test
    fun oldServiceCapabilitiesCannotStartAnUntouchableMirror() {
        service.hideMirror()
        service.serviceInfo = AccessibilityServiceInfo()
        assertFalse(service.showMirror(TextureView(service)) {})
        assertTrue(windows.views.isEmpty())
    }

    @Test
    fun missingAccessibilityStopsWhenTouchControlsAreEnabled() {
        service.hideMirror()
        val mirrorController = Robolectric.buildService(MirrorService::class.java).create()
        val mirrorService = mirrorController.get()
        mirrorService.onStartCommand(startIntent(mirrorService), 0, 1)
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
        try {
            mirrorService.onStartCommand(startIntent(mirrorService), 0, 1)
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

    private fun startIntent(context: android.content.Context) = Intent().apply {
        action = MirrorService.ACTION_START_OVERLAY
        putExtra(MirrorService.EXTRA_RESULT_CODE, Activity.RESULT_OK)
        putExtra(MirrorService.EXTRA_RESULT_INTENT, Intent())
        setClass(context, MirrorService::class.java)
    }

    private fun showMirror() {
        texture = TextureView(service)
        assertTrue(service.showMirror(texture) { closeCount++ })
        idle()
        overlay = windows.views.filterIsInstance<MirrorOverlayView>().single()
        layoutOverlay()
        assertTrue(MirrorCoordinateMapper(texture).isReady)
    }

    private fun layoutOverlay() {
        overlay.measure(
            View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(470, View.MeasureSpec.EXACTLY)
        )
        overlay.layout(0, 0, 320, 470)
    }

    private fun acknowledgeReplay(): GestureDispatch {
        val start = shadowOf(service).gesturesDispatched.last()
        val bounds = RectF()
        start.description().getStroke(0).path.computeBounds(bounds, true)
        touchEvent(MotionEvent.ACTION_DOWN, bounds.left, bounds.top, 100,
            KeyCharacterMap.VIRTUAL_KEYBOARD)
        start.callback().onCompleted(start.description())
        touch.state(TouchInteractionController.STATE_DELEGATING)
        return shadowOf(service).gesturesDispatched.last()
    }

    private fun finish(replay: GestureDispatch) {
        replay.callback().onCompleted(replay.description())
        touch.state(TouchInteractionController.STATE_CLEAR)
        idle()
    }

    private fun tap(x: Float = 80f, y: Float = 180f, deviceId: Int = 0) {
        touchEvent(MotionEvent.ACTION_DOWN, x, y, 0, deviceId)
        touchEvent(MotionEvent.ACTION_UP, x, y, 50, deviceId)
    }

    private fun touchEvent(action: Int, x: Float, y: Float, time: Long, deviceId: Int = 0) {
        if (action == MotionEvent.ACTION_DOWN) {
            touch.state(TouchInteractionController.STATE_TOUCH_INTERACTING)
        }
        val event = MotionEvent.obtain(
            0, time, action, x, y, 1f, 1f, 0, 1f, 1f, deviceId, 0
        )
        touch.callback?.onMotionEvent(event)
        event.recycle()
    }

    private fun multiTouch(action: Int, time: Long, pointers: List<Pair<Int, Float>>) {
        if (action == MotionEvent.ACTION_DOWN) {
            touch.state(TouchInteractionController.STATE_TOUCH_INTERACTING)
        }
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
        touch.callback?.onMotionEvent(event)
        event.recycle()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** Test the callback boundary. Android's real input pipeline still requires a device. */
    @Implements(TouchInteractionController::class)
    class TouchControllerShadow {
        var callback: TouchInteractionController.Callback? = null
        var delegations = 0

        @Implementation
        fun registerCallback(executor: Executor?, callback: TouchInteractionController.Callback) {
            this.callback = callback
        }

        @Implementation
        fun unregisterCallback(callback: TouchInteractionController.Callback): Boolean {
            if (this.callback !== callback) return false
            this.callback = null
            return true
        }

        @Implementation
        fun requestDelegating() {
            delegations++
        }

        fun state(state: Int) = callback?.onStateChanged(state)
    }

    @Implements(AccessibilityService::class)
    class InputServiceShadow : ShadowAccessibilityService() {
        private var info = AccessibilityServiceInfo()

        @Implementation
        fun getServiceInfo(): AccessibilityServiceInfo = info

        @Implementation
        fun setServiceInfo(info: AccessibilityServiceInfo) {
            this.info = info
        }
    }
}
