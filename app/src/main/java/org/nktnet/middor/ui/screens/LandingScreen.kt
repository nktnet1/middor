package org.nktnet.middor.ui.screens

import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.nktnet.middor.R
import org.nktnet.middor.config.Screen
import org.nktnet.middor.config.UserSettings
import org.nktnet.middor.managers.ScreenCaptureManager
import org.nktnet.middor.managers.ToastManager
import org.nktnet.middor.states.SystemState
import org.nktnet.middor.ui.ThemeDropdownIcon
import org.nktnet.middor.ui.components.AppLogo

@Composable
fun LandingScreen(
    navController: NavController,
    screenCaptureManager: ScreenCaptureManager,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var delaySeconds by remember { mutableIntStateOf(0) }
    // Extra boolean variable to disable with buffer
    var enableStartButton by remember { mutableStateOf(true) }

    fun startMirrorOverlay() {
        if (!Settings.canDrawOverlays(context)) {
            ToastManager.show(
                context,
                resources.getString(
                    R.string.settings_overlay_permission_error,
                    resources.getString(R.string.app_name)
                )
            )
            navController.navigate(Screen.Settings.route)
            return
        }

        delaySeconds = UserSettings.startDelaySeconds.value
        enableStartButton = false

        val handler = Handler(Looper.getMainLooper())

        if (delaySeconds > 0) {
            ToastManager.show(
                context,
                resources.getString(
                    R.string.toast_start_delay_message,
                    delaySeconds.toString()
                )
            )

            val countdownRunnable = object : Runnable {
                override fun run() {
                    delaySeconds--
                    if (delaySeconds > 0) {
                        handler.postDelayed(this, 1000L)
                    }
                }
            }
            handler.postDelayed(countdownRunnable, 1000L)
        }

        handler.postDelayed({
            screenCaptureManager.requestCapture()
            handler.postDelayed({
                enableStartButton = true
                delaySeconds = 0
            }, 1000L)
        }, delaySeconds * 1000L)
    }

    LaunchedEffect(Unit) {
        if (UserSettings.startOnLaunch.value && !SystemState.hasStartedOnLaunch) {
            startMirrorOverlay()
        }
        SystemState.hasStartedOnLaunch = true
    }

    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 8.dp, start = 4.dp, end = 4.dp)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            ThemeDropdownIcon()
            IconButton(
                onClick = { navController.navigate(Screen.Info.route) },
                modifier = Modifier.size(42.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.info_24px),
                    contentDescription = stringResource(
                        R.string.landing_info_content_description
                    ),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            IconButton(
                onClick = { navController.navigate(Screen.Settings.route) },
                modifier = Modifier.size(42.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.settings_24px),
                    contentDescription = stringResource(
                        R.string.landing_settings_content_description
                    ),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(
                onClick = { navController.navigate(Screen.Help.route) },
                modifier = Modifier.size(42.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.help_24px),
                    contentDescription = stringResource(
                        R.string.help_icon_content_description
                    ),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            AppLogo()
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { startMirrorOverlay() },
                enabled = enableStartButton,
                modifier = Modifier
                    .height(60.dp)
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(0.9f)
            ) {
                Text(
                    if (delaySeconds > 0) {
                        stringResource(
                            R.string.button_starting_in_seconds,
                            delaySeconds
                        )
                    } else if (!enableStartButton) {
                        stringResource(
                            R.string.button_starting_now,
                            delaySeconds
                        )
                    } else {
                        stringResource(R.string.button_start_mirror_display_overlay)
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
