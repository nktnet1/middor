package org.nktnet.middor.ui.components.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.nktnet.middor.R

@Composable
fun SettingLabel(label: String, modifier: Modifier = Modifier, info: String? = null) {
    var showInfo by remember { mutableStateOf(false) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f, fill = false))
        if (info != null) {
            IconButton(onClick = { showInfo = true }) {
                Icon(
                    painter = painterResource(R.drawable.info_24px),
                    contentDescription = stringResource(R.string.settings_info_description, label),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    if (showInfo && info != null) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(label) },
            text = { Text(info) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}
