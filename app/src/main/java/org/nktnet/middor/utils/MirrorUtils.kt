package org.nktnet.middor.utils

import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.view.Surface
import android.view.TextureView
import org.nktnet.middor.config.UserSettings

object MirrorUtils {

    fun obtainProjection(
        context: Context,
        resultCode: Int,
        data: Intent,
        cropTop: Int,
        cropBottom: Int,
        onStop: () -> Unit,
        onResize: (width: Int, height: Int) -> Unit
    ): MediaProjection? {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mpm.getMediaProjection(resultCode, data)?.also { mp ->
            mp.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    onStop()
                }

                override fun onCapturedContentResize(width: Int, height: Int) {
                    onResize(width, height - cropTop - cropBottom)
                }
            }, null)
        }
    }

    fun createVirtualDisplay(
        projection: MediaProjection,
        textureView: TextureView,
        displayName: String,
        densityDpi: Int,
        cropTop: Int,
        cropBottom: Int
    ): VirtualDisplay? {
        val w = textureView.width
        val h = textureView.height - cropTop - cropBottom
        return projection.createVirtualDisplay(
            displayName,
            w,
            h,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            Surface(textureView.surfaceTexture),
            null,
            null
        )
    }

    fun setupTextureView(
        context: Context,
        cropTop: Int,
        cropBottom: Int,
        onAvailable: (TextureView) -> Unit
    ): TextureView {
        return TextureView(context).apply {
            scaleX = if (UserSettings.flipHorizontally.value) -1f else 1f
            rotation = if (UserSettings.rotate180.value) 180f else 0f
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    st: android.graphics.SurfaceTexture, w: Int, h: Int
                ) {
                    onAvailable(this@apply)

                    val cropH = h - cropTop - cropBottom
                    println("[DEBUG] cropTop=$cropTop | cropBottom=$cropBottom")
                    val scale = maxOf(w.toFloat() / w.toFloat(), h.toFloat() / cropH.toFloat())
                    val matrix = Matrix()
                    matrix.setScale(scale, scale, w / 2f, h / 2f)
                    setTransform(matrix)
                }
                override fun onSurfaceTextureSizeChanged(
                    st: android.graphics.SurfaceTexture, w: Int, h: Int
                ) {}
                override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture) = true
                override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
            }
        }
    }

    fun releaseProjection(projection: MediaProjection?, virtualDisplay: VirtualDisplay?) {
        virtualDisplay?.release()
        projection?.stop()
    }
}
