package uk.nktnet.middor.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import uk.nktnet.middor.R
import uk.nktnet.middor.managers.ToastManager
import uk.nktnet.middor.ui.ThemeDropdownIcon

@Composable
fun LandingScreen(
    onStartClick: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    var overlayGranted by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    val lifecycle = activity?.lifecycle
    DisposableEffect(lifecycle) {
        if (lifecycle == null) {
            onDispose {}
        } else {
            val observer = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    overlayGranted = Settings.canDrawOverlays(activity)
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeContent),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ThemeDropdownIcon()
            }

            Button(
                onClick = {
                    if (!overlayGranted) {
                        ToastManager.show(
                            context,
                            "Please grant ${context.getString(R.string.app_name)} overlay permission."
                        )
                    }
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                    activity?.startActivity(intent)
                },
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (overlayGranted) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    if (overlayGranted) {
                        "Disable Overlay Permission"
                    } else {
                        "Grant Overlay Permission"
                    },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onStartClick() },
                enabled = overlayGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    "Start Screen Mirroring",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
