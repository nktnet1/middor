package org.nktnet.middor.ui.screens

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import org.nktnet.middor.R
import org.nktnet.middor.config.Screen
import org.nktnet.middor.config.UserSettings
import org.nktnet.middor.managers.ToastManager
import org.nktnet.middor.ui.MirrorModeDropdownIcon
import org.nktnet.middor.ui.ThemeDropdownIcon
import org.nktnet.middor.ui.components.settings.BooleanSetting
import org.nktnet.middor.ui.components.settings.IntSetting
import org.nktnet.middor.ui.components.settings.PermissionSetting
import org.nktnet.middor.ui.components.settings.ResetPreferencesDialog

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = context as? ComponentActivity
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    var overlayGranted by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    var notificationsGranted by remember {
        mutableStateOf(
            nm.areNotificationsEnabled()
        )
    }
    var showResetDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsGranted = isGranted
    }

    val lifecycle = activity?.lifecycle

    DisposableEffect(lifecycle) {
        if (lifecycle == null) onDispose {} else {
            val observer = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    overlayGranted = Settings.canDrawOverlays(activity)
                    notificationsGranted = nm.areNotificationsEnabled()
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
                    contentDescription = stringResource(R.string.screen_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                stringResource(R.string.settings_title),
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
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    ).apply {
                        data = Uri.fromParts(
                            "package",
                            context.packageName,
                            null
                        )
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Failed to start activity", e)
                        ToastManager.show(
                            context,
                            resources.getString(
                                R.string.settings_app_info_error,
                                e.message
                            )
                        )
                    }
                }
            ) {
                Text(
                    stringResource(R.string.settings_app_info),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.settings_permissions_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { navController.navigate(Screen.Help.route) },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.help_24px),
                        contentDescription = stringResource(
                            R.string.help_icon_content_description
                        )
                    )
                }
            }
            HorizontalDivider(
                Modifier.padding(bottom = 8.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )

            PermissionSetting(
                stringResource(R.string.settings_overlay_permission_label),
                overlayGranted
            ) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                activity?.startActivity(intent)
            }
            PermissionSetting(
                stringResource(R.string.settings_notifications_permission_label),
                notificationsGranted
            ) {
                activity?.let {
                    val granted = activity.checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    val shouldShowRationale = activity.shouldShowRequestPermissionRationale(
                        Manifest.permission.POST_NOTIFICATIONS
                    )

                    if (granted || shouldShowRationale) {
                        val intent = Intent().apply {
                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            putExtra(
                                Settings.EXTRA_APP_PACKAGE,
                                context.packageName
                            )
                        }
                        activity.startActivity(intent)
                    } else {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.settings_preferences_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showResetDialog = true },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.reset_settings),
                        contentDescription = stringResource(
                            R.string.settings_reset_icon_description
                        )
                    )
                }
            }
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
                Text(
                    stringResource(R.string.settings_mirror_mode_label)
                )
                MirrorModeDropdownIcon()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.settings_theme_label)
                )
                ThemeDropdownIcon()
            }
            BooleanSetting(
                stringResource(R.string.settings_start_on_launch_label),
                UserSettings.startOnLaunch.value
            ) { newValue ->
                UserSettings.setStartOnLaunch(context, newValue)
            }
            BooleanSetting(
                stringResource(R.string.settings_flip_horizontally_label),
                UserSettings.flipHorizontally.value
            ) { newValue ->
                UserSettings.setFlipHorizontally(context, newValue)
            }
            BooleanSetting(
                stringResource(R.string.settings_rotate_180_label),
                UserSettings.rotate180.value
            ) { newValue ->
                UserSettings.setRotate180(context, newValue)
            }
            IntSetting(
                label = stringResource(R.string.settings_start_delay_seconds_label),
                value = UserSettings.startDelaySeconds.value,
                onValueChange = { UserSettings.setStartDelay(context, it) },
                min = 0,
                max = 60,
                steps = 60,
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    ResetPreferencesDialog(
        showDialog = showResetDialog,
        onDismiss = { showResetDialog = false },
        onConfirm = { UserSettings.resetSettings(context) }
    )
}
