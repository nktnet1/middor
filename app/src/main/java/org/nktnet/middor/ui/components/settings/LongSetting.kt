package org.nktnet.middor.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.nktnet.middor.R

@Composable
fun LongSetting(
    label: String,
    value: Long,
    min: Long,
    max: Long,
    onValueChange: (Long) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val safeMin = min.coerceAtLeast(0L)
    val safeMax = max.coerceAtLeast(safeMin + 1)
    val stepCount = ((safeMax - safeMin).coerceAtMost(100L) - 1)
        .toInt().coerceAtLeast(0)

    var draftValue by remember { mutableLongStateOf(value.coerceIn(safeMin, safeMax)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            TextButton(
                onClick = {
                    draftValue = value.coerceIn(safeMin, safeMax)
                    showDialog = true
                },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    "$value ms",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = draftValue.toString(),
                    onValueChange = {
                        val parsed = it.toLongOrNull()
                        if (parsed != null) draftValue = parsed.coerceIn(safeMin, safeMax)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = draftValue.toFloat(),
                    onValueChange = { draftValue = it.toLong().coerceIn(safeMin, safeMax) },
                    valueRange = safeMin.toFloat()..safeMax.toFloat(),
                    steps = stepCount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    TextButton(
                        onClick = { showDialog = false }
                    ) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                    TextButton(
                        onClick = {
                            onValueChange(draftValue)
                            showDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.dialog_confirm))
                    }
                }
            }
        }
    }
}
