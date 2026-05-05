package androidx.compose.remote.creation.compose.action

import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString

open class Action {
    data object Empty : Action()
}

class HostAction(
    @Suppress("UNUSED_PARAMETER") name: RemoteString,
    @Suppress("UNUSED_PARAMETER") id: RemoteFloat,
) : Action()

class ValueChange<T>(
    @Suppress("UNUSED_PARAMETER") target: T,
    @Suppress("UNUSED_PARAMETER") value: T,
) : Action()
