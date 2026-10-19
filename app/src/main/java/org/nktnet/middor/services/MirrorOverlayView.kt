package org.nktnet.middor.services

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageButton
import org.nktnet.middor.R
import org.nktnet.middor.config.UserSettings

/** Only the video is transformed. Controls and feedback keep their visible screen positions. */
// Reached through the services Android instantiates from the manifest.
@Suppress("unused")
internal class MirrorOverlayView(context: Context) : FrameLayout(context) {
    private val feedback = TouchFeedbackView(context)
    private val close = ImageButton(context).apply {
        contentDescription = context.getString(R.string.mirror_close_description)
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(MirrorService.CLOSE_BUTTON_COLOUR)
        }
        setImageResource(R.drawable.close_24px)
        setColorFilter(Color.WHITE)
    }

    fun setup(texture: TextureView, onClose: () -> Unit) {
        setBackgroundColor(Color.BLACK)
        addView(texture, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(feedback, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        close.setOnClickListener { onClose() }
        val corner = UserSettings.closeButtonPosition.value.gravity
        addView(close, LayoutParams(120, 120, corner).apply {
            setMargins(40, if (UserSettings.removeSystemBars.value) 40 else 90, 40, 40)
        })
        // View-only mode must consume touches instead of sending them to the hidden app.
        setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
            true
        }
    }

    fun containsClose(rawX: Float, rawY: Float): Boolean {
        val location = IntArray(2)
        close.getLocationOnScreen(location)
        return rawX >= location[0] && rawX < location[0] + close.width &&
            rawY >= location[1] && rawY < location[1] + close.height
    }

    fun containsInput(rawX: Float, rawY: Float): Boolean {
        val location = IntArray(2)
        getLocationOnScreen(location)
        val insets = rootWindowInsets?.getInsets(WindowInsets.Type.systemBars())
        val x = rawX - location[0]
        val y = rawY - location[1]
        return x >= (insets?.left ?: 0) && x < width - (insets?.right ?: 0) &&
            y >= (insets?.top ?: 0) && y < height - (insets?.bottom ?: 0)
    }

    fun showTouch(event: MotionEvent) {
        if (UserSettings.touchFeedback.value) feedback.showTouch(event)
    }

    fun clearTouch() = feedback.clearTouch()

    companion object {
        fun layoutParams(type: Int, touchable: Boolean) = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.OPAQUE
        ).apply {
            alpha = 1f
            gravity = Gravity.TOP
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            if (!UserSettings.removeSystemBars.value) {
                flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }
        }
    }
}

// Reached through the overlay owned by the manifest services.
@Suppress("unused")
internal class TouchFeedbackView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }
    private var points = floatArrayOf()

    fun showTouch(event: MotionEvent) {
        val location = IntArray(2)
        getLocationOnScreen(location)
        points = FloatArray(event.pointerCount * 2) { index ->
            if (index % 2 == 0) {
                event.getRawX(index / 2) - location[0]
            } else {
                event.getRawY(index / 2) - location[1]
            }
        }
        invalidate()
    }

    fun clearTouch() {
        points = floatArrayOf()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (index in points.indices step 2) {
            canvas.drawCircle(
                points[index], points[index + 1], 16f * resources.displayMetrics.density, paint
            )
        }
    }
}
