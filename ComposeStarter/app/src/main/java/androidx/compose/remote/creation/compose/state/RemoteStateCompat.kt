package androidx.compose.remote.creation.compose.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RemoteString(val value: String) {
    operator fun plus(other: RemoteString) = RemoteString(value + other.value)
    operator fun plus(other: String) = RemoteString(value + other)
}

data class RemoteFloat(val value: Float) {
    operator fun plus(other: Float) = RemoteFloat(value + other)
    operator fun plus(other: RemoteFloat) = RemoteFloat(value + other.value)
    operator fun rem(other: Float) = RemoteFloat(value % other)
    fun createReference() = this
}

data class RemoteInt(val value: Int) {
    operator fun plus(other: Int) = RemoteInt(value + other)
    fun toRemoteString() = RemoteString(value.toString())
}

data class RemoteBoolean(val value: Boolean)

data class RemoteDp(val value: Dp)

data class RemoteSp(val value: TextUnit)

data class RemoteColor(val value: Color) {
    fun copy(alpha: RemoteFloat = RemoteFloat(value.alpha)) = RemoteColor(value.copy(alpha = alpha.value))
    fun copy(alpha: Float) = RemoteColor(value.copy(alpha = alpha))
}

val String.rs: RemoteString get() = RemoteString(this)
val Float.rf: RemoteFloat get() = RemoteFloat(this)
val Int.rf: RemoteFloat get() = RemoteFloat(toFloat())
val Int.rdp: RemoteDp get() = RemoteDp(dp)
val Float.rdp: RemoteDp get() = RemoteDp(dp)
val Int.rsp: RemoteSp get() = RemoteSp(sp)
val Float.rsp: RemoteSp get() = RemoteSp(sp)
val Boolean.rb: RemoteBoolean get() = RemoteBoolean(this)
val Color.rc: RemoteColor get() = RemoteColor(this)

@Composable fun rememberMutableRemoteInt(value: Int): RemoteInt = remember { RemoteInt(value) }

@Composable fun rememberMutableRemoteFloat(value: () -> RemoteFloat): RemoteFloat = remember { value() }

@Composable
fun animateRemoteFloat(
    @Suppress("UNUSED_PARAMETER") initialValue: Float,
    targetValue: () -> RemoteFloat,
): RemoteFloat = targetValue()

@Composable
fun rememberNamedRemoteBitmap(
    @Suppress("UNUSED_PARAMETER") name: String,
    bitmap: Any,
): Any = bitmap

@Composable
fun rememberNamedRemoteBitmap(
    @Suppress("UNUSED_PARAMETER") name: String,
    bitmap: () -> Any,
): Any = remember { bitmap() }

@Composable
fun rememberNamedRemoteColor(
    @Suppress("UNUSED_PARAMETER") name: String,
    color: Color,
): RemoteColor = RemoteColor(color)
