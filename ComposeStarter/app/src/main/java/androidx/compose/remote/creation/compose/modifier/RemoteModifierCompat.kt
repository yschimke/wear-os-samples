@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier

open class RemoteModifier(val modifier: Modifier = Modifier) {
    companion object : RemoteModifier()

    fun padding(all: RemoteDp) = RemoteModifier(modifier.padding(all.value))
    fun size(size: RemoteDp) = RemoteModifier(modifier.size(size.value))
    fun fillMaxSize() = RemoteModifier(modifier.fillMaxSize())
    fun fillMaxWidth() = RemoteModifier(modifier.fillMaxWidth())
    fun widthIn(min: RemoteDp) = RemoteModifier(modifier.widthIn(min = min.value))
    fun clickable(@Suppress("UNUSED_PARAMETER") action: Action) = this
    fun weight(@Suppress("UNUSED_PARAMETER") weight: Float) = this
}

fun RemoteModifier.padding(all: RemoteDp) = padding(all)
fun RemoteModifier.size(size: RemoteDp) = size(size)
fun RemoteModifier.fillMaxSize() = fillMaxSize()
fun RemoteModifier.fillMaxWidth() = fillMaxWidth()
fun RemoteModifier.widthIn(min: RemoteDp) = widthIn(min)
fun RemoteModifier.clickable(action: Action) = clickable(action)
fun RemoteModifier.weight(weight: Float) = weight(weight)
