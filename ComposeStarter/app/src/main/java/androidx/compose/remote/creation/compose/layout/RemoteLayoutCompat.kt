package androidx.compose.remote.creation.compose.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class RemoteComposable

enum class RemoteAlignment(val alignment: Alignment) {
    Center(Alignment.Center),
    Start(Alignment.CenterStart),
}

enum class RemoteArrangement(val arrangement: Arrangement.Vertical) {
    Center(Arrangement.Center),
}

@Composable
fun RemoteBox(
    modifier: RemoteModifier = RemoteModifier,
    contentAlignment: RemoteAlignment = RemoteAlignment.Center,
    content: @Composable () -> Unit = {},
) {
    Box(modifier.modifier, contentAlignment = contentAlignment.alignment) {
        content()
    }
}

@Composable
fun RemoteColumn(
    modifier: RemoteModifier = RemoteModifier,
    horizontalAlignment: RemoteAlignment = RemoteAlignment.Center,
    verticalArrangement: RemoteArrangement = RemoteArrangement.Center,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.modifier,
        horizontalAlignment = when (horizontalAlignment) {
            RemoteAlignment.Start -> Alignment.Start
            RemoteAlignment.Center -> Alignment.CenterHorizontally
        },
        verticalArrangement = verticalArrangement.arrangement,
    ) {
        content()
    }
}
