package org.nktnet.middor.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.nktnet.middor.R

@Composable
fun AppLogo() {
    val configuration = LocalConfiguration.current
    val (painterRes, modifier) = if (
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    ) {
        R.drawable.monochrome_long_icon to Modifier.width(200.dp).aspectRatio(1.8f)
    } else {
        R.drawable.monochrome_icon to Modifier.size(160.dp)
    }

    Image(
        painter = painterResource(painterRes),
        contentDescription = stringResource(R.string.landing_app_icon_content_description),
        modifier = modifier,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
    )
}
