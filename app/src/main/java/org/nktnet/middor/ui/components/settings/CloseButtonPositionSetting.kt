package org.nktnet.middor.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.nktnet.middor.R
import org.nktnet.middor.config.CloseButtonPosition
import org.nktnet.middor.config.UserSettings

@Composable
fun CloseButtonPositionSetting() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(R.string.settings_close_position_label))
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(stringResource(UserSettings.closeButtonPosition.value.labelResId))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                CloseButtonPosition.entries.forEach { position ->
                    DropdownMenuItem(
                        text = { Text(stringResource(position.labelResId)) },
                        onClick = {
                            UserSettings.setCloseButtonPosition(context, position)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
