@file:Suppress("RestrictedApiAndroidX")

package com.example.wear.tiles.widget

import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.remote.material3.RemoteButton
import androidx.wear.compose.remote.material3.RemoteButtonColors

// Shared dummy action for previews
public val dummyAction: HostAction = HostAction("dummy".rs, 1.rf)

// Shared container for widgets to match style
@Composable
@RemoteComposable
public fun WidgetContainer(content: @Composable @RemoteComposable () -> Unit) {
    val colors = RemoteButtonColors(
        containerColor = RemoteColor(Color(0xFF312E5C)),
        contentColor = RemoteColor(Color(0xFFE3E1E6)),
        secondaryContentColor = RemoteColor(Color(0xFFC7C5D0)),
        iconColor = RemoteColor(Color.White),
        disabledContainerColor = RemoteColor(Color.Gray),
        disabledContentColor = RemoteColor(Color.Gray),
        disabledSecondaryContentColor = RemoteColor(Color.Gray),
        disabledIconColor = RemoteColor(Color.Gray)
    )
    RemoteButton(
        onClick = dummyAction,
        modifier = RemoteModifier.fillMaxSize(),
        colors = colors,
        shape = RemoteRoundedCornerShape(16.rdp),
        content = {
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize(),
                contentAlignment = RemoteAlignment.Center,
                content = content
            )
        }
    )
}
