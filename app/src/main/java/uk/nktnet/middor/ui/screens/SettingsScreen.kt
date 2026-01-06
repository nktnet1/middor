package uk.nktnet.middor.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import uk.nktnet.middor.R
import uk.nktnet.middor.config.Screen
import uk.nktnet.middor.config.UserSettings
import uk.nktnet.middor.managers.ToastManager
import uk.nktnet.middor.ui.ThemeDropdownIcon

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var notificationsGranted by remember { mutableStateOf(checkNotificationPermission(context)) }

    val lifecycle = activity?.lifecycle
    DisposableEffect(lifecycle) {
        if (lifecycle == null) onDispose {} else {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxWidth(),
        ) {
            IconButton(onClick = {
                if (!navController.popBackStack()) {
                    navController.navigate(Screen.Landing.route)
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back_24px),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp)
            )

        }
        HorizontalDivider(
            Modifier.padding(top = 6.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            2.dp,
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Failed to start activity", e)
                        ToastManager.show(context, "Error: ${e.message}")
                    }
                }
            ) {
                Text(
                    text = "App Info",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "Preferences",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(
                Modifier.padding(bottom = 8.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Theme (${UserSettings.currentTheme.value.label})")
                ThemeDropdownIcon()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Start on Launch")
                Switch(
                    checked = UserSettings.startOnLaunch.value,
                    onCheckedChange = { newValue ->
                        UserSettings.setStartOnLaunch(context, newValue)
                    },
                )
            }

            Spacer(Modifier.height(48.dp))
            Text(
                "Permissions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(
                Modifier.padding(bottom = 8.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Overlay")
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri()
                        )
                        activity?.startActivity(intent)
                    }
                ) {
                    Text(
                        if (overlayGranted) "Granted" else "Request",
                        color = if (overlayGranted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Notifications")
                TextButton(
                    onClick = {
                        val intent = Intent().apply {
                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        activity?.startActivity(intent)
                    }
                ) {
                    Text(
                        if (notificationsGranted) "Granted" else "Request",
                        color = if (notificationsGranted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.areNotificationsEnabled()
}
