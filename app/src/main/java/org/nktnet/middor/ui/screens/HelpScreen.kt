package org.nktnet.middor.ui.screens

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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.nktnet.middor.R
import org.nktnet.middor.config.Screen

@Composable
fun HelpScreen(navController: NavController) {
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
                .fillMaxWidth()
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
                "Help",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        HorizontalDivider(
            Modifier.padding(top = 6.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            2.dp
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Instructions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(
                Modifier.padding(bottom = 8.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )
            SelectionContainer {

                Text(
                    text = """
                1. Open settings and grant the overlay permission (display over other apps).

                2. Grant the notification permission (optional). ${stringResource(R.string.app_name)} will be re-opened when the notification is tapped.

                3. On the starting page, tap on the "${stringResource(R.string.button_start_mirror_display_overlay)}" button.

                4. Ensure that "Single App" is used, then click start. Do not cast the "Entire Screen", as this will also capture the overlay.

                5. Select the application you want to mirror. This will launch the application, then create an overlay on top that mirrors the application screen.

                6. To exit, click the red cross icon at the top-right corner of the overlay.
            """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(36.dp))
            Text(
                "Notes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(
                Modifier.padding(bottom = 8.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )
            SelectionContainer {
                Text(
                    text = """
                1. You won't be able to interact with the application while the overlay is active. Thus, it is recommended that you set up the application beforehand (e.g. starting the navigation in Google Maps) prior to creating the mirror overlay.

                2. There will be padding between the application and the overlay. This amount of padding is outside the control of ${stringResource(R.string.button_start_mirror_display_overlay)}.
            """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
