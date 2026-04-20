@file:Suppress("RestrictedApiAndroidX")

package com.example.wear.tiles.widget

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
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
fun MeditationWidget() {
    WidgetContainer {
        RemoteColumn(horizontalAlignment = RemoteAlignment.CenterHorizontally) {
            RemoteText(
                text = "3 mindful tasks left".rs,
                style = RemoteMaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = RemoteModifier.fillMaxWidth()
            )
            RemoteText(
                text = "Breath".rs,
                style = RemoteMaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = RemoteModifier.fillMaxWidth()
            )
            RemoteText(
                text = "Daily mindfulness".rs,
                style = RemoteMaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = RemoteModifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 100)
@Composable
fun MeditationWidgetPreview() = RemotePreview(profile = RcPlatformProfiles.ANDROIDX) { MeditationWidget() }
