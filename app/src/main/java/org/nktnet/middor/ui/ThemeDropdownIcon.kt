package org.nktnet.middor.ui

import androidx.compose.foundation.isSystemInDarkTheme
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
import org.nktnet.middor.config.ThemeOption
import org.nktnet.middor.config.UserSettings

@Composable
fun ThemeDropdownIcon() {
    val context = LocalContext.current
    val currentTheme by UserSettings.currentTheme
    var expanded by remember { mutableStateOf(false) }
    val isSystemDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopCenter)
    ) {
        IconButton(
            modifier = Modifier.size(42.dp),
            onClick = { expanded = true },
        ) {
            Icon(
                painter = painterResource(
                    id = when (currentTheme) {
                        ThemeOption.DARK -> R.drawable.dark_mode_24px
                        ThemeOption.LIGHT -> R.drawable.light_mode_24px
                        ThemeOption.SYSTEM -> if (isSystemDark) {
                            R.drawable.moon_stars_24px
                        } else {
                            R.drawable.wb_sunny_24px
                        }
                    }
                ),
                contentDescription = stringResource(
                    R.string.settings_theme_label,
                    stringResource(currentTheme.labelResId)
                ),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.wrapContentSize()
        ) {
            ThemeOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(option.labelResId),
                            color = if (option == currentTheme) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        UserSettings.setTheme(context, option)
                        expanded = false
                    }
                )
            }
        }
    }
}
