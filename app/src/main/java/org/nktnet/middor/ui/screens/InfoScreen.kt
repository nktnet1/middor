package org.nktnet.middor.ui.screens

import android.os.Build
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.nktnet.middor.R
import org.nktnet.middor.config.Screen

@Composable
fun InfoScreen(navController: NavController) {
    val context = LocalContext.current
    val pm = context.packageManager

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
                stringResource(R.string.info_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        HorizontalDivider(
            Modifier.padding(top = 6.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            2.dp
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            InfoRow(label = stringResource(R.string.info_app_name)) {
                context.applicationInfo.loadLabel(pm).toString()
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            InfoRow(label = stringResource(R.string.info_package_name)) {
                context.packageName
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            InfoRow(label = stringResource(R.string.info_app_version)) {
                with(
                    pm.getPackageInfo(context.packageName, 0)
                ) {
                    "${versionName ?: ""} ($longVersionCode)"
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            InfoRow(label = stringResource(R.string.info_source_url)) {
                "https://github.com/nktnet1/middor"
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            InfoRow(label = stringResource(R.string.info_android_version)) {
                "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, valueProvider: () -> String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        SelectionContainer {
            Text(
                text = valueProvider(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
