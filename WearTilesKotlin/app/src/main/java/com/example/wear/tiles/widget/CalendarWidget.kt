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
fun CalendarWidget() {
    WidgetContainer {
        RemoteColumn(
            horizontalAlignment = RemoteAlignment.CenterHorizontally
        ) {
            RemoteText(
                text = "Advanced Tennis Coaching with Christina Lloyd".rs,
                style = RemoteMaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = RemoteModifier.fillMaxWidth()
            )
            RemoteText(
                text = "25 July, 6:30-7:30 PM".rs,
                style = RemoteMaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = RemoteModifier.fillMaxWidth()
            )
            RemoteText(
                text = "216 Market Street".rs,
                style = RemoteMaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = RemoteModifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 100)
@Composable
fun CalendarWidgetPreview() = RemotePreview(profile = RcPlatformProfiles.ANDROIDX) { CalendarWidget() }
