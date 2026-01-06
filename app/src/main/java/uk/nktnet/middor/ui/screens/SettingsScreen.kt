package uk.nktnet.middor.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import uk.nktnet.middor.managers.ToastManager
import uk.nktnet.middor.ui.ThemeDropdownIcon

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    var overlayGranted by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    var notificationsGranted by remember {
        mutableStateOf(checkNotificationPermission(context))
    }

    val lifecycle = activity?.lifecycle
    DisposableEffect(lifecycle) {
        if (lifecycle == null) {
            onDispose {}
        } else {
            val observer = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    overlayGranted = Settings.canDrawOverlays(activity)
                    notificationsGranted = checkNotificationPermission(context)
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
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            ThemeDropdownIcon()

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (!overlayGranted) {
                        ToastManager.show(
                            context,
                            "Please grant overlay permission."
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
                        MaterialTheme.colorScheme.primary
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
                        "Overlay Permission Granted"
                    } else {
                        "Grant Overlay Permission"
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    activity?.startActivity(intent)
                },
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (notificationsGranted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    if (notificationsGranted) {
                        "Notifications Enabled"
                    } else {
                        "Enable Notifications"
                    }
                )
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.areNotificationsEnabled()
    } else {
        true
    }
}
