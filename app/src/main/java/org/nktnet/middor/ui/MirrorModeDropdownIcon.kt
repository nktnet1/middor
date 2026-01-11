package org.nktnet.middor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.nktnet.middor.R
import org.nktnet.middor.config.MirrorModeOption
import org.nktnet.middor.config.UserSettings

@Composable
fun MirrorModeDropdownIcon() {
    val context = LocalContext.current
    val currentMode by UserSettings.mirrorMode
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopCenter)
    ) {
        IconButton(
            modifier = Modifier.size(42.dp),
            onClick = { expanded = true }
        ) {
            Icon(
                painter = painterResource(
                    id = when (currentMode) {
                        MirrorModeOption.ACTIVITY -> R.drawable.local_activity_24px
                        MirrorModeOption.OVERLAY -> R.drawable.shadow_24px
                    }
                ),
                contentDescription = stringResource(
                    R.string.settings_mirror_mode_label,
                    stringResource(currentMode.labelResId)
                ),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.wrapContentSize()
        ) {
            MirrorModeOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(option.labelResId),
                            color = if (option == currentMode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        UserSettings.setMirrorMode(context, option)
                        expanded = false
                    }
                )
            }
        }
    }
}
