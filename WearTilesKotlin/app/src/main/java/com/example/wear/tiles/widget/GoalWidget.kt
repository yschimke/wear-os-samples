@file:Suppress("RestrictTo") // Dummy change to force compilation

package com.example.wear.tiles.widget

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteCircularProgressIndicator
import androidx.wear.compose.remote.material3.RemoteIcon

@RemoteComposable
@Composable
fun GoalWidget() {
    WidgetContainer {
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth()
        ) {
            // Text column on the left with weight(1f)
            RemoteColumn(
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                modifier = RemoteModifier.weight(1f)
            ) {
                RemoteText(
                    text = "Steps".rs,
                    style = RemoteMaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = RemoteModifier.fillMaxWidth(),
                    color = RemoteColor(Color(0xFFE3E1E6))
                )
                RemoteText(
                    text = "5,168".rs,
                    style = RemoteMaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = RemoteModifier.fillMaxWidth(),
                    color = RemoteColor(Color(0xFFE3E1E6))
                )
                RemoteText(
                    text = "of 8,000".rs,
                    style = RemoteMaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = RemoteModifier.fillMaxWidth(),
                    color = RemoteColor(Color(0xFFC7C5D0))
                )
            }
            // Progress indicator with star icon on the right (stacked in a box)
            RemoteBox(
                modifier = RemoteModifier.size(40.rdp),
                contentAlignment = RemoteAlignment.Center
            ) {
                RemoteCircularProgressIndicator(
                    progress = (5168f / 8000f).rf,
                    modifier = RemoteModifier.fillMaxSize()
                )
                RemoteIcon(
                    imageVector = Icons.Default.Star, // Use Star icon which is in core!
                    contentDescription = "star".rs,
                    modifier = RemoteModifier.size(24.rdp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 100)
@Composable
fun GoalWidgetPreview() = RemotePreview(profile = RcPlatformProfiles.ANDROIDX) { GoalWidget() }
