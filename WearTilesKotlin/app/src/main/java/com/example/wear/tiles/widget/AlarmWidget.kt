@file:Suppress("RestrictedApiAndroidX")

package com.example.wear.tiles.widget

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.compose.remote.creation.compose.state.rs

@RemoteComposable
@Composable
fun AlarmWidget() {
    WidgetContainer {
        RemoteColumn(horizontalAlignment = RemoteAlignment.CenterHorizontally) {
            RemoteText(
                text = "Mon—Fri".rs,
                style = RemoteMaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            RemoteText(
                text = "PM".rs,
                style = RemoteMaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 100)
@Composable
fun AlarmWidgetPreview() = RemotePreview(profile = RcPlatformProfiles.ANDROIDX) { AlarmWidget() }
