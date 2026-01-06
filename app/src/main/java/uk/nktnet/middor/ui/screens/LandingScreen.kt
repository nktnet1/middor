package uk.nktnet.middor.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
            .padding(top = 8.dp, start = 4.dp, end = 4.dp)
            .fillMaxSize()
    ) {
        ThemeDropdownIcon()

        Column(
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            IconButton(
                onClick = { navController.navigate(Screen.Settings.route) },
                modifier = Modifier.size(42.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.settings_24px),
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(
                onClick = { navController.navigate(Screen.Help.route) },
                modifier = Modifier.size(42.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.help_24px),
                    contentDescription = "Help",
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
            Image(
                painter = painterResource(R.drawable.monochrome_icon),
                contentDescription = "App Icon",
                modifier = Modifier.size(160.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onStartClick() },
                modifier = Modifier
                    .height(60.dp)
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(0.9f)
            ) {
                Text(
                    stringResource(R.string.button_start_mirror_display_overlay),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
