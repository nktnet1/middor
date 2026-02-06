package org.nktnet.middor.ui.screens

import android.os.Build
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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.nktnet.middor.R
import org.nktnet.middor.config.Screen
import org.nktnet.middor.config.UserSettings
import org.nktnet.middor.managers.ScreenCaptureManager
import org.nktnet.middor.managers.ToastManager
import org.nktnet.middor.states.SystemState
import org.nktnet.middor.ui.ThemeDropdownIcon
import org.nktnet.middor.ui.components.Android14Qpr2Note
import org.nktnet.middor.ui.components.AppLogo

@Composable
fun LandingScreen(
    navController: NavController,
    screenCaptureManager: ScreenCaptureManager,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var delaySeconds by remember { mutableIntStateOf(-1) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    var countdownRunnable by remember { mutableStateOf<Runnable?>(null) }

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

        countdownRunnable = object : Runnable {
            override fun run() {
                // Off by one because we start with a postDelay
                if (delaySeconds > 1) {
                    handler.postDelayed(this, 1000L)
                    delaySeconds--
                } else {
                    screenCaptureManager.requestCapture()
                    handler.postDelayed(
                        {
                            delaySeconds = -1
                        },
                        500L
                    )
                }
            }
        }
        countdownRunnable?.let {
            if (delaySeconds > 0) {
                handler.postDelayed(it, 1000L)
            } else {
                handler.post(it)
            }
        }
    }

    fun cancelMirrorOverlay() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        delaySeconds = -1
    }

    fun navigateWithCancel(route: String) {
        cancelMirrorOverlay()
        navController.navigate(route)
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
                onClick = { navigateWithCancel(Screen.Info.route) },
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
                onClick = { navigateWithCancel(Screen.Settings.route) },
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
                onClick = { navigateWithCancel(Screen.Help.route) },
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
                enabled = delaySeconds != 0,
                onClick = {
                    if (delaySeconds > 0) {
                        cancelMirrorOverlay()
                    } else {
                        startMirrorOverlay()
                    }
                },
                colors = if (delaySeconds > 0)
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                else
                    ButtonDefaults.buttonColors(),
                modifier = Modifier
                    .height(60.dp)
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(0.9f)
            ) {
                Text(
                    when {
                        delaySeconds > 0 -> {
                            pluralStringResource(
                                id = R.plurals.button_starting_in_seconds,
                                count = delaySeconds,
                                delaySeconds
                            )
                        }
                        delaySeconds == 0 -> {
                            stringResource(
                                R.string.button_starting_now
                            )
                        }
                        else -> {
                            stringResource(
                                R.string.button_start_mirror_display,
                            )
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 8.sp,
                        maxFontSize = 32.sp
                    ),
                )
            }

            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Spacer(Modifier.height(8.dp))
                Android14Qpr2Note()
            }
        }
    }
}
