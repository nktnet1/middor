package org.nktnet.middor

import android.content.Intent
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.nktnet.middor.config.UserSettings
import org.nktnet.middor.services.MirrorService

class MirrorActivity : ComponentActivity() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val resultCode = intent.getIntExtra(
            MirrorService.EXTRA_RESULT_CODE,
            RESULT_CANCELED
        )
        val data = intent.getParcelableExtra(
            MirrorService.EXTRA_RESULT_INTENT,
            Intent::class.java
        ) ?: run {
            finish()
            return
        }

        projection = obtainMediaProjection(resultCode, data)

        setContent {
            MirrorScreen(projection) {
                finish()
            }
        }
    }

    override fun onDestroy() {
        releaseProjection()
        super.onDestroy()
    }

    private fun obtainMediaProjection(resultCode: Int, data: Intent): MediaProjection? {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mpm.getMediaProjection(resultCode, data)?.also { mp ->
            mp.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    finish()
                }

                override fun onCapturedContentResize(width: Int, height: Int) {
                    virtualDisplay?.resize(width, height, resources.displayMetrics.densityDpi)
                }
            }, null)
        }
    }

    private fun releaseProjection() {
        virtualDisplay?.release()
        virtualDisplay = null
        projection?.stop()
        projection = null
    }

    @Composable
    private fun MirrorScreen(projection: MediaProjection?, onClose: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize()) {
            val densityDpi = resources.displayMetrics.densityDpi

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    TextureView(it).apply {
                        scaleX = if (UserSettings.flipHorizontally.value) -1f else 1f
                        rotation = if (UserSettings.rotate180.value) 180f else 0f
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                st: SurfaceTexture,
                                w: Int,
                                h: Int
                            ) {
                                virtualDisplay = projection?.createVirtualDisplay(
                                    MirrorService.VIRTUAL_DISPLAY_NAME,
                                    w,
                                    h,
                                    densityDpi,
                                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                    Surface(st),
                                    null,
                                    null
                                )
                            }

                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                                virtualDisplay?.resize(w, h, densityDpi)
                            }

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                        }
                    }
                }
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(56.dp)
                    .background(
                        color = Color(MirrorService.CLOSE_BUTTON_COLOUR),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    painter = painterResource(R.drawable.close_24px),
                    contentDescription = null,
                    tint = Color.White
                )
            }

            DisposableEffect(Unit) {
                onDispose { releaseProjection() }
            }
        }
    }
}
