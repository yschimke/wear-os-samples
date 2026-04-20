@file:Suppress("RestrictedApiAndroidX")

package com.example.wear.tiles.widget

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.compose.remote.creation.compose.state.rs

@RemoteComposable
@Composable
fun WeatherWidget() {
    WidgetContainer {
        RemoteColumn(horizontalAlignment = RemoteAlignment.CenterHorizontally) {
            RemoteText(text = "Weather".rs, style = RemoteMaterialTheme.typography.titleMedium)
            RemoteText(text = "72° Sunny".rs, style = RemoteMaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 100)
@Composable
fun WeatherWidgetPreview() = RemotePreview(profile = RcPlatformProfiles.ANDROIDX) { WeatherWidget() }
