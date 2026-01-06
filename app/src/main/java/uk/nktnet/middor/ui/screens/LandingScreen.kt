package uk.nktnet.middor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import uk.nktnet.middor.R
import uk.nktnet.middor.config.Screen
import uk.nktnet.middor.ui.ThemeDropdownIcon

@Composable
fun LandingScreen(
    navController: NavController,
    onStartClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(4.dp)
            .fillMaxSize()
    ) {
        ThemeDropdownIcon()

        IconButton(
            onClick = { navController.navigate(Screen.Settings.route) },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.settings_24px),
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onStartClick() },
                modifier = Modifier
                    .height(60.dp)
            ) {
                Text("Start Screen Mirroring")
            }
        }
    }
}
