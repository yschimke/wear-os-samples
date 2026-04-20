@file:Suppress("RestrictTo")

package com.example.wear.tiles.widget

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.RemoteColor

@RemoteComposable
@Composable
fun SkiWidget() {
    WidgetContainer {
        RemoteColumn(horizontalAlignment = RemoteAlignment.CenterHorizontally) {
            RemoteText(
                text = "Latest run".rs,
                style = RemoteMaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = RemoteModifier.fillMaxWidth(),
                color = RemoteColor(Color(0xFFE3E1E6))
            )
            RemoteText(
                text = "Max Spd: 46.5 mph".rs,
                style = RemoteMaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = RemoteModifier.fillMaxWidth(),
                color = RemoteColor(Color(0xFFE3E1E6))
            )
            RemoteText(
                text = "Distance: 21.8 mile".rs,
                style = RemoteMaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = RemoteModifier.fillMaxWidth(),
                color = RemoteColor(Color(0xFFE3E1E6))
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 100)
@Composable
fun SkiWidgetPreview() = RemotePreview(profile = RcPlatformProfiles.ANDROIDX) { SkiWidget() }
