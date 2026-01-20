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
                    contentDescription = stringResource(R.string.screen_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                stringResource(R.string.help_title),
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
                stringResource(R.string.instructions_title),
                style = MaterialTheme.typography.titleLarge,
            )
            HorizontalDivider(
                Modifier.padding(top = 6.dp, bottom = 12.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )
            SelectionContainer {
                Text(
                    text = listOf(
                        stringResource(
                            R.string.help_overlay_step,
                            stringResource(R.string.app_name),
                        ),
                        stringResource(
                            R.string.help_notification_step,
                            stringResource(R.string.app_name)
                        ),
                        stringResource(
                            R.string.help_start_button_step,
                            stringResource(
                                R.string.button_start_mirror_display,
                            )
                        ),
                        stringResource(R.string.help_single_app_step),
                        stringResource(R.string.help_select_app_step),
                        stringResource(R.string.help_exit_overlay_step)
                    ).joinToString("\n\n"),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(36.dp))
            Text(
                stringResource(R.string.notes_title),
                style = MaterialTheme.typography.titleLarge,
            )
            HorizontalDivider(
                Modifier.padding(top = 6.dp, bottom = 12.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )

            SelectionContainer {
                Text(
                    text = listOf(
                        stringResource(R.string.help_overlay_note),
                        stringResource(R.string.help_remove_system_bars)
                    ).joinToString("\n\n"),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
