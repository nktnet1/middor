package uk.nktnet.middor

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uk.nktnet.middor.config.Screen
import uk.nktnet.middor.config.ThemeOption
import uk.nktnet.middor.config.UserSettings
import uk.nktnet.middor.managers.ScreenCaptureManager
import uk.nktnet.middor.managers.ToastManager
import uk.nktnet.middor.ui.screens.HelpScreen
import uk.nktnet.middor.ui.screens.LandingScreen
import uk.nktnet.middor.ui.screens.SettingsScreen
import uk.nktnet.middor.ui.theme.MiddorTheme

class MainActivity : ComponentActivity() {
    private lateinit var screenCaptureManager: ScreenCaptureManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        UserSettings.loadTheme(this)

        screenCaptureManager = ScreenCaptureManager(this) { resultCode, data ->
            val serviceIntent = Intent(this, MirrorService::class.java)
                .apply {
                    putExtra("code", resultCode)
                    putExtra("data", data)
                }
            startForegroundService(serviceIntent)
        }

        setContent {
            val context = LocalContext.current
            val themeOption by UserSettings.currentTheme
            val isDarkTheme = resolveTheme(themeOption)
            val navController = rememberNavController()

            val insetsController = remember(window) {
                window?.let { WindowInsetsControllerCompat(it, it.decorView) }
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
                    NavHost(navController = navController, startDestination = Screen.Landing.route) {
                        composable(Screen.Landing.route) {
                            LandingScreen(
                                navController = navController,
                                onStartClick = {
                                    if (!Settings.canDrawOverlays(context)) {
                                        ToastManager.show(
                                            context,
                                            "Error: please grant ${context.getString(R.string.app_name)} overlay permissions in settings.",
                                        )
                                        navController.navigate(Screen.Settings.route)
                                    } else {
                                        screenCaptureManager.requestCapture()
                                    }
                                },
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(navController)
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
}
