package androidx.wear.compose.remote.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteSp
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

object RemoteMaterialTheme {
    val colorScheme: RemoteColorScheme
        @Composable get() = RemoteColorScheme(
            primary = RemoteColor(MaterialTheme.colorScheme.primary),
            onPrimary = RemoteColor(MaterialTheme.colorScheme.onPrimary),
            outline = RemoteColor(MaterialTheme.colorScheme.outline),
        )
}

data class RemoteColorScheme(
    val primary: RemoteColor,
    val onPrimary: RemoteColor,
    val outline: RemoteColor,
)

data class RemoteButtonColors(
    val containerColor: RemoteColor = RemoteColor(Color.Unspecified),
    val contentColor: RemoteColor = RemoteColor(Color.Unspecified),
    val disabledContainerColor: RemoteColor = RemoteColor(Color.Unspecified),
    val disabledContentColor: RemoteColor = RemoteColor(Color.Unspecified),
) {
    val iconColor: RemoteColor get() = contentColor
}

object RemoteButtonDefaults {
    val shape = Any()
    val SmallIconSize = RemoteDp(24.dp)
    val ExtraSmallIconSize = RemoteDp(18.dp)
    fun buttonColors() = RemoteButtonColors()
    fun containerPainter(@Suppress("UNUSED_PARAMETER") painter: Any) = painter
}

object RemoteIconButtonDefaults {
    fun iconButtonColors() = RemoteButtonColors()
}

object RemoteTextButtonDefaults {
    fun textButtonColors() = RemoteButtonColors()
}

object RemoteButtonGroupDefaults {
    val MinWidth = RemoteDp(48.dp)
    val Spacing = RemoteDp(4.dp)
}

@Composable
fun RemoteText(
    text: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor = RemoteColor(Color.Unspecified),
    fontSize: RemoteSp? = null,
    @Suppress("UNUSED_PARAMETER") style: Any? = null,
    @Suppress("UNUSED_PARAMETER") overflow: Any? = null,
    @Suppress("UNUSED_PARAMETER") maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text.value,
        modifier = modifier.modifier,
        color = color.value,
        fontSize = fontSize?.value ?: androidx.compose.ui.unit.TextUnit.Unspecified,
    )
}

@Composable
fun RemoteButton(
    @Suppress("UNUSED_PARAMETER") onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    @Suppress("UNUSED_PARAMETER") enabled: RemoteBoolean = RemoteBoolean(true),
    @Suppress("UNUSED_PARAMETER") colors: RemoteButtonColors = RemoteButtonDefaults.buttonColors(),
    @Suppress("UNUSED_PARAMETER") shape: Any? = null,
    @Suppress("UNUSED_PARAMETER") border: RemoteDp? = null,
    @Suppress("UNUSED_PARAMETER") borderColor: RemoteColor? = null,
    @Suppress("UNUSED_PARAMETER") containerPainter: Any? = null,
    icon: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null,
    secondaryLabel: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier.modifier
            .background(colors.containerColor.value)
            .clickable {}
            .padding(8.dp),
    ) {
        icon?.invoke()
        label?.invoke()
        secondaryLabel?.invoke()
        content?.invoke()
    }
}

@Composable
fun RemoteCompactButton(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    @Suppress("UNUSED_PARAMETER") icon: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") border: RemoteDp? = null,
    @Suppress("UNUSED_PARAMETER") borderColor: RemoteColor? = null,
    @Suppress("UNUSED_PARAMETER") shape: Any? = null,
) {
    RemoteButton(onClick = onClick, modifier = modifier, icon = icon, label = label)
}

@Composable
fun RemoteTextButton(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = RemoteBoolean(true),
    colors: RemoteButtonColors = RemoteTextButtonDefaults.textButtonColors(),
    @Suppress("UNUSED_PARAMETER") border: RemoteDp? = null,
    @Suppress("UNUSED_PARAMETER") borderColor: RemoteColor? = null,
    content: @Composable () -> Unit,
) {
    RemoteButton(onClick, modifier, enabled, colors, content = content)
}

@Composable
fun RemoteIconButton(
    @Suppress("UNUSED_PARAMETER") onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    @Suppress("UNUSED_PARAMETER") enabled: RemoteBoolean = RemoteBoolean(true),
    @Suppress("UNUSED_PARAMETER") colors: RemoteButtonColors = RemoteIconButtonDefaults.iconButtonColors(),
    @Suppress("UNUSED_PARAMETER") shape: Any? = null,
    @Suppress("UNUSED_PARAMETER") border: RemoteDp? = null,
    @Suppress("UNUSED_PARAMETER") borderColor: RemoteColor? = null,
    content: @Composable () -> Unit,
) {
    Box(modifier.modifier.clickable {}.padding(8.dp)) {
        content()
    }
}

@Composable
fun RemoteIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: RemoteModifier = RemoteModifier,
    tint: RemoteColor = RemoteColor(Color.Unspecified),
) {
    Icon(imageVector, contentDescription, modifier.modifier, tint = tint.value)
}

@Composable
fun RemoteIcon(
    imageVector: RemoteImageVector,
    contentDescription: String?,
    modifier: RemoteModifier = RemoteModifier,
    tint: RemoteColor = RemoteColor(Color.Unspecified),
) {
    RemoteIcon(imageVector.imageVector, contentDescription, modifier, tint)
}

@Composable
fun RemoteCard(
    @Suppress("UNUSED_PARAMETER") onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.modifier.clickable {}.padding(8.dp)) {
        content()
    }
}

@Composable
fun RemoteOutlinedCard(
    @Suppress("UNUSED_PARAMETER") onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable () -> Unit,
) {
    RemoteCard(onClick, modifier, content)
}

@Composable
fun RemoteTitleCard(
    onClick: Action,
    title: @Composable () -> Unit,
    time: @Composable () -> Unit = {},
    modifier: RemoteModifier = RemoteModifier,
    subtitle: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    RemoteCard(onClick, modifier) {
        title()
        subtitle?.invoke()
        time()
        content()
    }
}

@Composable
fun RemoteAppCard(
    onClick: Action,
    appName: @Composable () -> Unit,
    time: @Composable () -> Unit = {},
    title: @Composable () -> Unit,
    modifier: RemoteModifier = RemoteModifier,
    appImage: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    RemoteCard(onClick, modifier) {
        appImage?.invoke()
        appName()
        title()
        time()
        content()
    }
}

@Composable
fun RemoteButtonGroup(
    modifier: RemoteModifier = RemoteModifier,
    @Suppress("UNUSED_PARAMETER") spacing: RemoteDp = RemoteButtonGroupDefaults.Spacing,
    content: @Composable () -> Unit = {},
) {
    Row(modifier.modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        content()
    }
}

@Composable
fun RemoteCircularProgressIndicator(
    modifier: RemoteModifier = RemoteModifier,
    progress: RemoteFloat? = null,
    @Suppress("UNUSED_PARAMETER") startAngle: RemoteFloat? = null,
    @Suppress("UNUSED_PARAMETER") endAngle: RemoteFloat? = null,
    @Suppress("UNUSED_PARAMETER") gapSize: RemoteDp? = null,
    @Suppress("UNUSED_PARAMETER") colors: Any? = null,
    @Suppress("UNUSED_PARAMETER") enabled: RemoteBoolean = RemoteBoolean(true),
) {
    if (progress == null) {
        CircularProgressIndicator(modifier = modifier.modifier.size(48.dp))
    } else {
        CircularProgressIndicator(progress = { progress.value }, modifier = modifier.modifier.size(48.dp))
    }
}

@Composable
fun RemoteCircularProgressIndicator(
    progress: RemoteFloat,
    modifier: RemoteModifier = RemoteModifier,
) {
    RemoteCircularProgressIndicator(modifier = modifier, progress = progress)
}

object RemoteProgressIndicatorDefaults {
    fun colors(
        @Suppress("UNUSED_PARAMETER") indicatorColor: RemoteColor = RemoteColor(Color.Unspecified),
        @Suppress("UNUSED_PARAMETER") trackColor: RemoteColor = RemoteColor(Color.Unspecified),
    ) = Any()
}

fun RemoteModifier.buttonSizeModifier() = this

val DefaultRemoteIcon: ImageVector = Icons.Filled.Favorite
