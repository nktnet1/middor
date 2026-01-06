package uk.nktnet.middor

import android.content.Intent
import android.os.Bundle
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
import uk.nktnet.middor.config.ThemeOption
import uk.nktnet.middor.config.UserSettings
import uk.nktnet.middor.managers.ScreenCaptureManager
import uk.nktnet.middor.ui.screens.LandingScreen
import uk.nktnet.middor.ui.theme.MiddorTheme

class MainActivity : ComponentActivity() {

    private lateinit var screenCaptureManager: ScreenCaptureManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load saved theme from DataStore
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
            val themeOption by UserSettings.currentTheme
            val isDarkTheme = resolveTheme(themeOption)

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
                    LandingScreen(
                        onStartClick = { screenCaptureManager.requestCapture() }
                    )
                }
            }
        }
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
