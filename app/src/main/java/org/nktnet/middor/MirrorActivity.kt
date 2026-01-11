package org.nktnet.middor

import android.content.Intent
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Bundle
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
import org.nktnet.middor.services.MirrorService
import org.nktnet.middor.utils.MirrorUtils

class MirrorActivity : ComponentActivity() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val resultCode = intent.getIntExtra(
            MirrorService.EXTRA_RESULT_CODE, RESULT_CANCELED
        )
        val data = intent.getParcelableExtra(
            MirrorService.EXTRA_RESULT_INTENT, Intent::class.java
        ) ?: run { finish(); return }

        projection = MirrorUtils.obtainProjection(
            context = this,
            resultCode = resultCode,
            data = data,
            onStop = { finish() },
            onResize = { w, h ->
                virtualDisplay?.resize(
                    w,
                    h,
                    resources.displayMetrics.densityDpi
                )
            }
        )

        setContent {
            MirrorScreen(projection) { finish() }
        }
    }

    override fun onDestroy() {
        MirrorUtils.releaseProjection(projection, virtualDisplay)
        projection = null
        virtualDisplay = null
        super.onDestroy()
    }

    @Composable
    private fun MirrorScreen(projection: MediaProjection?, onClose: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize()) {
            val densityDpi = resources.displayMetrics.densityDpi

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MirrorUtils.setupTextureView(context = context) { tv ->
                        virtualDisplay = projection?.let {
                            MirrorUtils.createVirtualDisplay(
                                it,
                                tv,
                                MirrorService.VIRTUAL_DISPLAY_NAME,
                                densityDpi
                            )
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
                onDispose {
                    MirrorUtils.releaseProjection(projection, virtualDisplay)
                    virtualDisplay = null
                }
            }
        }
    }
}
