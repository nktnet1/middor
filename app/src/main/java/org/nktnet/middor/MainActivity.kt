package org.nktnet.middor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.nktnet.middor.config.Screen
import org.nktnet.middor.config.ThemeOption
import org.nktnet.middor.config.UserSettings
import org.nktnet.middor.managers.ScreenCaptureManager
import org.nktnet.middor.managers.ToastManager
import org.nktnet.middor.services.MirrorService
import org.nktnet.middor.ui.screens.HelpScreen
import org.nktnet.middor.ui.screens.InfoScreen
import org.nktnet.middor.ui.screens.LandingScreen
import org.nktnet.middor.ui.screens.SettingsScreen
import org.nktnet.middor.ui.theme.MiddorTheme

class MainActivity : ComponentActivity() {
    private lateinit var screenCaptureManager: ScreenCaptureManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        UserSettings.init(this)

        screenCaptureManager = ScreenCaptureManager(this) { resultCode, data ->
            val serviceIntent = Intent(this, MirrorService::class.java)
                .apply {
                    putExtra("code", resultCode)
                    putExtra("data", data)
                }
            startForegroundService(serviceIntent)
        }

        setContent {
            val themeOption by UserSettings.currentTheme
            val isDarkTheme = resolveTheme(themeOption)
            val navController = rememberNavController()

            val insetsController = remember(window) {
                window?.let { WindowInsetsControllerCompat(it, it.decorView) }
            }

            LaunchedEffect(Unit) {
                if (UserSettings.startOnLaunch.value) {
                    startMirrorOverlay(navController, screenCaptureManager)
                }
            }

            LaunchedEffect(isDarkTheme) {
                insetsController?.isAppearanceLightStatusBars = !isDarkTheme
                insetsController?.isAppearanceLightNavigationBars = !isDarkTheme
            }

            MiddorTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Landing.route
                    ) {
                        composable(Screen.Landing.route) {
                            LandingScreen(
                                navController = navController,
                                onStartClick = {
                                    startMirrorOverlay(navController, screenCaptureManager)
                                },
                            )

                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(navController)
                        }
                        composable(Screen.Info.route) {
                            InfoScreen(navController)
                        }
                        composable(Screen.Help.route) {
                            HelpScreen(navController)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MirrorService::class.java)
        stopService(intent)
    }

    @Composable
    private fun resolveTheme(theme: ThemeOption): Boolean {
        return when (theme) {
            ThemeOption.SYSTEM -> isSystemInDarkTheme()
            ThemeOption.DARK -> true
            ThemeOption.LIGHT -> false
        }
    }

    private fun startMirrorOverlay(
        navController: NavController,
        screenCaptureManager: ScreenCaptureManager
    ) {
        if (!Settings.canDrawOverlays(this)) {
            ToastManager.show(
                this,
                getString(
                    R.string.settings_overlay_permission_error,
                    getString(R.string.app_name)
                )
            )
            navController.navigate(Screen.Settings.route)
        } else {
            val delayMs = UserSettings.startDelayMs.value
            if (delayMs > 0) {
                Handler(mainLooper).postDelayed(
                    {
                        screenCaptureManager.requestCapture()
                    },
                    delayMs
                )
            } else {
                screenCaptureManager.requestCapture()
            }
        }
    }
}
