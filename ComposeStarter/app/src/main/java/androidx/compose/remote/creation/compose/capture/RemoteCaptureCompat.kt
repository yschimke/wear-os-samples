package androidx.compose.remote.creation.compose.capture

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

data class RemoteDensity(val density: RemoteFloat, val fontScale: RemoteFloat)

class RemoteComposeCreationState {
    var density: RemoteDensity? = null
    var remoteDensity: RemoteDensity?
        get() = density
        set(value) {
            density = value
        }
}

object LocalRemoteComposeCreationState {
    val current = RemoteComposeCreationState()
}

class RemoteImageVector(val imageVector: ImageVector) {
    class Builder(
        private val viewportWidth: RemoteFloat,
        private val viewportHeight: RemoteFloat,
        private val name: String,
        @Suppress("UNUSED_PARAMETER") tintColor: RemoteColor = RemoteColor(Color.Black),
        @Suppress("UNUSED_PARAMETER") autoMirror: Boolean = false,
    ) {
        private val builder = ImageVector.Builder(
            name = name,
            defaultWidth = viewportWidth.value.dp,
            defaultHeight = viewportHeight.value.dp,
            viewportWidth = viewportWidth.value,
            viewportHeight = viewportHeight.value,
        )

        internal fun addPath(
            @Suppress("UNUSED_PARAMETER") fill: SolidColor,
            builderAction: PathBuilder.() -> Unit,
        ): Builder {
            PathBuilder().builderAction()
            builder.path {}
            return this
        }

        fun build() = RemoteImageVector(builder.build())
    }
}

fun RemoteImageVector.Builder.path(
    fill: SolidColor,
    builderAction: PathBuilder.() -> Unit,
): RemoteImageVector.Builder = addPath(fill, builderAction)

class PathBuilder {
    fun moveTo(@Suppress("UNUSED_PARAMETER") x: RemoteFloat, @Suppress("UNUSED_PARAMETER") y: RemoteFloat) {}
    fun verticalLineToRelative(@Suppress("UNUSED_PARAMETER") y: RemoteFloat) {}
    fun horizontalLineToRelative(@Suppress("UNUSED_PARAMETER") x: RemoteFloat) {}
    fun lineToRelative(@Suppress("UNUSED_PARAMETER") x: RemoteFloat, @Suppress("UNUSED_PARAMETER") y: RemoteFloat) {}
    fun lineTo(@Suppress("UNUSED_PARAMETER") x: RemoteFloat, @Suppress("UNUSED_PARAMETER") y: RemoteFloat) {}
    fun close() {}
    fun curveToRelative(
        @Suppress("UNUSED_PARAMETER") x1: RemoteFloat,
        @Suppress("UNUSED_PARAMETER") y1: RemoteFloat,
        @Suppress("UNUSED_PARAMETER") x2: RemoteFloat,
        @Suppress("UNUSED_PARAMETER") y2: RemoteFloat,
        @Suppress("UNUSED_PARAMETER") x3: RemoteFloat,
        @Suppress("UNUSED_PARAMETER") y3: RemoteFloat,
    ) {}
    fun reflectiveCurveToRelative(
        @Suppress("UNUSED_PARAMETER") x: RemoteFloat,
        @Suppress("UNUSED_PARAMETER") y: RemoteFloat,
        @Suppress("UNUSED_PARAMETER") x2: RemoteFloat,
        @Suppress("UNUSED_PARAMETER") y2: RemoteFloat,
    ) {}
}
