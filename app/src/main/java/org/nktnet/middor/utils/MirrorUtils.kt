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
        onStop: () -> Unit,
        onResize: (width: Int, height: Int) -> Unit
    ): MediaProjection? {
        val mpm = context.getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
        return mpm.getMediaProjection(resultCode, data)?.also { mp ->
            mp.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    onStop()
                }

                override fun onCapturedContentResize(width: Int, height: Int) {
                    onResize(width, height)
                }
            }, null)
        }
    }

    fun createVirtualDisplay(
        projection: MediaProjection,
        textureView: TextureView,
        displayName: String,
        densityDpi: Int
    ): VirtualDisplay? {
        return projection.createVirtualDisplay(
            displayName,
            textureView.width,
            textureView.height,
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
        cropLeft: Int,
        cropRight: Int,
        onAvailable: (TextureView) -> Unit
    ): TextureView {
        return TextureView(context).apply {
            val flip = UserSettings.flipHorizontally.value
            val rotate = UserSettings.rotate180.value
            fun updateTransform(w: Int, h: Int) {
                setTransform(imageTransform(
                    w, h, cropTop, cropBottom, cropLeft, cropRight, flip, rotate
                ))
            }
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    st: android.graphics.SurfaceTexture, w: Int, h: Int
                ) {
                    st.setDefaultBufferSize(w, h)
                    updateTransform(w, h)
                    onAvailable(this@apply)
                }
                override fun onSurfaceTextureSizeChanged(
                    st: android.graphics.SurfaceTexture, w: Int, h: Int
                ) {
                    updateTransform(w, h)
                }
                override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture) = true
                override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
            }
        }
    }

    /** The same matrix renders the image and is inverted for touch mapping. */
    internal fun imageTransform(
        width: Int,
        height: Int,
        cropTop: Int,
        cropBottom: Int,
        cropLeft: Int,
        cropRight: Int,
        flip: Boolean,
        rotate: Boolean
    ): Matrix {
        val cropW = (width - cropLeft - cropRight).coerceAtLeast(1)
        val cropH = (height - cropTop - cropBottom).coerceAtLeast(1)
        val scale = maxOf(width.toFloat() / cropW, height.toFloat() / cropH)
        return Matrix().apply {
            setScale(scale, scale)
            postTranslate(
                -cropLeft * scale + (width - cropW * scale) / 2f,
                -cropTop * scale + (height - cropH * scale) / 2f
            )
            if (flip) postScale(-1f, 1f, width / 2f, height / 2f)
            if (rotate) postRotate(180f, width / 2f, height / 2f)
        }
    }

    fun releaseProjection(projection: MediaProjection?, virtualDisplay: VirtualDisplay?) {
        virtualDisplay?.release()
        projection?.stop()
    }
}
