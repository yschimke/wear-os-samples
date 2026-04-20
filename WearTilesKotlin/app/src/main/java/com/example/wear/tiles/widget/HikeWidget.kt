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
fun HikeWidget() {
    WidgetContainer {
        RemoteColumn(horizontalAlignment = RemoteAlignment.CenterHorizontally) {
            RemoteText(
                text = "Hike".rs,
                style = RemoteMaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = RemoteModifier.fillMaxWidth(),
                color = RemoteColor(Color(0xFFE3E1E6))
            )
            RemoteText(
                text = "10".rs,
                style = RemoteMaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = RemoteModifier.fillMaxWidth(),
                color = RemoteColor(Color(0xFFE3E1E6))
            )
            RemoteText(
                text = "Miles".rs,
                style = RemoteMaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = RemoteModifier.fillMaxWidth(),
                color = RemoteColor(Color(0xFFC7C5D0))
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 100)
@Composable
fun HikeWidgetPreview() = RemotePreview(profile = RcPlatformProfiles.ANDROIDX) { HikeWidget() }
