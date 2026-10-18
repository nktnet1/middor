package org.nktnet.middor.services

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import java.time.Duration
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
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWindowManagerImpl

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MirrorInputServiceTest {
    private val controller = Robolectric.buildService(MirrorInputService::class.java)
    private lateinit var service: MirrorInputService
    private lateinit var windows: ShadowWindowManagerImpl
    private lateinit var texture: TextureView

    @Before
    fun setUp() {
        service = controller.create().get()
        windows = Shadow.extract(service.getSystemService(WindowManager::class.java))
        shadowOf(service).setCanDispatchGestures(true)
        texture = TextureView(service)
        assertTrue(service.showMirror(texture) {})
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        texture.layout(0, 0, 320, 470)
        assertTrue(texture.isAttachedToWindow)
        assertTrue(MirrorCoordinateMapper(texture).isReady)
    }

    @After
    fun tearDown() {
        controller.destroy()
    }

    @Test
    fun mirrorUsesAnOpaqueHardwareAcceleratedTrustedWindow() {
        val mirror = windows.views.filterIsInstance<FrameLayout>().single()
        val params = mirror.layoutParams as WindowManager.LayoutParams
        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, params.type)
        assertEquals(PixelFormat.OPAQUE, params.format)
        assertEquals(1f, params.alpha, 0f)
        assertEquals(Color.BLACK, (mirror.background as ColorDrawable).color)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED != 0)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
    }

    @Test
    fun replayReleasesBothTouchWindowsAndRestoresThemOnCompletion() {
        texture.scaleX = -1f
        tap()
        assertControlsTouchable(false)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32))
        val dispatch = shadowOf(service).gesturesDispatched.single()
        val bounds = RectF()
        dispatch.description().getStroke(0).path.computeBounds(bounds, true)
        assertEquals(240f, bounds.left, 0.01f)
        assertEquals(180f, bounds.top, 0.01f)
        dispatch.callback().onCompleted(dispatch.description())
        assertControlsTouchable(true)
    }

    @Test
    fun cancelledReplayRestoresControls() {
        tap()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32))
        val dispatch = shadowOf(service).gesturesDispatched.single()
        dispatch.callback().onCancelled(dispatch.description())
        assertControlsTouchable(true)
    }

    @Test
    fun rejectedReplayRestoresControls() {
        shadowOf(service).setCanDispatchGestures(false)
        tap()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32))
        assertControlsTouchable(true)
    }

    @Test
    fun stoppingBeforeDispatchRemovesAllWindowsAndDropsPendingInput() {
        tap()
        service.hideMirror()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32))
        assertTrue(windows.views.isEmpty())
        assertTrue(shadowOf(service).gesturesDispatched.isEmpty())
    }

    @Test
    fun missingAccessibilityStopsInsteadOfCreatingATranslucentFallback() {
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

    private fun tap() {
        val input = windows.views.filterIsInstance<MirrorTouchView>().single()
        val events = listOf(MotionEvent.ACTION_DOWN to 0L, MotionEvent.ACTION_UP to 50L)
        for ((action, time) in events) {
            val event = MotionEvent.obtain(0, time, action, 80f, 180f, 0)
            input.onTouchEvent(event)
            event.recycle()
        }
    }

    private fun assertControlsTouchable(touchable: Boolean) {
        val controls = windows.views.filter { it is MirrorTouchView || it is ImageButton }
        assertEquals(2, controls.size)
        for (view: View in controls) {
            val flags = (view.layoutParams as WindowManager.LayoutParams).flags
            if (touchable) {
                assertFalse(flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
            } else {
                assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
            }
        }
    }
}
